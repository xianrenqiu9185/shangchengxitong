package com.linli.mall.order.dto;

public record OrderAddressView(String receiver, String phone, String province,
                               String city, String district, String detail) {
}
