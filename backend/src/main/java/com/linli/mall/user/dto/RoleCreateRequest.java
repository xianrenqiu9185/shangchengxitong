package com.linli.mall.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RoleCreateRequest(
        @NotBlank(message = "角色名称不能为空")
        @Size(max = 50, message = "角色名称不能超过 50 个字符")
        String name,

        @NotBlank(message = "角色编码不能为空")
        @Size(max = 50, message = "角色编码不能超过 50 个字符")
        @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{1,49}$", message = "角色编码需以字母开头，仅包含字母、数字和下划线")
        String code,

        @Size(max = 255, message = "角色说明不能超过 255 个字符")
        String description,

        @Min(value = 0, message = "状态只能为 0 或 1")
        @Max(value = 1, message = "状态只能为 0 或 1")
        Integer status,

        @Size(max = 100, message = "权限点数量超出限制")
        List<@NotBlank(message = "权限点编码不能为空") String> permissions
) {
}
