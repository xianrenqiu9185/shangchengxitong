package com.linli.mall.user.dto;

import java.time.LocalDateTime;

public record AccountView(
        Long id,
        String phone,
        String nickname,
        String role,
        Integer userType,
        Long roleId,
        String roleName,
        Integer status,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {
}
