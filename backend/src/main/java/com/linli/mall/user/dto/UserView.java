package com.linli.mall.user.dto;

public record UserView(
        Long id,
        String phone,
        String nickname,
        Integer userType,
        Long roleId,
        String role,
        String roleName,
        Integer status
) {
}
