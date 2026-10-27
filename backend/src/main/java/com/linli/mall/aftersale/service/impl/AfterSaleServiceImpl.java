package com.linli.mall.aftersale.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linli.mall.aftersale.dto.AfterSaleAuditRequest;
import com.linli.mall.aftersale.dto.AfterSaleCreateRequest;
import com.linli.mall.aftersale.dto.AfterSaleView;
import com.linli.mall.aftersale.entity.AfterSale;
import com.linli.mall.aftersale.integration.OrderInfo;
import com.linli.mall.aftersale.integration.OrderPort;
import com.linli.mall.aftersale.integration.OrderStatus;
import com.linli.mall.aftersale.integration.RefundPort;
import com.linli.mall.aftersale.integration.RefundRequest;
import com.linli.mall.aftersale.integration.RefundResult;
import com.linli.mall.aftersale.integration.RefundStatus;
import com.linli.mall.aftersale.mapper.AfterSaleMapper;
import com.linli.mall.aftersale.service.AfterSaleService;
import com.linli.mall.common.api.ErrorCode;
import com.linli.mall.common.api.PageResult;
import com.linli.mall.common.exception.BizException;
import com.linli.mall.common.idempotency.IdempotencyClaim;
import com.linli.mall.common.idempotency.IdempotencyService;
import com.linli.mall.user.entity.User;
import com.linli.mall.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AfterSaleServiceImpl implements AfterSaleService {

    private static final String APPLY_BIZ_TYPE = "AFTER_SALE_APPLY";
    private static final String AUDIT_BIZ_TYPE = "AFTER_SALE_AUDIT";
    private static final String DESCRIPTION_SEPARATOR = " | ";
    private static final DateTimeFormatter NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AfterSaleMapper afterSaleMapper;
    private final UserMapper userMapper;
    private final IdempotencyService idempotencyService;
    private final ObjectProvider<OrderPort> orderPortProvider;
    private final ObjectProvider<RefundPort> refundPortProvider;

    @Override
    @Transactional
    public AfterSaleView apply(Long userId, String idempotencyKey, AfterSaleCreateRequest request) {
        String saleNo = newNo("AS");
        IdempotencyClaim claim = idempotencyService.claim(userId, APPLY_BIZ_TYPE, idempotencyKey, saleNo);
        if (!claim.acquired()) {
            return getUser(userId, claim.bizNo());
        }

        OrderPort orderPort = requireOrderPort();
        OrderInfo order = orderPort.findByOrderNoForUser(request.orderNo(), userId);
        if (order == null || !Objects.equals(order.userId(), userId)) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        if (!OrderStatus.canApplyAfterSale(order.status())) {
            throw new BizException(ErrorCode.AFTER_SALE_STATUS_INVALID);
        }
        Long active = afterSaleMapper.selectCount(new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getOrderId, order.orderId())
                .eq(AfterSale::getUserId, userId)
                .in(AfterSale::getStatus, 0, 1, 3));
        if (active != null && active > 0) {
            throw new BizException(ErrorCode.AFTER_SALE_STATUS_INVALID, "该订单已有处理中的售后申请");
        }

        orderPort.markAfterSale(order.orderId(), order.status());
        AfterSale sale = new AfterSale();
        sale.setSaleNo(saleNo);
        sale.setOrderId(order.orderId());
        sale.setUserId(userId);
        sale.setType("RETURN".equals(request.type()) ? 2 : 1);
        sale.setReason(buildStoredReason(request.reason(), request.description()));
        sale.setAmount(order.payAmount() == null ? 0L : order.payAmount());
        sale.setStatus(0);
        sale.setPreStatus(order.status());
        afterSaleMapper.insert(sale);
        idempotencyService.succeed(userId, APPLY_BIZ_TYPE, idempotencyKey, saleNo);
        return toViews(List.of(sale), Map.of(order.orderId(), order), customerNames(List.of(userId))).get(0);
    }

    @Override
    public PageResult<AfterSaleView> listUser(Long userId, String status, long pageNum, long pageSize) {
        long safePageNum = Math.max(pageNum, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 100);
        Page<AfterSale> page = afterSaleMapper.selectPage(new Page<>(safePageNum, safePageSize),
                new LambdaQueryWrapper<AfterSale>()
                        .eq(AfterSale::getUserId, userId)
                        .eq(hasText(status), AfterSale::getStatus, parseStatus(status))
                        .orderByDesc(AfterSale::getCreateTime)
                        .orderByDesc(AfterSale::getId));
        return PageResult.of(toViewsWithOrders(page.getRecords()), page.getTotal(), safePageNum, safePageSize);
    }

    @Override
    public AfterSaleView getUser(Long userId, String saleNo) {
        AfterSale sale = afterSaleMapper.selectOne(new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getSaleNo, saleNo)
                .eq(AfterSale::getUserId, userId));
        if (sale == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return toViewsWithOrders(List.of(sale)).get(0);
    }

    @Override
    public PageResult<AfterSaleView> listAdmin(String status, long pageNum, long pageSize) {
        long safePageNum = Math.max(pageNum, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 100);
        Page<AfterSale> page = afterSaleMapper.selectPage(new Page<>(safePageNum, safePageSize),
                new LambdaQueryWrapper<AfterSale>()
                        .eq(hasText(status), AfterSale::getStatus, parseStatus(status))
                        .orderByDesc(AfterSale::getCreateTime)
                        .orderByDesc(AfterSale::getId));
        return PageResult.of(toViewsWithOrders(page.getRecords()), page.getTotal(), safePageNum, safePageSize);
    }

    @Override
    public AfterSaleView audit(Long operatorId, String saleNo, String idempotencyKey,
                               AfterSaleAuditRequest request) {
        AfterSale sale = requireBySaleNo(saleNo);
        IdempotencyClaim claim = idempotencyService.claim(operatorId, AUDIT_BIZ_TYPE, idempotencyKey, saleNo);
        if (!claim.acquired()) {
            return toViewsWithOrders(List.of(requireBySaleNo(claim.bizNo()))).get(0);
        }
        try {
            if (Boolean.TRUE.equals(request.approved())) {
                return approve(sale, operatorId, idempotencyKey, request.remark());
            }
            return reject(sale, operatorId, idempotencyKey, request.remark());
        } catch (RuntimeException e) {
            idempotencyService.fail(operatorId, AUDIT_BIZ_TYPE, idempotencyKey);
            throw e;
        }
    }

    private AfterSaleView approve(AfterSale sale, Long operatorId, String idempotencyKey, String remark) {
        String refundNo = newNo("RF");
        if (afterSaleMapper.approve(sale.getId(), refundNo, remark) == 0) {
            throw new BizException(ErrorCode.AFTER_SALE_STATUS_INVALID);
        }
        RefundPort refundPort = refundPortProvider.getIfAvailable();
        if (refundPort == null) {
            afterSaleMapper.markRefunding(sale.getId());
            idempotencyService.succeed(operatorId, AUDIT_BIZ_TYPE, idempotencyKey, sale.getSaleNo());
            return reload(sale.getSaleNo());
        }
        RefundResult result;
        try {
            result = refundPort.refund(new RefundRequest(refundNo, sale.getSaleNo(), sale.getOrderId(), sale.getAmount()));
        } catch (RuntimeException e) {
            log.warn("退款调用结果未知，saleNo={}, refundNo={}", sale.getSaleNo(), refundNo, e);
            afterSaleMapper.markRefunding(sale.getId());
            idempotencyService.succeed(operatorId, AUDIT_BIZ_TYPE, idempotencyKey, sale.getSaleNo());
            return reload(sale.getSaleNo());
        }
        if (result != null && result.status() == RefundStatus.SUCCESS) {
            afterSaleMapper.complete(sale.getId());
            OrderPort orderPort = orderPortProvider.getIfAvailable();
            if (orderPort != null) {
                try {
                    orderPort.markRefunded(sale.getOrderId());
                } catch (RuntimeException e) {
                    log.error("退款成功但订单状态回写失败，orderId={}, saleNo={}", sale.getOrderId(), sale.getSaleNo(), e);
                }
            }
        } else {
            afterSaleMapper.markRefunding(sale.getId());
            if (result != null && result.status() == RefundStatus.FAILED) {
                log.warn("退款失败并转退款中重试，saleNo={}, message={}", sale.getSaleNo(), result.message());
            }
        }
        idempotencyService.succeed(operatorId, AUDIT_BIZ_TYPE, idempotencyKey, sale.getSaleNo());
        log.info("售后审核完成，operatorId={}, saleNo={}, approved=true", operatorId, sale.getSaleNo());
        return reload(sale.getSaleNo());
    }

    private AfterSaleView reject(AfterSale sale, Long operatorId, String idempotencyKey, String remark) {
        if (afterSaleMapper.reject(sale.getId(), remark) == 0) {
            throw new BizException(ErrorCode.AFTER_SALE_STATUS_INVALID);
        }
        OrderPort orderPort = orderPortProvider.getIfAvailable();
        if (orderPort != null) {
            orderPort.restoreStatus(sale.getOrderId(), sale.getPreStatus());
        }
        idempotencyService.succeed(operatorId, AUDIT_BIZ_TYPE, idempotencyKey, sale.getSaleNo());
        log.info("售后审核驳回，operatorId={}, saleNo={}", operatorId, sale.getSaleNo());
        return reload(sale.getSaleNo());
    }

    private AfterSaleView reload(String saleNo) {
        return toViewsWithOrders(List.of(requireBySaleNo(saleNo))).get(0);
    }

    private AfterSale requireBySaleNo(String saleNo) {
        AfterSale sale = afterSaleMapper.selectOne(new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getSaleNo, saleNo));
        if (sale == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return sale;
    }

    private OrderPort requireOrderPort() {
        OrderPort port = orderPortProvider.getIfAvailable();
        if (port == null) {
            throw new BizException(ErrorCode.AFTER_SALE_NOT_ALLOWED, "订单模块尚未接入，暂不能申请售后");
        }
        return port;
    }

    private List<AfterSaleView> toViewsWithOrders(List<AfterSale> sales) {
        if (sales.isEmpty()) {
            return List.of();
        }
        OrderPort port = orderPortProvider.getIfAvailable();
        List<Long> orderIds = sales.stream().map(AfterSale::getOrderId).distinct().toList();
        Map<Long, OrderInfo> orders = port == null ? Collections.emptyMap() : port.findByIds(orderIds);
        return toViews(sales, orders, customerNames(sales.stream().map(AfterSale::getUserId).distinct().toList()));
    }

    private List<AfterSaleView> toViews(List<AfterSale> sales, Map<Long, OrderInfo> orders,
                                        Map<Long, String> customers) {
        return sales.stream().map(sale -> {
            OrderInfo order = orders.get(sale.getOrderId());
            String[] reasonParts = splitReason(sale.getReason());
            return new AfterSaleView(sale.getSaleNo(), order == null ? null : order.orderNo(),
                    customers.get(sale.getUserId()), sale.getType() == 2 ? "RETURN" : "REFUND",
                    reasonParts[0], reasonParts[1], sale.getAmount(), statusName(sale.getStatus()),
                    sale.getStatus(), sale.getPreStatus(), sale.getRefundNo(), sale.getAuditTime(),
                    sale.getRemark(), sale.getCreateTime());
        }).toList();
    }

    private Map<Long, String> customerNames(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId,
                        user -> user.getNickname() == null ? user.getPhone() : user.getNickname()));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private int parseStatus(String status) {
        if (!hasText(status)) {
            return 0;
        }
        return switch (status.trim().toUpperCase()) {
            case "PENDING" -> 0;
            case "APPROVED" -> 1;
            case "REJECTED" -> 2;
            case "REFUNDING" -> 3;
            case "COMPLETED" -> 4;
            default -> throw new BizException(ErrorCode.BAD_REQUEST, "售后状态非法");
        };
    }

    private String statusName(Integer status) {
        return switch (status == null ? -1 : status) {
            case 0 -> "PENDING";
            case 1 -> "APPROVED";
            case 2 -> "REJECTED";
            case 3 -> "REFUNDING";
            case 4 -> "COMPLETED";
            default -> "UNKNOWN";
        };
    }

    private String buildStoredReason(String reason, String description) {
        String value = reason.trim();
        if (description != null && !description.isBlank()) {
            value += DESCRIPTION_SEPARATOR + description.trim();
        }
        return value.length() > 255 ? value.substring(0, 255) : value;
    }

    private String[] splitReason(String stored) {
        if (stored == null) {
            return new String[]{"", null};
        }
        int index = stored.indexOf(DESCRIPTION_SEPARATOR);
        if (index < 0) {
            return new String[]{stored, null};
        }
        return new String[]{stored.substring(0, index), stored.substring(index + DESCRIPTION_SEPARATOR.length())};
    }

    private String newNo(String prefix) {
        int random = RANDOM.nextInt(1_000_000);
        return prefix + LocalDateTime.now().format(NO_FORMATTER) + String.format("%06d", random);
    }
}
