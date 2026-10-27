package com.linli.mall.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单项（保存商品快照）。
 */
@Data
@TableName("t_order_item")
public class OrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long productId;

    private String productName;

    private String spec;

    /**
     * 成交单价，单位：分。
     */
    private Long price;

    private Integer quantity;

    /**
     * 小计，单位：分。
     */
    private Long amount;

    private LocalDateTime createTime;
}
