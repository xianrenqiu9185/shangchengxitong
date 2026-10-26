package com.linli.mall.product;

import com.linli.mall.common.api.ErrorCode;
import com.linli.mall.common.exception.BizException;
import com.linli.mall.product.cache.ProductCache;
import com.linli.mall.product.dto.CategoryRequest;
import com.linli.mall.product.entity.Category;
import com.linli.mall.product.mapper.CategoryMapper;
import com.linli.mall.product.mapper.ProductMapper;
import com.linli.mall.product.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private CategoryMapper categoryMapper;
    @Mock private ProductMapper productMapper;
    @Mock private ProductCache productCache;
    @InjectMocks private CategoryServiceImpl categoryService;

    @Test
    void createShouldRejectDuplicateName() {
        when(categoryMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> categoryService.create(new CategoryRequest("水果", 1, 1)))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_NAME_DUPLICATE);
        verify(categoryMapper, never()).insert(any(Category.class));
    }

    @Test
    void deleteShouldRejectCategoryWithProducts() {
        when(categoryMapper.selectById(1L)).thenReturn(category());
        when(productMapper.selectCount(any())).thenReturn(2L);

        assertThatThrownBy(() -> categoryService.delete(1L))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_IN_USE);
        verify(categoryMapper, never()).deleteById(1L);
    }

    @Test
    void createShouldEvictCategoryCache() {
        when(categoryMapper.selectCount(any())).thenReturn(0L);
        when(categoryMapper.insert(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setId(5L);
            return 1;
        });

        categoryService.create(new CategoryRequest("水果", 10, 1));

        verify(productCache).evictCategories();
    }

    private Category category() {
        Category category = new Category();
        category.setId(1L);
        category.setName("水果");
        category.setSort(10);
        category.setStatus(1);
        return category;
    }
}
