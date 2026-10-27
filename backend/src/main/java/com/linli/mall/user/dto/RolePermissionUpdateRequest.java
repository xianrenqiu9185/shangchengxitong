package com.linli.mall.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RolePermissionUpdateRequest(
        @NotNull(message = "权限点列表不能为空")
        @Size(max = 100, message = "权限点数量超出限制")
        List<@NotBlank(message = "权限点编码不能为空") String> permissions
) {
}
