package com.linli.mall.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linli.mall.payment.entity.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {

    /**
     * 回调抢占：0 待支付 → 1 支付中，避免并发重复处理。
     */
    @Update("""
            UPDATE t_payment
            SET status = 1, update_time = NOW()
            WHERE pay_no = #{payNo} AND status = 0
            """)
    int claimForCallback(@Param("payNo") String payNo);

    @Update("""
            UPDATE t_payment
            SET status = 2, pay_time = NOW(), update_time = NOW()
            WHERE pay_no = #{payNo} AND status IN (0, 1)
            """)
    int markPaid(@Param("payNo") String payNo);

    @Update("""
            UPDATE t_payment
            SET status = 3, update_time = NOW()
            WHERE pay_no = #{payNo} AND status IN (0, 1)
            """)
    int markFailed(@Param("payNo") String payNo);

    @Update("""
            UPDATE t_payment
            SET status = 4, update_time = NOW()
            WHERE pay_no = #{payNo} AND status IN (0, 1)
            """)
    int markClosed(@Param("payNo") String payNo);

    @Select("""
            SELECT * FROM t_payment
            WHERE order_id = #{orderId} AND status = 2
            ORDER BY id DESC LIMIT 1
            """)
    Payment findSuccessfulByOrderId(@Param("orderId") Long orderId);

    @Select("""
            SELECT * FROM t_payment
            WHERE order_id = #{orderId} AND status IN (0, 1)
            ORDER BY id DESC LIMIT 1
            """)
    Payment findActiveByOrderId(@Param("orderId") Long orderId);
}
