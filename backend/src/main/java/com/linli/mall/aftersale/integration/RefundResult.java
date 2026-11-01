package com.linli.mall.aftersale.integration;

public record RefundResult(RefundStatus status, String refundNo, String message) {

    public static RefundResult success(String refundNo) {
        return new RefundResult(RefundStatus.SUCCESS, refundNo, "退款成功");
    }

    public static RefundResult processing(String refundNo) {
        return new RefundResult(RefundStatus.PROCESSING, refundNo, "退款处理中");
    }

    public static RefundResult failed(String refundNo, String message) {
        return new RefundResult(RefundStatus.FAILED, refundNo, message);
    }
}
