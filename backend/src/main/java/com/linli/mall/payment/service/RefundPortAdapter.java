package com.linli.mall.payment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linli.mall.aftersale.entity.AfterSale;
import com.linli.mall.aftersale.integration.RefundPort;
import com.linli.mall.aftersale.integration.RefundRequest;
import com.linli.mall.aftersale.integration.RefundResult;
import com.linli.mall.aftersale.mapper.AfterSaleMapper;
import com.linli.mall.payment.entity.Payment;
import com.linli.mall.payment.entity.Refund;
import com.linli.mall.payment.mapper.PaymentMapper;
import com.linli.mall.payment.mapper.RefundMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 售后模块的 RefundPort 实现（沙箱退款，refundNo 幂等）。
 */
@Service
@RequiredArgsConstructor
public class RefundPortAdapter implements RefundPort {

    private final RefundMapper refundMapper;
    private final PaymentMapper paymentMapper;
    private final AfterSaleMapper afterSaleMapper;

    @Override
    public RefundResult refund(RefundRequest request) {
        Refund existing = findByRefundNo(request.refundNo());
        if (existing != null) {
            return toResult(existing);
        }

        Payment payment = paymentMapper.findSuccessfulByOrderId(request.orderId());
        if (payment == null) {
            return RefundResult.failed(request.refundNo(), "未找到成功支付记录");
        }

        AfterSale afterSale = afterSaleMapper.selectOne(new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getSaleNo, request.saleNo()));

        Refund refund = new Refund();
        refund.setRefundNo(request.refundNo());
        refund.setPaymentId(payment.getId());
        refund.setOrderId(request.orderId());
        refund.setAfterSaleId(afterSale == null ? null : afterSale.getId());
        refund.setAmount(request.amount());
        // 沙箱退款立即成功
        refund.setStatus(1);
        refund.setRefundTime(LocalDateTime.now());
        try {
            refundMapper.insert(refund);
        } catch (DuplicateKeyException e) {
            existing = findByRefundNo(request.refundNo());
            if (existing != null) {
                return toResult(existing);
            }
            return RefundResult.failed(request.refundNo(), "退款单重复");
        }
        return RefundResult.success(request.refundNo());
    }

    private Refund findByRefundNo(String refundNo) {
        return refundMapper.selectOne(new LambdaQueryWrapper<Refund>()
                .eq(Refund::getRefundNo, refundNo));
    }

    private RefundResult toResult(Refund refund) {
        return switch (refund.getStatus()) {
            case 1 -> RefundResult.success(refund.getRefundNo());
            case 0 -> RefundResult.processing(refund.getRefundNo());
            default -> RefundResult.failed(refund.getRefundNo(), "退款失败");
        };
    }
}
