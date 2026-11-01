package com.linli.mall.payment.dto;

import java.time.LocalDateTime;

public record PaymentView(String payNo, String orderNo, Long amount, String status,
                          LocalDateTime payTime) {
}
