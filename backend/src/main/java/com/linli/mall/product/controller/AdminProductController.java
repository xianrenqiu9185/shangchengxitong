package com.linli.mall.product.controller;

import com.linli.mall.common.api.PageResult;
import com.linli.mall.common.api.Result;
import com.linli.mall.common.auth.RequirePermission;
import com.linli.mall.product.dto.CategoryRequest;
import com.linli.mall.product.dto.CategoryView;
import com.linli.mall.product.dto.ProductRequest;
import com.linli.mall.product.dto.ProductStatusRequest;
import com.linli.mall.product.dto.ProductView;
import com.linli.mall.product.service.CategoryService;
import com.linli.mall.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminProductController {

    private final CategoryService categoryService;
    private final ProductService productService;

    @GetMapping("/categories")
    @RequirePermission("product:category:manage")
    public Result<List<CategoryView>> categories() {
        return Result.ok(categoryService.listAdmin());
    }

    @PostMapping("/categories")
    @RequirePermission("product:category:manage")
    public Result<CategoryView> createCategory(@Valid @RequestBody CategoryRequest request) {
        return Result.ok(categoryService.create(request));
    }

    @PutMapping("/categories/{id}")
    @RequirePermission("product:category:manage")
    public Result<CategoryView> updateCategory(@PathVariable Long id,
                                               @Valid @RequestBody CategoryRequest request) {
        return Result.ok(categoryService.update(id, request));
    }

    @DeleteMapping("/categories/{id}")
    @RequirePermission("product:category:manage")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.ok();
    }

    @GetMapping("/products")
    @RequirePermission("product:goods:manage")
    public Result<PageResult<ProductView>> products(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(productService.listAdmin(categoryId, keyword, status, pageNum, pageSize));
    }

    @PostMapping("/products")
    @RequirePermission("product:goods:manage")
    public Result<ProductView> createProduct(@Valid @RequestBody ProductRequest request) {
        return Result.ok(productService.create(request));
    }

    @PutMapping("/products/{id}")
    @RequirePermission("product:goods:manage")
    public Result<ProductView> updateProduct(@PathVariable Long id,
                                             @Valid @RequestBody ProductRequest request) {
        return Result.ok(productService.update(id, request));
    }

    @PutMapping("/products/{id}/status")
    @RequirePermission("product:goods:manage")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody ProductStatusRequest request) {
        productService.updateStatus(id, request.status());
        return Result.ok();
    }

    @DeleteMapping("/products/{id}")
    @RequirePermission("product:goods:manage")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        productService.delete(id);
        return Result.ok();
    }
}
