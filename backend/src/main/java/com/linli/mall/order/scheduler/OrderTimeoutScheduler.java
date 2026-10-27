package com.linli.mall.order.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linli.mall.order.entity.Order;
import com.linli.mall.order.enums.OrderStatus;
import com.linli.mall.order.mapper.OrderMapper;
import com.linli.mall.order.service.OrderManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 超时未支付自动关单：扫描并释放库存。
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class OrderTimeoutScheduler {

    private final OrderMapper orderMapper;
    private final OrderManagementService orderManagementService;

    @Scheduled(fixedDelayString = "${order.timeout.scan-interval-ms:60000}")
    public void closeExpiredOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, OrderStatus.PENDING_PAYMENT.getCode())
                .lt(Order::getPayDeadline, now)
                .orderByAsc(Order::getId)
                .last("LIMIT 50"));
        for (Order order : orders) {
            try {
                if (orderManagementService.closeIfPending(order.getOrderNo())) {
                    log.info("超时订单已关闭并释放库存，orderNo={}", order.getOrderNo());
                }
            } catch (Exception e) {
                log.warn("超时关单失败，orderNo={}", order.getOrderNo(), e);
            }
        }
    }
}
