package com.linli.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linli.mall.common.api.ErrorCode;
import com.linli.mall.common.api.PageResult;
import com.linli.mall.common.exception.BizException;
import com.linli.mall.inventory.entity.Inventory;
import com.linli.mall.inventory.mapper.InventoryMapper;
import com.linli.mall.product.cache.ProductCache;
import com.linli.mall.product.dto.ProductRequest;
import com.linli.mall.product.dto.ProductView;
import com.linli.mall.product.entity.Category;
import com.linli.mall.product.entity.Product;
import com.linli.mall.product.mapper.CategoryMapper;
import com.linli.mall.product.mapper.ProductMapper;
import com.linli.mall.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final InventoryMapper inventoryMapper;
    private final ProductCache productCache;

    @Override
    public PageResult<ProductView> listPublic(Long categoryId, String keyword, long pageNum, long pageSize) {
        return query(categoryId, keyword, 1, pageNum, pageSize);
    }

    @Override
    public ProductView getPublic(Long id) {
        Product product = productCache.getProduct(id);
        if (product == null) {
            product = productMapper.selectById(id);
            if (product != null) {
                productCache.putProduct(product);
            }
        }
        if (product == null || !Integer.valueOf(1).equals(product.getStatus())) {
            throw new BizException(ErrorCode.PRODUCT_UNAVAILABLE);
        }
        return toViews(List.of(product)).get(0);
    }

    @Override
    public Product requireOnSale(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null || !Integer.valueOf(1).equals(product.getStatus())) {
            throw new BizException(ErrorCode.PRODUCT_UNAVAILABLE);
        }
        return product;
    }

    @Override
    public PageResult<ProductView> listAdmin(Long categoryId, String keyword, Integer status,
                                             long pageNum, long pageSize) {
        return query(categoryId, keyword, status, pageNum, pageSize);
    }

    @Override
    @Transactional
    public ProductView create(ProductRequest request) {
        requireCategory(request.categoryId());
        Product product = new Product();
        fill(product, request);
        product.setStatus(request.status() == null ? 1 : request.status());
        productMapper.insert(product);
        productCache.evictProduct(product.getId());
        return toViews(List.of(product)).get(0);
    }

    @Override
    @Transactional
    public ProductView update(Long id, ProductRequest request) {
        Product product = requireProduct(id);
        requireCategory(request.categoryId());
        fill(product, request);
        if (request.status() != null) {
            product.setStatus(request.status());
        }
        productMapper.updateById(product);
        productCache.evictProduct(id);
        return toViews(List.of(product)).get(0);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        requireProduct(id);
        int affected = productMapper.updateStatus(id, status);
        if (affected == 0) {
            throw new BizException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        productCache.evictProduct(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireProduct(id);
        productMapper.deleteById(id);
        productCache.evictProduct(id);
    }

    private PageResult<ProductView> query(Long categoryId, String keyword, Integer status,
                                          long pageNum, long pageSize) {
        long safePageNum = Math.max(pageNum, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 100);
        String safeKeyword = normalizeKeyword(keyword);

        LambdaQueryWrapper<Product> query = new LambdaQueryWrapper<Product>()
                .eq(categoryId != null, Product::getCategoryId, categoryId)
                .eq(status != null, Product::getStatus, status)
                .like(safeKeyword != null, Product::getName, safeKeyword)
                .orderByDesc(Product::getUpdateTime)
                .orderByDesc(Product::getId);
        Page<Product> page = productMapper.selectPage(new Page<>(safePageNum, safePageSize), query);
        return PageResult.of(toViews(page.getRecords()), page.getTotal(), safePageNum, safePageSize);
    }

    private List<ProductView> toViews(Collection<Product> products) {
        if (products.isEmpty()) {
            return List.of();
        }
        List<Long> categoryIds = products.stream().map(Product::getCategoryId).distinct().toList();
        List<Long> productIds = products.stream().map(Product::getId).toList();
        Map<Long, String> categoryNames = categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
        Map<Long, Inventory> inventories = productIds.isEmpty()
                ? Collections.emptyMap()
                : inventoryMapper.selectList(new LambdaQueryWrapper<Inventory>().in(Inventory::getProductId, productIds))
                .stream()
                .collect(Collectors.toMap(Inventory::getProductId, Function.identity()));
        return products.stream().map(product -> {
            Inventory inventory = inventories.get(product.getId());
            int stock = inventory == null ? 0 : Math.max(inventory.available(), 0);
            int sales = inventory == null ? 0 : inventory.getSold();
            return new ProductView(product.getId(), product.getCategoryId(), categoryNames.get(product.getCategoryId()),
                    product.getName(), product.getSpec(), product.getSpec(), product.getSpec(), product.getImage(),
                    product.getDescription(), product.getPrice(), null, product.getStatus(), stock, sales,
                    product.getCreateTime(), product.getUpdateTime(), product.getUpdateTime());
        }).toList();
    }

    private void fill(Product product, ProductRequest request) {
        product.setCategoryId(request.categoryId());
        product.setName(request.name().trim());
        product.setSpec(trimToNull(request.spec()));
        product.setImage(trimToNull(request.image()));
        product.setDescription(request.description());
        product.setPrice(request.price());
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String value = keyword.trim();
        if (value.length() > 32) {
            throw new BizException(ErrorCode.BAD_REQUEST, "搜索关键词不能超过 32 个字符");
        }
        return value;
    }

    private Product requireProduct(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BizException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return product;
    }

    private void requireCategory(Long categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BizException(ErrorCode.PRODUCT_INVALID, "分类不存在");
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
