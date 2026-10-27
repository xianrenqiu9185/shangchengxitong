package com.linli.mall.user.dto;

import java.time.LocalDateTime;
import java.util.List;

public record RoleView(
        Long id,
        String name,
        String code,
        String description,
        Integer status,
        Integer permissionCount,
        List<String> permissions,
        LocalDateTime updatedAt
) {
}
