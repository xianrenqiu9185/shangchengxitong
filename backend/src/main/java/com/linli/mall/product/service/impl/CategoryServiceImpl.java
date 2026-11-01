package com.linli.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linli.mall.common.api.ErrorCode;
import com.linli.mall.common.exception.BizException;
import com.linli.mall.product.cache.ProductCache;
import com.linli.mall.product.dto.CategoryRequest;
import com.linli.mall.product.dto.CategoryView;
import com.linli.mall.product.entity.Category;
import com.linli.mall.product.entity.Product;
import com.linli.mall.product.mapper.CategoryMapper;
import com.linli.mall.product.mapper.ProductMapper;
import com.linli.mall.product.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final ProductCache productCache;

    @Override
    public List<CategoryView> listPublic() {
        List<CategoryView> cached = productCache.getCategories();
        if (cached != null) {
            return cached;
        }
        List<CategoryView> categories = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                        .eq(Category::getStatus, 1)
                        .orderByAsc(Category::getSort)
                        .orderByAsc(Category::getId))
                .stream()
                .map(this::toView)
                .toList();
        productCache.putCategories(categories);
        return categories;
    }

    @Override
    public List<CategoryView> listAdmin() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                        .orderByAsc(Category::getSort)
                        .orderByAsc(Category::getId))
                .stream()
                .map(this::toView)
                .toList();
    }

    @Override
    @Transactional
    public CategoryView create(CategoryRequest request) {
        String name = request.name().trim();
        requireUniqueName(name, null);
        Category category = new Category();
        category.setName(name);
        category.setSort(request.sort() == null ? 0 : request.sort());
        category.setStatus(request.status() == null ? 1 : request.status());
        categoryMapper.insert(category);
        productCache.evictCategories();
        return toView(category);
    }

    @Override
    @Transactional
    public CategoryView update(Long id, CategoryRequest request) {
        Category category = requireCategory(id);
        String name = request.name().trim();
        requireUniqueName(name, id);
        category.setName(name);
        category.setSort(request.sort() == null ? category.getSort() : request.sort());
        category.setStatus(request.status() == null ? category.getStatus() : request.status());
        categoryMapper.updateById(category);
        productCache.evictCategories();
        return toView(category);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireCategory(id);
        Long products = productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getCategoryId, id));
        if (products != null && products > 0) {
            throw new BizException(ErrorCode.CATEGORY_IN_USE);
        }
        categoryMapper.deleteById(id);
        productCache.evictCategories();
    }

    private Category requireCategory(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "分类不存在");
        }
        return category;
    }

    private void requireUniqueName(String name, Long excludeId) {
        LambdaQueryWrapper<Category> query = new LambdaQueryWrapper<Category>().eq(Category::getName, name);
        if (excludeId != null) {
            query.ne(Category::getId, excludeId);
        }
        Long count = categoryMapper.selectCount(query);
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.CATEGORY_NAME_DUPLICATE);
        }
    }

    private CategoryView toView(Category category) {
        return new CategoryView(category.getId(), category.getName(), category.getSort(), category.getStatus());
    }
}
