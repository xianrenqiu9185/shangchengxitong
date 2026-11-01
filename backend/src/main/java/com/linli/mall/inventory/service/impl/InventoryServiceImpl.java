package com.linli.mall.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linli.mall.common.api.ErrorCode;
import com.linli.mall.common.exception.BizException;
import com.linli.mall.inventory.entity.Inventory;
import com.linli.mall.inventory.mapper.InventoryMapper;
import com.linli.mall.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryMapper inventoryMapper;

    @Override
    public Inventory getByProductId(Long productId) {
        return inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getProductId, productId));
    }

    @Override
    public void lock(Long productId, int quantity) {
        requirePositive(quantity);
        int affected = inventoryMapper.lock(productId, quantity);
        if (affected == 0) {
            throw new BizException(ErrorCode.INVENTORY_NOT_ENOUGH);
        }
    }

    @Override
    public void deduct(Long productId, int quantity) {
        requirePositive(quantity);
        int affected = inventoryMapper.deduct(productId, quantity);
        if (affected == 0) {
            throw new BizException(ErrorCode.INVENTORY_NOT_ENOUGH, "锁定库存不足，无法扣减");
        }
    }

    @Override
    public void release(Long productId, int quantity) {
        requirePositive(quantity);
        int affected = inventoryMapper.release(productId, quantity);
        if (affected == 0) {
            throw new BizException(ErrorCode.INVENTORY_NOT_ENOUGH, "锁定库存不足，无法释放");
        }
    }

    private void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "数量必须大于 0");
        }
    }
}
