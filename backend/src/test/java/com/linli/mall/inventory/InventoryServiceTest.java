package com.linli.mall.inventory;

import com.linli.mall.common.exception.BizException;
import com.linli.mall.inventory.entity.Inventory;
import com.linli.mall.inventory.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class InventoryServiceTest {

    private static final Long PRODUCT_ID = 1L;

    @Autowired
    private InventoryService inventoryService;

    @Test
    void lockAndReleaseShouldKeepInvariant() {
        Inventory before = inventoryService.getByProductId(PRODUCT_ID);
        assertThat(before).isNotNull();

        inventoryService.lock(PRODUCT_ID, 2);
        Inventory locked = inventoryService.getByProductId(PRODUCT_ID);
        assertThat(locked.getLocked()).isEqualTo(before.getLocked() + 2);
        assertThat(locked.available()).isEqualTo(before.available() - 2);

        inventoryService.release(PRODUCT_ID, 2);
        Inventory released = inventoryService.getByProductId(PRODUCT_ID);
        assertThat(released.getLocked()).isEqualTo(before.getLocked());
        assertThat(released.available()).isEqualTo(before.available());
    }

    @Test
    void lockAndDeductShouldMoveLockedToSold() {
        Inventory before = inventoryService.getByProductId(PRODUCT_ID);

        inventoryService.lock(PRODUCT_ID, 1);
        inventoryService.deduct(PRODUCT_ID, 1);

        Inventory after = inventoryService.getByProductId(PRODUCT_ID);
        assertThat(after.getLocked()).isEqualTo(before.getLocked());
        assertThat(after.getSold()).isEqualTo(before.getSold() + 1);
        assertThat(after.getTotal()).isEqualTo(before.getTotal());
    }

    @Test
    void lockShouldFailWhenAvailableNotEnough() {
        Inventory before = inventoryService.getByProductId(PRODUCT_ID);

        assertThatThrownBy(() -> inventoryService.lock(PRODUCT_ID, before.available() + 1))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("库存不足");
    }
}
