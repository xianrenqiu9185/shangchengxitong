package com.linli.mall.cart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linli.mall.cart.entity.CartItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CartItemMapper extends BaseMapper<CartItem> {

    /**
     * 唯一键 (user_id, product_id) 合并加购，单商品数量封顶 99。
     */
    @Insert("""
            INSERT INTO t_cart_item (user_id, product_id, quantity, checked)
            VALUES (#{userId}, #{productId}, #{quantity}, 1)
            ON DUPLICATE KEY UPDATE
                quantity = LEAST(quantity + VALUES(quantity), 99),
                checked = 1,
                update_time = NOW()
            """)
    int merge(@Param("userId") Long userId,
              @Param("productId") Long productId,
              @Param("quantity") int quantity);
}
