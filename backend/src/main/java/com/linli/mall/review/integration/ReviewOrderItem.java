package com.linli.mall.review.integration;

public record ReviewOrderItem(Long orderId, Long orderItemId, Long productId, String productName) {
}
