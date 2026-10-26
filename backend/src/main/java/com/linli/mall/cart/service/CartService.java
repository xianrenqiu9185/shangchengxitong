package com.linli.mall.cart.service;

import com.linli.mall.cart.dto.CartItemAddRequest;
import com.linli.mall.cart.dto.CartItemUpdateRequest;
import com.linli.mall.cart.dto.CartView;

public interface CartService {

    CartView get(Long userId);

    CartView add(Long userId, CartItemAddRequest request);

    CartView update(Long userId, Long itemId, CartItemUpdateRequest request);

    CartView remove(Long userId, Long itemId);

    CartView clear(Long userId);

    int removeChecked(Long userId);
}
