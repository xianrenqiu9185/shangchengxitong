package com.linli.mall.review.integration;

/**
 * 评价模块到订单模块的单向 Port，由订单模块实现。
 */
public interface ReviewOrderPort {

    /**
     * 查询当前会员在已完成订单中的可评价订单项。
     * 订单不属于该会员、订单未完成或订单项不匹配时返回 null。
     */
    ReviewOrderItem findReviewableItem(Long userId, String orderNo, Long orderId,
                                       Long orderItemId, Long productId);
}
