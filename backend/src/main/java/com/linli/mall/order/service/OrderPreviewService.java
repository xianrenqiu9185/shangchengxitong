package com.linli.mall.order.service;

import com.linli.mall.order.dto.OrderPreviewView;
import com.linli.mall.order.dto.PreviewOrderRequest;

public interface OrderPreviewService {

    OrderPreviewView preview(PreviewOrderRequest request);
}
