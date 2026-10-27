package com.linli.mall.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_refund")
public class Refund {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String refundNo;

    private Long paymentId;

    private Long orderId;

    private Long afterSaleId;

    private Long amount;

    /**
     * 0 退款中、1 成功、2 失败。
     */
    private Integer status;

    private LocalDateTime refundTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
