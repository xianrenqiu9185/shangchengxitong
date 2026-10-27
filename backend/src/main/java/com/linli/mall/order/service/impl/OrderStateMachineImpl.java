package com.linli.mall.order.service.impl;

import com.linli.mall.common.api.ErrorCode;
import com.linli.mall.common.exception.BizException;
import com.linli.mall.order.enums.OrderStatus;
import com.linli.mall.order.mapper.OrderMapper;
import com.linli.mall.order.service.OrderStateMachine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderStateMachineImpl implements OrderStateMachine {

    private final OrderMapper orderMapper;

    @Override
    public void transition(String orderNo, OrderStatus from, OrderStatus to) {
        if (from == null || to == null || !from.canTransitionTo(to)) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID);
        }
        int affected = orderMapper.transition(orderNo, from.getCode(), to.getCode());
        if (affected == 0) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID);
        }
    }
}
