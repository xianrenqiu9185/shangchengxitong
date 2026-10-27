package com.linli.mall.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_order")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private Long addressId;

    /**
     * 1 配送、2 自提。
     */
    private Integer fulfillType;

    /**
     * 金额单位：分。
     */
    private Long totalAmount;

    private Long discountAmount;

    private Long payAmount;

    private Long couponId;

    /**
     * 见 OrderStatus。
     */
    private Integer status;

    private LocalDateTime payDeadline;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
