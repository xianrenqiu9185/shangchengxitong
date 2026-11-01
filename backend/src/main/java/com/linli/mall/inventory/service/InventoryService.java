package com.linli.mall.inventory.service;

import com.linli.mall.inventory.entity.Inventory;

public interface InventoryService {

    Inventory getByProductId(Long productId);

    /**
     * 下单锁定库存；可用量不足抛出 20003。
     */
    void lock(Long productId, int quantity);

    /**
     * 支付成功确认扣减；锁定不足抛出业务异常。
     */
    void deduct(Long productId, int quantity);

    /**
     * 取消/超时关单释放锁定；重复释放不会产生负库存。
     */
    void release(Long productId, int quantity);
}
