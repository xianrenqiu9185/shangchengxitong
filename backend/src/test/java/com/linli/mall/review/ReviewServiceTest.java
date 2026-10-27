package com.linli.mall.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linli.mall.common.api.ErrorCode;
import com.linli.mall.common.exception.BizException;
import com.linli.mall.product.entity.Product;
import com.linli.mall.product.mapper.ProductMapper;
import com.linli.mall.review.dto.ReviewCreateRequest;
import com.linli.mall.review.dto.ReviewReplyRequest;
import com.linli.mall.review.dto.ReviewView;
import com.linli.mall.review.entity.Review;
import com.linli.mall.review.integration.ReviewOrderItem;
import com.linli.mall.review.integration.ReviewOrderPort;
import com.linli.mall.review.mapper.ReviewMapper;
import com.linli.mall.review.service.impl.ReviewServiceImpl;
import com.linli.mall.user.entity.User;
import com.linli.mall.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewMapper reviewMapper;
    @Mock private ProductMapper productMapper;
    @Mock private UserMapper userMapper;
    @Mock private ReviewOrderPort reviewOrderPort;

    private ReviewServiceImpl reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewServiceImpl(reviewMapper, productMapper, userMapper,
                reviewOrderPort, new ObjectMapper());
    }

    @Test
    void createShouldUseLoginUserAndPersistReview() {
        when(reviewOrderPort.findReviewableItem(3L, "LL1", null, null, 1L))
                .thenReturn(new ReviewOrderItem(11L, 21L, 1L, "苹果"));
        when(reviewMapper.insert(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            review.setId(9L);
            return 1;
        });
        when(productMapper.selectById(1L)).thenReturn(product());
        when(userMapper.selectById(3L)).thenReturn(user());

        ReviewView view = reviewService.create(3L, new ReviewCreateRequest(
                "LL1", null, null, 1L, 5, null, "很好吃", List.of("https://img/1.jpg"), false));

        assertThat(view.id()).isEqualTo(9L);
        assertThat(view.productName()).isEqualTo("苹果");
        assertThat(view.rating()).isEqualTo(5);
        assertThat(view.user()).isEqualTo("小邻");
        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewMapper).insert(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(3L);
        assertThat(captor.getValue().getOrderItemId()).isEqualTo(21L);
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
    }

    @Test
    void createShouldRejectOrderThatCannotBeReviewed() {
        when(reviewOrderPort.findReviewableItem(3L, "LL1", null, null, 1L)).thenReturn(null);

        assertThatThrownBy(() -> reviewService.create(3L, new ReviewCreateRequest(
                "LL1", null, null, 1L, 5, null, "很好吃", null, false)))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.REVIEW_NOT_ALLOWED);
        verify(reviewMapper, never()).insert(any(Review.class));
    }

    @Test
    void createShouldMapDuplicateOrderItemToReviewDuplicate() {
        when(reviewOrderPort.findReviewableItem(3L, "LL1", null, null, 1L))
                .thenReturn(new ReviewOrderItem(11L, 21L, 1L, "苹果"));
        when(reviewMapper.insert(any(Review.class))).thenThrow(new DuplicateKeyException("uk_order_item"));

        assertThatThrownBy(() -> reviewService.create(3L, new ReviewCreateRequest(
                "LL1", null, null, 1L, 5, null, "很好吃", null, false)))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.REVIEW_DUPLICATE);
    }

    @Test
    void replyShouldRejectMissingReview() {
        when(reviewMapper.reply(99L, "谢谢")).thenReturn(0);

        assertThatThrownBy(() -> reviewService.reply(99L, new ReviewReplyRequest("谢谢")))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.REVIEW_NOT_FOUND);
    }

    @Test
    void createShouldRejectImagesThatExceedColumnLimit() {
        when(reviewOrderPort.findReviewableItem(3L, "LL1", null, null, 1L))
                .thenReturn(new ReviewOrderItem(11L, 21L, 1L, "苹果"));

        assertThatThrownBy(() -> reviewService.create(3L, new ReviewCreateRequest(
                "LL1", null, null, 1L, 5, null, "很好吃",
                List.of("https://img/" + "a".repeat(490), "https://img/" + "b".repeat(490)), false)))
                .isInstanceOf(BizException.class)
                .extracting(error -> ((BizException) error).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
        verify(reviewMapper, never()).insert(any(Review.class));
    }

    private Product product() {
        Product product = new Product();
        product.setId(1L);
        product.setName("苹果");
        product.setStatus(1);
        return product;
    }

    private User user() {
        User user = new User();
        user.setId(3L);
        user.setPhone("13800000003");
        user.setNickname("小邻");
        return user;
    }
}
