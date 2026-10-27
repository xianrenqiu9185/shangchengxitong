package com.linli.mall.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 兼容接口文档的 orderId/orderItemId/score，以及 C 端现有的 orderNo/productId/rating。
 * user_id 不从请求体接收，统一取登录态。
 */
public record ReviewCreateRequest(
        @Size(max = 32, message = "订单号格式不正确") String orderNo,
        Long orderId,
        Long orderItemId,
        Long productId,
        @Min(value = 1, message = "评分必须在 1-5 之间")
        @Max(value = 5, message = "评分必须在 1-5 之间") Integer rating,
        @Min(value = 1, message = "评分必须在 1-5 之间")
        @Max(value = 5, message = "评分必须在 1-5 之间") Integer score,
        @Size(max = 1000, message = "评价内容不能超过 1000 个字符") String content,
        @Size(max = 9, message = "最多上传 9 张图片")
        List<@Size(max = 500, message = "图片地址过长") String> images,
        Boolean anonymous
) {
}
