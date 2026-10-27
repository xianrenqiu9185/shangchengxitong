package com.linli.mall.cart;

import com.linli.mall.cart.dto.CartItemAddRequest;
import com.linli.mall.cart.dto.CartItemUpdateRequest;
import com.linli.mall.cart.dto.CartView;
import com.linli.mall.cart.entity.CartItem;
import com.linli.mall.cart.mapper.CartItemMapper;
import com.linli.mall.cart.service.impl.CartServiceImpl;
import com.linli.mall.common.api.ErrorCode;
import com.linli.mall.common.exception.BizException;
import com.linli.mall.inventory.entity.Inventory;
import com.linli.mall.inventory.mapper.InventoryMapper;
import com.linli.mall.product.entity.Product;
import com.linli.mall.product.mapper.ProductMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock private CartItemMapper cartItemMapper;
    @Mock private ProductMapper productMapper;
    @Mock private InventoryMapper inventoryMapper;
    @InjectMocks private CartServiceImpl cartService;

    @Test
    void addShouldMergeDuplicateProductAndReturnTotals() {
        when(productMapper.selectById(1L)).thenReturn(product(1L, 1290L, 1));
        when(cartItemMapper.selectOne(any())).thenReturn(null);
        when(cartItemMapper.selectCount(any())).thenReturn(2L);
        when(cartItemMapper.merge(3L, 1L, 2)).thenReturn(1);
        when(cartItemMapper.selectList(any())).thenReturn(List.of(cartItem(5L, 1L, 3, 1)));
        when(productMapper.selectBatchIds(any())).thenReturn(List.of(product(1L, 1290L, 1)));
        when(inventoryMapper.selectList(any())).thenReturn(List.of(inventory(1L, 10, 0, 2)));

        CartView view = cartService.add(3L, new CartItemAddRequest(1L, 2));

        assertThat(view.items()).hasSize(1);
        assertThat(view.items().get(0).subtotal()).isEqualTo(3870);
        assertThat(view.count()).isEqualTo(3);
        assertThat(view.selectedCount()).isEqualTo(3);
        assertThat(view.total()).isEqualTo(3870);
        verify(cartItemMapper).merge(3L, 1L, 2);
    }

    @Test
    void addShouldRejectUnavailableProduct() {
        when(productMapper.selectById(1L)).thenReturn(product(1L, 1290L, 0));

        assertThatThrownBy(() -> cartService.add(3L, new CartItemAddRequest(1L, 1)))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_UNAVAILABLE);
        verify(cartItemMapper, never()).merge(any(), any(), anyInt());
    }

    @Test
    void addShouldRejectWhenCartHasOneHundredItems() {
        when(productMapper.selectById(1L)).thenReturn(product(1L, 1290L, 1));
        when(cartItemMapper.selectOne(any())).thenReturn(null);
        when(cartItemMapper.selectCount(any())).thenReturn(100L);

        assertThatThrownBy(() -> cartService.add(3L, new CartItemAddRequest(1L, 1)))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.CART_FULL);
    }

    @Test
    void updateShouldRejectAnotherUsersItem() {
        when(cartItemMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> cartService.update(3L, 9L, new CartItemUpdateRequest(2, true)))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    private Product product(Long id, Long price, int status) {
        Product product = new Product();
        product.setId(id);
        product.setName("山东红富士苹果");
        product.setSpec("500g/份");
        product.setPrice(price);
        product.setStatus(status);
        return product;
    }

    private CartItem cartItem(Long id, Long productId, int quantity, int checked) {
        CartItem item = new CartItem();
        item.setId(id);
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setChecked(checked);
        return item;
    }

    private Inventory inventory(Long productId, int total, int locked, int sold) {
        Inventory inventory = new Inventory();
        inventory.setProductId(productId);
        inventory.setTotal(total);
        inventory.setLocked(locked);
        inventory.setSold(sold);
        return inventory;
    }
}
