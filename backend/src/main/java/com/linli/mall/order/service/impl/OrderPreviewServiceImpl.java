package com.linli.mall.order.service.impl;

import com.linli.mall.common.api.ErrorCode;
import com.linli.mall.common.exception.BizException;
import com.linli.mall.order.dto.OrderItemRequest;
import com.linli.mall.order.dto.OrderPreviewView;
import com.linli.mall.order.dto.PreviewOrderRequest;
import com.linli.mall.order.service.OrderPreviewService;
import com.linli.mall.order.support.OrderAmounts;
import com.linli.mall.product.entity.Product;
import com.linli.mall.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderPreviewServiceImpl implements OrderPreviewService {

    private final ProductService productService;

    @Override
    public OrderPreviewView preview(PreviewOrderRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "订单商品不能为空");
        }
        if (request.getFulfillType() == null
                || (request.getFulfillType() != 1 && request.getFulfillType() != 2)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "履约方式非法");
        }

        long goodsAmount = 0L;
        for (OrderItemRequest item : request.getItems()) {
            if (item.getProductId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BizException(ErrorCode.BAD_REQUEST, "订单商品参数非法");
            }
            Product product = productService.requireOnSale(item.getProductId());
            goodsAmount = Math.addExact(goodsAmount,
                    Math.multiplyExact(product.getPrice(), item.getQuantity().longValue()));
        }

        long deliveryFee = OrderAmounts.deliveryFee(request.getFulfillType(), goodsAmount);
        // TODO 优惠券接入后按券规则计算
        long discountAmount = 0L;
        long payAmount = goodsAmount + deliveryFee - discountAmount;
        return new OrderPreviewView(goodsAmount, deliveryFee, discountAmount, payAmount);
    }
}
