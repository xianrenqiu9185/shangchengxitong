package com.linli.mall.product.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linli.mall.product.dto.CategoryView;
import com.linli.mall.product.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductCache {

    private static final String CATEGORIES_KEY = "cache:categories";
    private static final String PRODUCT_PREFIX = "cache:product:";
    private static final Duration CATEGORY_TTL = Duration.ofHours(1);
    private static final Duration PRODUCT_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public List<CategoryView> getCategories() {
        try {
            String json = redisTemplate.opsForValue().get(CATEGORIES_KEY);
            return json == null ? null : objectMapper.readValue(json, new TypeReference<>() { });
        } catch (Exception e) {
            log.warn("读取分类缓存失败，回退数据库", e);
            return null;
        }
    }

    public void putCategories(List<CategoryView> categories) {
        try {
            redisTemplate.opsForValue().set(CATEGORIES_KEY, objectMapper.writeValueAsString(categories), CATEGORY_TTL);
        } catch (Exception e) {
            log.warn("写入分类缓存失败", e);
        }
    }

    public void evictCategories() {
        redisTemplate.delete(CATEGORIES_KEY);
    }

    public Product getProduct(Long id) {
        try {
            String json = redisTemplate.opsForValue().get(PRODUCT_PREFIX + id);
            return json == null ? null : objectMapper.readValue(json, Product.class);
        } catch (Exception e) {
            log.warn("读取商品缓存失败，productId={}", id, e);
            return null;
        }
    }

    public void putProduct(Product product) {
        try {
            redisTemplate.opsForValue().set(PRODUCT_PREFIX + product.getId(),
                    objectMapper.writeValueAsString(product), PRODUCT_TTL);
        } catch (Exception e) {
            log.warn("写入商品缓存失败，productId={}", product.getId(), e);
        }
    }

    public void evictProduct(Long id) {
        if (id != null) {
            redisTemplate.delete(PRODUCT_PREFIX + id);
        }
    }
}
