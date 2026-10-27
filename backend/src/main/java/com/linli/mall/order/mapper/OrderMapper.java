package com.linli.mall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linli.mall.order.integration.MemberOrderStatsRow;
import com.linli.mall.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

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

    @Select("""
            <script>
            SELECT user_id AS userId,
                   COUNT(*) AS orderCount,
                   COALESCE(SUM(pay_amount), 0) AS totalSpent
            FROM t_order
            WHERE deleted = 0
              AND status IN (1, 2, 3, 5)
              AND user_id IN
              <foreach collection="userIds" item="userId" open="(" separator="," close=")">
                #{userId}
              </foreach>
            GROUP BY user_id
            </script>
            """)
    List<MemberOrderStatsRow> selectMemberStats(@Param("userIds") Collection<Long> userIds);
}
