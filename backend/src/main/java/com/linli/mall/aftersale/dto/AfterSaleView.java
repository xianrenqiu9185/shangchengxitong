package com.linli.mall.aftersale.dto;

import java.time.LocalDateTime;

public record AfterSaleView(
        String saleNo,
        String orderNo,
        String customer,
        String type,
        String reason,
        String description,
        Long amount,
        String status,
        Integer statusCode,
        Integer preStatus,
        String refundNo,
        LocalDateTime auditTime,
        String remark,
        LocalDateTime createdAt
) {
}
