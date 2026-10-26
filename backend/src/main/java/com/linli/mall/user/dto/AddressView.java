package com.linli.mall.user.dto;

public record AddressView(
        Long id,
        String receiver,
        String phone,
        String province,
        String city,
        String district,
        String detail,
        String tag,
        Boolean isDefault
) {
}
