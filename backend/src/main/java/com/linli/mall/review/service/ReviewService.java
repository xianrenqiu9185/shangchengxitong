package com.linli.mall.review.service;

import com.linli.mall.common.api.PageResult;
import com.linli.mall.review.dto.AdminReviewView;
import com.linli.mall.review.dto.ReviewCreateRequest;
import com.linli.mall.review.dto.ReviewReplyRequest;
import com.linli.mall.review.dto.ReviewView;

public interface ReviewService {

    ReviewView create(Long userId, ReviewCreateRequest request);

    PageResult<ReviewView> listByProduct(Long productId, long pageNum, long pageSize);

    PageResult<AdminReviewView> listAdmin(String keyword, Long productId, Integer score,
                                          Integer status, long pageNum, long pageSize);

    AdminReviewView reply(Long reviewId, ReviewReplyRequest request);
}
