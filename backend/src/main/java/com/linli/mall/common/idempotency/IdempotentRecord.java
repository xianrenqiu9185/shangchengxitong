package com.linli.mall.common.idempotency;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_idempotent")
public class IdempotentRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String idemKey;
    private Long userId;
    private String bizType;
    private String bizNo;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime expireTime;
}
