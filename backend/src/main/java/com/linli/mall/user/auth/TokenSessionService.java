package com.linli.mall.user.auth;

import com.linli.mall.common.auth.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

/**
 * Redis 不透明 Token 会话。会话值固定为 userId:userType:roleId。
 */
@Service
@RequiredArgsConstructor
public class TokenSessionService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder TOKEN_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final StringRedisTemplate redisTemplate;

    public String createSession(Long userId, Integer userType, Long roleId) {
        byte[] random = new byte[32];
        SECURE_RANDOM.nextBytes(random);
        String token = TOKEN_ENCODER.encodeToString(random);
        String sessionValue = userId + ":" + userType + ":" + roleId;
        String sessionKey = AuthInterceptor.SESSION_PREFIX + token;
        String userSessionsKey = userSessionsKey(userId);

        redisTemplate.opsForValue().set(sessionKey, sessionValue, AuthInterceptor.SESSION_TTL);
        redisTemplate.opsForSet().add(userSessionsKey, token);
        redisTemplate.expire(userSessionsKey, AuthInterceptor.SESSION_TTL);
        return token;
    }

    public void revoke(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        String sessionKey = AuthInterceptor.SESSION_PREFIX + token;
        String sessionValue = redisTemplate.opsForValue().get(sessionKey);
        redisTemplate.delete(sessionKey);
        Long userId = parseUserId(sessionValue);
        if (userId != null) {
            redisTemplate.opsForSet().remove(userSessionsKey(userId), token);
        }
    }

    public void revokeAll(Long userId) {
        if (userId == null) {
            return;
        }
        String key = userSessionsKey(userId);
        Set<String> tokens = redisTemplate.opsForSet().members(key);
        if (tokens != null) {
            for (String token : tokens) {
                redisTemplate.delete(AuthInterceptor.SESSION_PREFIX + token);
            }
        }
        redisTemplate.delete(key);
    }

    private Long parseUserId(String sessionValue) {
        if (sessionValue == null || sessionValue.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(sessionValue.split(":")[0]);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String userSessionsKey(Long userId) {
        return AuthInterceptor.USER_SESSIONS_PREFIX + userId + AuthInterceptor.USER_SESSIONS_SUFFIX;
    }
}
