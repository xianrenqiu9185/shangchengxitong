package com.linli.mall.cart.controller;

import com.linli.mall.cart.dto.CartItemAddRequest;
import com.linli.mall.cart.dto.CartItemUpdateRequest;
import com.linli.mall.cart.dto.CartView;
import com.linli.mall.cart.service.CartService;
import com.linli.mall.common.api.Result;
import com.linli.mall.common.auth.RequirePermission;
import com.linli.mall.common.auth.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    @RequirePermission("cart:item:manage")
    public Result<CartView> get() {
        return Result.ok(cartService.get(UserContext.userId()));
    }

    @PostMapping("/items")
    @RequirePermission("cart:item:manage")
    public Result<CartView> add(@Valid @RequestBody CartItemAddRequest request) {
        return Result.ok(cartService.add(UserContext.userId(), request));
    }

    @PutMapping("/items/{id}")
    @RequirePermission("cart:item:manage")
    public Result<CartView> update(@PathVariable Long id,
                                   @Valid @RequestBody CartItemUpdateRequest request) {
        return Result.ok(cartService.update(UserContext.userId(), id, request));
    }

    @DeleteMapping("/items/{id}")
    @RequirePermission("cart:item:manage")
    public Result<CartView> delete(@PathVariable Long id) {
        return Result.ok(cartService.remove(UserContext.userId(), id));
    }

    @DeleteMapping("/items")
    @RequirePermission("cart:item:manage")
    public Result<CartView> clear() {
        return Result.ok(cartService.clear(UserContext.userId()));
    }
}
