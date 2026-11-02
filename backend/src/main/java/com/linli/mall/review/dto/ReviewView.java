package com.linli.mall.review.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewView(
        Long id,
        Long productId,
        String productName,
        String user,
        Integer rating,
        String content,
        List<String> images,
        String reply,
        LocalDateTime replyTime,
        LocalDateTime createdAt
) {
}
