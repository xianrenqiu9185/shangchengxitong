package com.linli.mall.product.service;

import com.linli.mall.common.api.PageResult;
import com.linli.mall.product.dto.ProductRequest;
import com.linli.mall.product.dto.ProductView;

public interface ProductService {

    PageResult<ProductView> listPublic(Long categoryId, String keyword, long pageNum, long pageSize);

    ProductView getPublic(Long id);

    PageResult<ProductView> listAdmin(Long categoryId, String keyword, Integer status, long pageNum, long pageSize);

    ProductView create(ProductRequest request);

    ProductView update(Long id, ProductRequest request);

    void updateStatus(Long id, Integer status);

    void delete(Long id);
}
