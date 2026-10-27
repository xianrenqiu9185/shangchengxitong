package com.linli.mall.review.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linli.mall.common.api.ErrorCode;
import com.linli.mall.common.api.PageResult;
import com.linli.mall.common.exception.BizException;
import com.linli.mall.product.entity.Product;
import com.linli.mall.product.mapper.ProductMapper;
import com.linli.mall.review.dto.AdminReviewView;
import com.linli.mall.review.dto.ReviewCreateRequest;
import com.linli.mall.review.dto.ReviewReplyRequest;
import com.linli.mall.review.dto.ReviewView;
import com.linli.mall.review.entity.Review;
import com.linli.mall.review.integration.ReviewOrderItem;
import com.linli.mall.review.integration.ReviewOrderPort;
import com.linli.mall.review.mapper.ReviewMapper;
import com.linli.mall.review.service.ReviewService;
import com.linli.mall.user.entity.User;
import com.linli.mall.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private static final TypeReference<List<String>> IMAGE_LIST_TYPE = new TypeReference<>() {
    };

    private final ReviewMapper reviewMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final ReviewOrderPort reviewOrderPort;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ReviewView create(Long userId, ReviewCreateRequest request) {
        int score = resolveScore(request);
        String content = normalizeContent(request.content());
        ReviewOrderItem item = reviewOrderPort.findReviewableItem(
                userId, trimToNull(request.orderNo()), request.orderId(),
                request.orderItemId(), request.productId());
        if (item == null) {
            throw new BizException(ErrorCode.REVIEW_NOT_ALLOWED);
        }

        Review review = new Review();
        review.setOrderId(item.orderId());
        review.setOrderItemId(item.orderItemId());
        review.setUserId(userId);
        review.setProductId(item.productId());
        review.setScore(score);
        review.setContent(content);
        review.setImages(writeImages(request.images()));
        review.setStatus(1);
        review.setCreateTime(LocalDateTime.now());
        review.setUpdateTime(review.getCreateTime());
        try {
            if (reviewMapper.insert(review) != 1) {
                throw new BizException(ErrorCode.SYSTEM_ERROR);
            }
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.REVIEW_DUPLICATE);
        }

        Product product = productMapper.selectById(item.productId());
        User user = userMapper.selectById(userId);
        return toReviewView(review, product, user);
    }

    @Override
    public PageResult<ReviewView> listByProduct(Long productId, long pageNum, long pageSize) {
        Product product = productMapper.selectById(productId);
        if (product == null || !Integer.valueOf(1).equals(product.getStatus())) {
            throw new BizException(ErrorCode.PRODUCT_UNAVAILABLE);
        }
        long safePageNum = Math.max(pageNum, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 100);
        Page<Review> page = reviewMapper.selectPage(new Page<>(safePageNum, safePageSize),
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getProductId, productId)
                        .eq(Review::getStatus, 1)
                        .orderByDesc(Review::getCreateTime)
                        .orderByDesc(Review::getId));
        List<Review> records = page.getRecords();
        Map<Long, User> users = usersByIds(records.stream().map(Review::getUserId).distinct().toList());
        List<ReviewView> views = records.stream()
                .map(review -> toReviewView(review, product, users.get(review.getUserId())))
                .toList();
        return PageResult.of(views, page.getTotal(), safePageNum, safePageSize);
    }

    @Override
    public PageResult<AdminReviewView> listAdmin(String keyword, Long productId, Integer score,
                                                 Integer status, long pageNum, long pageSize) {
        long safePageNum = Math.max(pageNum, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 100);
        String safeKeyword = trimToNull(keyword);
        List<Long> matchingProductIds = matchingProductIds(safeKeyword);
        List<Long> matchingUserIds = matchingUserIds(safeKeyword);

        LambdaQueryWrapper<Review> query = new LambdaQueryWrapper<Review>()
                .eq(productId != null, Review::getProductId, productId)
                .eq(score != null, Review::getScore, score)
                .eq(status != null, Review::getStatus, status);
        if (safeKeyword != null) {
            query.and(wrapper -> {
                wrapper.like(Review::getContent, safeKeyword);
                if (!matchingProductIds.isEmpty()) {
                    wrapper.or().in(Review::getProductId, matchingProductIds);
                }
                if (!matchingUserIds.isEmpty()) {
                    wrapper.or().in(Review::getUserId, matchingUserIds);
                }
            });
        }
        query.orderByDesc(Review::getCreateTime).orderByDesc(Review::getId);

        Page<Review> page = reviewMapper.selectPage(new Page<>(safePageNum, safePageSize), query);
        List<Review> records = page.getRecords();
        Map<Long, Product> products = productsByIds(records.stream().map(Review::getProductId).distinct().toList());
        Map<Long, User> users = usersByIds(records.stream().map(Review::getUserId).distinct().toList());
        List<AdminReviewView> views = records.stream()
                .map(review -> toAdminView(review, products.get(review.getProductId()), users.get(review.getUserId())))
                .toList();
        return PageResult.of(views, page.getTotal(), safePageNum, safePageSize);
    }

    @Override
    @Transactional
    public AdminReviewView reply(Long reviewId, ReviewReplyRequest request) {
        String reply = request.reply() == null ? null : request.reply().trim();
        if (reply == null || reply.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "回复内容不能为空");
        }
        if (reviewMapper.reply(reviewId, reply) == 0) {
            throw new BizException(ErrorCode.REVIEW_NOT_FOUND);
        }
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BizException(ErrorCode.REVIEW_NOT_FOUND);
        }
        Product product = productMapper.selectById(review.getProductId());
        User user = userMapper.selectById(review.getUserId());
        return toAdminView(review, product, user);
    }

    private int resolveScore(ReviewCreateRequest request) {
        Integer score = request.rating() == null ? request.score() : request.rating();
        if (score == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "评分不能为空");
        }
        if (request.rating() != null && request.score() != null && !request.rating().equals(request.score())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "rating 与 score 必须一致");
        }
        if (score < 1 || score > 5) {
            throw new BizException(ErrorCode.BAD_REQUEST, "评分必须在 1-5 之间");
        }
        return score;
    }

    private String normalizeContent(String content) {
        String value = trimToNull(content);
        if (value != null && value.length() > 1000) {
            throw new BizException(ErrorCode.BAD_REQUEST, "评价内容不能超过 1000 个字符");
        }
        return value;
    }

    private String writeImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        List<String> normalized = images.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(normalized);
            if (json.length() > 1000) {
                throw new BizException(ErrorCode.BAD_REQUEST, "图片地址总长度超出限制");
            }
            return json;
        } catch (JsonProcessingException e) {
            throw new BizException(ErrorCode.BAD_REQUEST, "图片地址格式不正确");
        }
    }

    private List<String> readImages(String images) {
        if (images == null || images.isBlank()) {
            return List.of();
        }
        try {
            return List.copyOf(objectMapper.readValue(images, IMAGE_LIST_TYPE));
        } catch (JsonProcessingException | IllegalArgumentException e) {
            return List.of();
        }
    }

    private List<Long> matchingProductIds(String keyword) {
        if (keyword == null) {
            return Collections.emptyList();
        }
        return productMapper.selectList(new LambdaQueryWrapper<Product>()
                        .like(Product::getName, keyword))
                .stream().map(Product::getId).toList();
    }

    private List<Long> matchingUserIds(String keyword) {
        if (keyword == null) {
            return Collections.emptyList();
        }
        return userMapper.selectList(new LambdaQueryWrapper<User>()
                        .and(wrapper -> wrapper.like(User::getNickname, keyword)
                                .or().like(User::getPhone, keyword)))
                .stream().map(User::getId).toList();
    }

    private Map<Long, Product> productsByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return productMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private Map<Long, User> usersByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private ReviewView toReviewView(Review review, Product product, User user) {
        return new ReviewView(review.getId(), review.getProductId(),
                product == null ? null : product.getName(), displayUser(user), review.getScore(),
                review.getContent(), readImages(review.getImages()), review.getReply(),
                review.getReplyTime(), review.getCreateTime());
    }

    private AdminReviewView toAdminView(Review review, Product product, User user) {
        return new AdminReviewView(review.getId(), review.getOrderId(), review.getOrderItemId(),
                review.getUserId(), user == null ? null : maskPhone(user.getPhone()), displayUser(user),
                review.getProductId(), product == null ? null : product.getName(), review.getScore(),
                review.getContent(), readImages(review.getImages()), review.getReply(),
                review.getReplyTime(), review.getStatus(), review.getCreateTime());
    }

    private String displayUser(User user) {
        if (user == null) {
            return "邻里会员";
        }
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname();
        }
        return maskPhone(user.getPhone());
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
