package com.linli.mall.common;

import com.linli.mall.common.api.ErrorCode;
import com.linli.mall.common.exception.BizException;
import com.linli.mall.common.idempotency.IdempotencyClaim;
import com.linli.mall.common.idempotency.IdempotentMapper;
import com.linli.mall.common.idempotency.IdempotentRecord;
import com.linli.mall.common.idempotency.IdempotencyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock private IdempotentMapper idempotentMapper;
    @InjectMocks private IdempotencyService idempotencyService;

    @Test
    void firstRequestShouldAcquireKey() {
        when(idempotentMapper.insertIgnore("key", 3L, "APPLY", "NO1")).thenReturn(1);

        IdempotencyClaim claim = idempotencyService.claim(3L, "APPLY", "key", "NO1");

        assertThat(claim.acquired()).isTrue();
        assertThat(claim.bizNo()).isEqualTo("NO1");
    }

    @Test
    void completedRequestShouldReturnOriginalBizNo() {
        when(idempotentMapper.insertIgnore("key", 3L, "APPLY", "NO2")).thenReturn(0);
        when(idempotentMapper.selectOne(any())).thenReturn(record(1, "NO1"));

        IdempotencyClaim claim = idempotencyService.claim(3L, "APPLY", "key", "NO2");

        assertThat(claim.acquired()).isFalse();
        assertThat(claim.completed()).isTrue();
        assertThat(claim.bizNo()).isEqualTo("NO1");
    }

    @Test
    void inFlightRequestShouldBeRejected() {
        when(idempotentMapper.insertIgnore("key", 3L, "APPLY", "NO2")).thenReturn(0);
        when(idempotentMapper.selectOne(any())).thenReturn(record(0, "NO1"));

        assertThatThrownBy(() -> idempotencyService.claim(3L, "APPLY", "key", "NO2"))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_REQUEST);
    }

    @Test
    void missingKeyShouldBeRejectedWithoutDatabaseAccess() {
        assertThatThrownBy(() -> idempotencyService.claim(3L, "APPLY", " ", "NO1"))
                .isInstanceOf(BizException.class);
        verify(idempotentMapper, never()).insertIgnore(any(), any(), any(), any());
    }

    private IdempotentRecord record(int status, String bizNo) {
        IdempotentRecord record = new IdempotentRecord();
        record.setStatus(status);
        record.setBizNo(bizNo);
        return record;
    }
}
