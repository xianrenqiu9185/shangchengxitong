package com.linli.mall.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AccountCreateRequest(
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        @NotBlank(message = "账号名称不能为空")
        @Size(max = 50, message = "账号名称不能超过 50 个字符")
        String nickname,

        @NotBlank(message = "角色不能为空")
        @Pattern(regexp = "(?i)operator|admin", message = "角色仅支持 operator 或 admin")
        String role,

        @NotBlank(message = "初始密码不能为空")
        @Size(min = 6, max = 32, message = "密码长度需为 6-32 位")
        String password
) {
}
