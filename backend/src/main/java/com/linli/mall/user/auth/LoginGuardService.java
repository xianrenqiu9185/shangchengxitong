package com.linli.mall.user.auth;

import com.linli.mall.common.api.ErrorCode;
import com.linli.mall.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 连续 5 次密码错误后锁定 15 分钟。计数只按手机号保存，不记录密码或 Token。
 */
@Service
@RequiredArgsConstructor
public class LoginGuardService {

    private static final String FAIL_PREFIX = "auth:login:fail:";
    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK_TTL = Duration.ofMinutes(15);

    private final StringRedisTemplate redisTemplate;

    public void assertAllowed(String phone) {
        String value = redisTemplate.opsForValue().get(key(phone));
        if (value != null && Integer.parseInt(value) >= MAX_FAILURES) {
            throw new BizException(ErrorCode.LOGIN_LOCKED);
        }
    }

    public void recordFailure(String phone) {
        String key = key(phone);
        Long count = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, LOCK_TTL);
        if (count != null && count >= MAX_FAILURES) {
            throw new BizException(ErrorCode.LOGIN_LOCKED);
        }
        throw new BizException(ErrorCode.INVALID_CREDENTIALS);
    }

    public void clear(String phone) {
        redisTemplate.delete(key(phone));
    }

    private String key(String phone) {
        return FAIL_PREFIX + phone;
    }
}
