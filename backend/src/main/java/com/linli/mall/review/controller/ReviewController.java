package com.linli.mall.review.controller;

import com.linli.mall.common.api.PageResult;
import com.linli.mall.common.api.Result;
import com.linli.mall.common.auth.RequirePermission;
import com.linli.mall.common.auth.UserContext;
import com.linli.mall.review.dto.ReviewCreateRequest;
import com.linli.mall.review.dto.ReviewView;
import com.linli.mall.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/reviews")
    @RequirePermission("review:goods:create")
    public Result<ReviewView> create(@Valid @RequestBody ReviewCreateRequest request) {
        return Result.ok(reviewService.create(UserContext.userId(), request));
    }

    @GetMapping("/products/{productId}/reviews")
    public Result<PageResult<ReviewView>> listByProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "20") long pageSize) {
        return Result.ok(reviewService.listByProduct(productId, pageNum, pageSize));
    }
}
