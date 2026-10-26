package com.linli.mall.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank(message = "收货人不能为空")
        @Size(max = 50, message = "收货人不能超过 50 个字符")
        String receiver,

        @NotBlank(message = "联系电话不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系电话格式不正确")
        String phone,

        @Size(max = 50) String province,
        @Size(max = 50) String city,
        @Size(max = 50) String district,

        @NotBlank(message = "详细地址不能为空")
        @Size(max = 255, message = "详细地址不能超过 255 个字符")
        String detail,

        @Size(max = 20) String tag,
        Boolean isDefault
) {
}
