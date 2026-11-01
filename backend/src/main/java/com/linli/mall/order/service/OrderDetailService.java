package com.linli.mall.order.service;

import com.linli.mall.common.api.PageResult;
import com.linli.mall.order.dto.OrderDetailView;

public interface OrderDetailService {

    OrderDetailView getDetail(String orderNo, Long userId);

    OrderDetailView getDetailForAdmin(String orderNo);

    PageResult<OrderDetailView> listUser(Long userId, String status, long pageNum, long pageSize);

    PageResult<OrderDetailView> listAdmin(String status, String keyword, long pageNum, long pageSize);
}
