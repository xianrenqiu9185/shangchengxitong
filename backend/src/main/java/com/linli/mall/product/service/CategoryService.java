package com.linli.mall.product.service;

import com.linli.mall.product.dto.CategoryRequest;
import com.linli.mall.product.dto.CategoryView;

import java.util.List;

public interface CategoryService {

    List<CategoryView> listPublic();

    List<CategoryView> listAdmin();

    CategoryView create(CategoryRequest request);

    CategoryView update(Long id, CategoryRequest request);

    void delete(Long id);
}
