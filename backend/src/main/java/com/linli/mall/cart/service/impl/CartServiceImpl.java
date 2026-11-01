package com.linli.mall.cart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.linli.mall.cart.dto.CartItemAddRequest;
import com.linli.mall.cart.dto.CartItemUpdateRequest;
import com.linli.mall.cart.dto.CartItemView;
import com.linli.mall.cart.dto.CartProductView;
import com.linli.mall.cart.dto.CartView;
import com.linli.mall.cart.entity.CartItem;
import com.linli.mall.cart.mapper.CartItemMapper;
import com.linli.mall.cart.service.CartService;
import com.linli.mall.common.api.ErrorCode;
import com.linli.mall.common.exception.BizException;
import com.linli.mall.inventory.entity.Inventory;
import com.linli.mall.inventory.mapper.InventoryMapper;
import com.linli.mall.product.entity.Product;
import com.linli.mall.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private static final int MAX_ITEMS = 100;
    private static final int MAX_QUANTITY = 99;

    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;
    private final InventoryMapper inventoryMapper;

    @Override
    public CartView get(Long userId) {
        return buildView(userId);
    }

    @Override
    public CartView add(Long userId, CartItemAddRequest request) {
        int quantity = request.quantity() == null ? 1 : request.quantity();
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw new BizException(ErrorCode.CART_QUANTITY_INVALID);
        }
        Product product = productMapper.selectById(request.productId());
        if (product == null || !Integer.valueOf(1).equals(product.getStatus())) {
            throw new BizException(ErrorCode.PRODUCT_UNAVAILABLE);
        }
        CartItem existing = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getProductId, request.productId()));
        if (existing == null) {
            Long count = cartItemMapper.selectCount(new LambdaQueryWrapper<CartItem>()
                    .eq(CartItem::getUserId, userId));
            if (count != null && count >= MAX_ITEMS) {
                throw new BizException(ErrorCode.CART_FULL);
            }
        }
        cartItemMapper.merge(userId, request.productId(), quantity);
        return buildView(userId);
    }

    @Override
    public CartView update(Long userId, Long itemId, CartItemUpdateRequest request) {
        if (request.quantity() == null && request.checked() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "数量或勾选状态至少提供一项");
        }
        requireOwned(userId, itemId);
        LambdaUpdateWrapper<CartItem> update = new LambdaUpdateWrapper<CartItem>()
                .eq(CartItem::getId, itemId)
                .eq(CartItem::getUserId, userId);
        if (request.quantity() != null) {
            update.set(CartItem::getQuantity, request.quantity());
        }
        if (request.checked() != null) {
            update.set(CartItem::getChecked, request.checked() ? 1 : 0);
        }
        cartItemMapper.update(null, update);
        return buildView(userId);
    }

    @Override
    public CartView remove(Long userId, Long itemId) {
        requireOwned(userId, itemId);
        cartItemMapper.deleteById(itemId);
        return buildView(userId);
    }

    @Override
    public CartView clear(Long userId) {
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId));
        return buildView(userId);
    }

    @Override
    public int removeChecked(Long userId) {
        return cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getChecked, 1));
    }

    private CartItem requireOwned(Long userId, Long itemId) {
        CartItem item = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getId, itemId)
                .eq(CartItem::getUserId, userId));
        if (item == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return item;
    }

    private CartView buildView(Long userId) {
        List<CartItem> items = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .orderByDesc(CartItem::getUpdateTime)
                .orderByDesc(CartItem::getId));
        if (items.isEmpty()) {
            return new CartView(List.of(), 0, 0, 0);
        }

        List<Long> productIds = items.stream().map(CartItem::getProductId).distinct().toList();
        Map<Long, Product> products = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        Map<Long, Inventory> inventories = inventoryMapper.selectList(new LambdaQueryWrapper<Inventory>()
                        .in(Inventory::getProductId, productIds))
                .stream()
                .collect(Collectors.toMap(Inventory::getProductId, Function.identity()));

        List<CartItemView> views = items.stream().map(item -> {
            Product product = products.get(item.getProductId());
            Inventory inventory = inventories.get(item.getProductId());
            boolean available = product != null && Integer.valueOf(1).equals(product.getStatus());
            int stock = inventory == null ? 0 : Math.max(inventory.available(), 0);
            int sales = inventory == null ? 0 : inventory.getSold();
            CartProductView productView = product == null
                    ? new CartProductView(item.getProductId(), "商品不存在或已下架", null, null, "",
                    0L, 0, 0, false, 0)
                    : new CartProductView(product.getId(), product.getName(), product.getImage(), product.getSpec(),
                    product.getSpec(), product.getPrice(), stock, product.getStatus(), available, sales);
            long subtotal = productView.price() * item.getQuantity();
            return new CartItemView(item.getId(), item.getProductId(), item.getQuantity(),
                    Integer.valueOf(1).equals(item.getChecked()), productView, subtotal, available);
        }).toList();

        int count = items.stream().mapToInt(CartItem::getQuantity).sum();
        int selectedCount = views.stream()
                .filter(item -> Boolean.TRUE.equals(item.checked()))
                .mapToInt(CartItemView::quantity)
                .sum();
        long total = views.stream()
                .filter(item -> Boolean.TRUE.equals(item.checked()) && Boolean.TRUE.equals(item.available()))
                .mapToLong(CartItemView::subtotal)
                .sum();
        return new CartView(views, count, selectedCount, total);
    }
}
