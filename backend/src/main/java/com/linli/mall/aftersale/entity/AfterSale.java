package com.linli.mall.aftersale.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_after_sale")
public class AfterSale {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String saleNo;
    private Long orderId;
    private Long userId;
    private Integer type;
    private String reason;
    private Long amount;
    private Integer status;
    private Integer preStatus;
    private String refundNo;
    private LocalDateTime auditTime;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
