package com.linli.mall.order.service;

/**
 * 支付成功后的订单侧动作：订单转待发货 + 库存 locked→sold。
 */
public interface OrderPaymentService {

    /**
     * @return true 订单已支付（含幂等重复）；false 订单已关闭（需退款补偿）
     */
    boolean markPaid(String orderNo);
}
