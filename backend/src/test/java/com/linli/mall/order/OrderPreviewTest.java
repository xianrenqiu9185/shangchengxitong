package com.linli.mall.order;

import com.linli.mall.common.exception.BizException;
import com.linli.mall.order.dto.CreateOrderRequest;
import com.linli.mall.order.dto.CreateOrderResponse;
import com.linli.mall.order.dto.OrderDetailView;
import com.linli.mall.order.dto.OrderItemRequest;
import com.linli.mall.order.dto.OrderPreviewView;
import com.linli.mall.order.dto.PreviewOrderRequest;
import com.linli.mall.order.service.OrderDetailService;
import com.linli.mall.order.service.OrderPreviewService;
import com.linli.mall.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderPreviewTest {

    private static final Long MEMBER_ID = 3L;

    @Autowired
    private OrderPreviewService orderPreviewService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Test
    void previewDeliveryShouldIncludeDeliveryFee() {
        OrderPreviewView preview = orderPreviewService.preview(previewRequest(1, 1L, 2));

        assertThat(preview.goodsAmount()).isEqualTo(2580L);
        assertThat(preview.deliveryFee()).isEqualTo(300L);
        assertThat(preview.discountAmount()).isZero();
        assertThat(preview.payAmount()).isEqualTo(2880L);
    }

    @Test
    void previewPickupShouldNotChargeDelivery() {
        OrderPreviewView preview = orderPreviewService.preview(previewRequest(2, 1L, 2));

        assertThat(preview.goodsAmount()).isEqualTo(2580L);
        assertThat(preview.deliveryFee()).isZero();
        assertThat(preview.payAmount()).isEqualTo(2580L);
    }

    @Test
    void previewShouldWaiveDeliveryOverThreshold() {
        // 商品 4 售价 3990 分，达到免配送门槛
        OrderPreviewView preview = orderPreviewService.preview(previewRequest(1, 4L, 1));

        assertThat(preview.goodsAmount()).isEqualTo(3990L);
        assertThat(preview.deliveryFee()).isZero();
        assertThat(preview.payAmount()).isEqualTo(3990L);
    }

    @Test
    void previewShouldRejectMissingProduct() {
        assertThatThrownBy(() -> orderPreviewService.preview(previewRequest(2, 999999L, 1)))
                .isInstanceOf(BizException.class);
    }

    @Test
    void createDeliveryOrderShouldMatchPreview() {
        OrderPreviewView preview = orderPreviewService.preview(previewRequest(1, 1L, 2));

        CreateOrderRequest request = new CreateOrderRequest();
        request.setFulfillType(1);
        request.setAddressId(1L);
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(1L);
        item.setQuantity(2);
        request.setItems(List.of(item));

        CreateOrderResponse order = orderService.createOrder(
                MEMBER_ID, UUID.randomUUID().toString(), request);

        assertThat(order.payAmount()).isEqualTo(preview.payAmount());

        OrderDetailView detail = orderDetailService.getDetail(order.orderNo(), MEMBER_ID);
        assertThat(detail.goodsAmount()).isEqualTo(preview.goodsAmount());
        assertThat(detail.deliveryFee()).isEqualTo(preview.deliveryFee());
        assertThat(detail.payAmount()).isEqualTo(preview.payAmount());
    }

    private PreviewOrderRequest previewRequest(int fulfillType, Long productId, int quantity) {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(productId);
        item.setQuantity(quantity);

        PreviewOrderRequest request = new PreviewOrderRequest();
        request.setFulfillType(fulfillType);
        request.setItems(List.of(item));
        return request;
    }
}
