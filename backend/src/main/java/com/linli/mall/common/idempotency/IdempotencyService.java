package com.linli.mall.common.idempotency;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linli.mall.common.api.ErrorCode;
import com.linli.mall.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotentMapper idempotentMapper;

    public IdempotencyClaim claim(Long userId, String bizType, String idemKey, String bizNo) {
        String key = normalizeKey(idemKey);
        if (idempotentMapper.insertIgnore(key, userId, bizType, bizNo) == 1) {
            return new IdempotencyClaim(bizNo, true, false);
        }
        IdempotentRecord record = idempotentMapper.selectOne(new LambdaQueryWrapper<IdempotentRecord>()
                .eq(IdempotentRecord::getUserId, userId)
                .eq(IdempotentRecord::getBizType, bizType)
                .eq(IdempotentRecord::getIdemKey, key));
        if (record != null && Integer.valueOf(1).equals(record.getStatus()) && record.getBizNo() != null) {
            return new IdempotencyClaim(record.getBizNo(), false, true);
        }
        throw new BizException(ErrorCode.DUPLICATE_REQUEST);
    }

    public void succeed(Long userId, String bizType, String idemKey, String bizNo) {
        idempotentMapper.markSuccess(userId, bizType, normalizeKey(idemKey), bizNo);
    }

    public void fail(Long userId, String bizType, String idemKey) {
        idempotentMapper.markFailed(userId, bizType, normalizeKey(idemKey));
    }

    private String normalizeKey(String idemKey) {
        if (idemKey == null || idemKey.isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "缺少 Idempotency-Key 请求头");
        }
        String value = idemKey.trim();
        if (value.length() > 64) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Idempotency-Key 不能超过 64 个字符");
        }
        return value;
    }
}
