package com.linli.mall.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存（三态：可用 = total - locked - sold）。
 */
@Data
@TableName("t_inventory")
public class Inventory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    private Integer total;

    private Integer locked;

    private Integer sold;

    private Integer version;

    private LocalDateTime updateTime;

    public int available() {
        return total - locked - sold;
    }
}
