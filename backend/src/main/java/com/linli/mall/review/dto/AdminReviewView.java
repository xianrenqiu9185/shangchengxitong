package com.linli.mall.review.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminReviewView(
        Long id,
        Long orderId,
        Long orderItemId,
        Long userId,
        String phone,
        String user,
        Long productId,
        String productName,
        Integer rating,
        String content,
        List<String> images,
        String reply,
        LocalDateTime replyTime,
        Integer status,
        LocalDateTime createdAt
) {
}
