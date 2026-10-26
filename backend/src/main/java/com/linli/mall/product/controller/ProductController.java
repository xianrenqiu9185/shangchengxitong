package com.linli.mall.product.controller;

import com.linli.mall.common.api.PageResult;
import com.linli.mall.common.api.Result;
import com.linli.mall.product.dto.CategoryView;
import com.linli.mall.product.dto.ProductView;
import com.linli.mall.product.service.CategoryService;
import com.linli.mall.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProductController {

    private final CategoryService categoryService;
    private final ProductService productService;

    @GetMapping("/categories")
    public Result<List<CategoryView>> categories() {
        return Result.ok(categoryService.listPublic());
    }

    @GetMapping("/products")
    public Result<PageResult<ProductView>> products(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(productService.listPublic(categoryId, keyword, pageNum, pageSize));
    }

    @GetMapping("/products/{id}")
    public Result<ProductView> product(@PathVariable Long id) {
        return Result.ok(productService.getPublic(id));
    }
}
