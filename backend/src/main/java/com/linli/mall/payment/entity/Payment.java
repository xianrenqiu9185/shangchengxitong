package com.linli.mall.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_payment")
public class Payment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String payNo;

    private Long orderId;

    private Long userId;

    private Long amount;

    private Integer channel;

    /**
     * 0 待支付、1 支付中、2 成功、3 失败、4 已关闭。
     */
    private Integer status;

    private LocalDateTime payTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
