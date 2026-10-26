package com.linli.mall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linli.mall.order.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}
