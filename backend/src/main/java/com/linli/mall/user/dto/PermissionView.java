package com.linli.mall.user.dto;

public record PermissionView(
        String value,
        String label,
        String module,
        Integer type,
        Integer sort
) {
}
