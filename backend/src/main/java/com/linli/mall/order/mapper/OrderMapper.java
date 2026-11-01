package com.linli.mall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linli.mall.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 条件更新状态：只有当前状态等于 from 时才迁移到 to。
     */
    @Update("""
            UPDATE t_order
            SET status = #{to}, update_time = NOW()
            WHERE order_no = #{orderNo}
              AND status = #{from}
              AND deleted = 0
            """)
    int transition(@Param("orderNo") String orderNo,
                   @Param("from") int from,
                   @Param("to") int to);

    @Update("""
            UPDATE t_order
            SET status = #{to}, update_time = NOW()
            WHERE id = #{orderId}
              AND status = #{from}
              AND deleted = 0
            """)
    int transitionById(@Param("orderId") Long orderId,
                       @Param("from") int from,
                       @Param("to") int to);
}
