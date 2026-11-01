package com.linli.mall.aftersale;

import com.linli.mall.aftersale.dto.AfterSaleAuditRequest;
import com.linli.mall.aftersale.dto.AfterSaleCreateRequest;
import com.linli.mall.aftersale.dto.AfterSaleView;
import com.linli.mall.aftersale.entity.AfterSale;
import com.linli.mall.aftersale.integration.OrderInfo;
import com.linli.mall.aftersale.integration.OrderPort;
import com.linli.mall.aftersale.integration.RefundPort;
import com.linli.mall.aftersale.integration.RefundResult;
import com.linli.mall.aftersale.mapper.AfterSaleMapper;
import com.linli.mall.aftersale.service.impl.AfterSaleServiceImpl;
import com.linli.mall.common.api.ErrorCode;
import com.linli.mall.common.exception.BizException;
import com.linli.mall.common.idempotency.IdempotencyClaim;
import com.linli.mall.common.idempotency.IdempotencyService;
import com.linli.mall.user.entity.User;
import com.linli.mall.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AfterSaleServiceTest {

    @Mock private AfterSaleMapper afterSaleMapper;
    @Mock private UserMapper userMapper;
    @Mock private IdempotencyService idempotencyService;
    @Mock private ObjectProvider<OrderPort> orderPortProvider;
    @Mock private ObjectProvider<RefundPort> refundPortProvider;
    @Mock private OrderPort orderPort;
    @Mock private RefundPort refundPort;

    private AfterSaleServiceImpl afterSaleService;

    @BeforeEach
    void setUp() {
        afterSaleService = new AfterSaleServiceImpl(afterSaleMapper, userMapper, idempotencyService,
                orderPortProvider, refundPortProvider);
    }

    @Test
    void applyShouldMarkOrderAndPersistPendingSale() {
        when(idempotencyService.claim(eq(3L), eq("AFTER_SALE_APPLY"), eq("key-1"), anyString()))
                .thenReturn(new IdempotencyClaim("generated", true, false));
        when(orderPortProvider.getIfAvailable()).thenReturn(orderPort);
        OrderInfo order = new OrderInfo(11L, "LL202610260001", 3L, 2, 4590L);
        when(orderPort.findByOrderNoForUser("LL202610260001", 3L)).thenReturn(order);
        when(afterSaleMapper.selectCount(any())).thenReturn(0L);
        when(afterSaleMapper.insert(any(AfterSale.class))).thenAnswer(invocation -> {
            AfterSale sale = invocation.getArgument(0);
            sale.setId(8L);
            return 1;
        });
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(user()));

        AfterSaleView view = afterSaleService.apply(3L, "key-1",
                new AfterSaleCreateRequest("LL202610260001", "REFUND", "商品破损", "外包装压坏"));

        assertThat(view.saleNo()).startsWith("AS");
        assertThat(view.orderNo()).isEqualTo("LL202610260001");
        assertThat(view.status()).isEqualTo("PENDING");
        assertThat(view.reason()).isEqualTo("商品破损");
        assertThat(view.description()).isEqualTo("外包装压坏");
        assertThat(view.amount()).isEqualTo(4590L);
        verify(orderPort).markAfterSale(11L, 2);
        verify(idempotencyService).succeed(eq(3L), eq("AFTER_SALE_APPLY"), eq("key-1"), anyString());
    }

    @Test
    void applyShouldRejectOrderStateNotEligible() {
        when(idempotencyService.claim(eq(3L), eq("AFTER_SALE_APPLY"), eq("key-1"), anyString()))
                .thenReturn(new IdempotencyClaim("generated", true, false));
        when(orderPortProvider.getIfAvailable()).thenReturn(orderPort);
        when(orderPort.findByOrderNoForUser("LL1", 3L))
                .thenReturn(new OrderInfo(11L, "LL1", 3L, 4, 4590L));

        assertThatThrownBy(() -> afterSaleService.apply(3L, "key-1",
                new AfterSaleCreateRequest("LL1", "REFUND", "商品破损", null)))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.AFTER_SALE_STATUS_INVALID);
        verify(orderPort, never()).markAfterSale(anyLong(), anyInt());
    }

    @Test
    void approveShouldCallRefundAndCompleteSale() {
        when(afterSaleMapper.selectOne(any())).thenReturn(pendingSale(), completedSale());
        when(idempotencyService.claim(eq(1L), eq("AFTER_SALE_AUDIT"), eq("audit-1"), anyString()))
                .thenReturn(new IdempotencyClaim("AS1", true, false));
        when(afterSaleMapper.approve(eq(8L), anyString(), eq("审核通过"))).thenReturn(1);
        when(refundPortProvider.getIfAvailable()).thenReturn(refundPort);
        when(refundPort.refund(any())).thenReturn(RefundResult.success("RF1"));
        when(afterSaleMapper.complete(8L)).thenReturn(1);
        when(orderPortProvider.getIfAvailable()).thenReturn(orderPort);
        when(orderPort.findByIds(any())).thenReturn(Map.of(11L,
                new OrderInfo(11L, "LL1", 3L, 3, 4590L)));
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(user()));

        AfterSaleView view = afterSaleService.audit(1L, "AS1", "audit-1",
                new AfterSaleAuditRequest(true, "审核通过"));

        assertThat(view.status()).isEqualTo("COMPLETED");
        assertThat(view.refundNo()).isEqualTo("RF1");
        verify(refundPort).refund(any());
        verify(orderPort).markRefunded(11L);
    }

    @Test
    void auditShouldRejectAlreadyProcessedSale() {
        when(afterSaleMapper.selectOne(any())).thenReturn(pendingSale());
        when(idempotencyService.claim(eq(1L), eq("AFTER_SALE_AUDIT"), eq("audit-1"), anyString()))
                .thenReturn(new IdempotencyClaim("AS1", true, false));
        when(afterSaleMapper.approve(eq(8L), anyString(), anyString())).thenReturn(0);

        assertThatThrownBy(() -> afterSaleService.audit(1L, "AS1", "audit-1",
                new AfterSaleAuditRequest(true, "同意")))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.AFTER_SALE_STATUS_INVALID);
        verify(idempotencyService).fail(1L, "AFTER_SALE_AUDIT", "audit-1");
    }

    private AfterSale pendingSale() {
        AfterSale sale = baseSale();
        sale.setStatus(0);
        return sale;
    }

    private AfterSale completedSale() {
        AfterSale sale = baseSale();
        sale.setStatus(4);
        sale.setRefundNo("RF1");
        return sale;
    }

    private AfterSale baseSale() {
        AfterSale sale = new AfterSale();
        sale.setId(8L);
        sale.setSaleNo("AS1");
        sale.setOrderId(11L);
        sale.setUserId(3L);
        sale.setType(1);
        sale.setReason("商品破损");
        sale.setAmount(4590L);
        sale.setPreStatus(2);
        return sale;
    }

    private User user() {
        User user = new User();
        user.setId(3L);
        user.setPhone("13800000003");
        user.setNickname("邻里会员");
        return user;
    }
}
