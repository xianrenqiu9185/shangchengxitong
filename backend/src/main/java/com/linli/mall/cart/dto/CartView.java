package com.linli.mall.cart.dto;

import java.util.List;

public record CartView(List<CartItemView> items, int count, int selectedCount, long total) {
}
