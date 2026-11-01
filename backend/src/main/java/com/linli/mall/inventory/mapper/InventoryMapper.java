package com.linli.mall.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linli.mall.inventory.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InventoryMapper extends BaseMapper<Inventory> {

    /**
     * 锁定库存：可用量足够才成功，天然防超卖。
     */
    @Update("""
            UPDATE t_inventory
            SET locked = locked + #{quantity},
                version = version + 1,
                update_time = NOW()
            WHERE product_id = #{productId}
              AND total - locked - sold >= #{quantity}
            """)
    int lock(@Param("productId") Long productId, @Param("quantity") int quantity);

    /**
     * 支付成功：锁定库存转为已售库存。
     */
    @Update("""
            UPDATE t_inventory
            SET locked = locked - #{quantity},
                sold = sold + #{quantity},
                version = version + 1,
                update_time = NOW()
            WHERE product_id = #{productId}
              AND locked >= #{quantity}
            """)
    int deduct(@Param("productId") Long productId, @Param("quantity") int quantity);

    /**
     * 取消/超时关单：释放锁定库存。
     */
    @Update("""
            UPDATE t_inventory
            SET locked = locked - #{quantity},
                version = version + 1,
                update_time = NOW()
            WHERE product_id = #{productId}
              AND locked >= #{quantity}
            """)
    int release(@Param("productId") Long productId, @Param("quantity") int quantity);
}
