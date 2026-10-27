package com.linli.mall.user.dto;

import java.time.LocalDateTime;

public record MemberView(
        Long id,
        String phone,
        String nickname,
        String level,
        Integer status,
        Long orderCount,
        Long totalSpent,
        LocalDateTime createdAt
) {
}
