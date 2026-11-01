package com.linli.mall.common.auth;

import com.linli.mall.common.api.ErrorCode;
import com.linli.mall.common.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

/**
 * 统一鉴权拦截器（方案 B：随机 Token 存 Redis）。
 *
 * <p>会话值约定：userId:userType:roleId，由用户模块登录时写入
 * auth:session:{token}；本脚手架只做契约与登录校验，RBAC 校验由
 * PermissionChecker 实现补全。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    public static final String SESSION_PREFIX = "auth:session:";
    public static final String USER_SESSIONS_PREFIX = "auth:user:";
    public static final String USER_SESSIONS_SUFFIX = ":sessions";
    public static final Duration SESSION_TTL = Duration.ofHours(2);

    private final StringRedisTemplate redisTemplate;
    private final ObjectProvider<PermissionChecker> permissionCheckerProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        String token = authorization.substring(7).trim();
        String sessionKey = SESSION_PREFIX + token;
        String session = redisTemplate.opsForValue().get(sessionKey);
        if (session == null || session.isBlank()) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        try {
            String[] parts = session.split(":");
            if (parts.length != 3) {
                throw new NumberFormatException("session format");
            }
            Long userId = Long.valueOf(parts[0]);
            Integer userType = Integer.valueOf(parts[1]);
            Long roleId = Long.valueOf(parts[2]);
            UserContext.set(new UserContext.CurrentUser(userId, userType, roleId));

            // 滑动续期，同时续期“用户会话集合”，保证按用户批量踢下线始终有效。
            redisTemplate.expire(sessionKey, SESSION_TTL);
            redisTemplate.expire(USER_SESSIONS_PREFIX + userId + USER_SESSIONS_SUFFIX, SESSION_TTL);

            RequirePermission requirePermission = handlerMethod.getMethodAnnotation(RequirePermission.class);
            if (requirePermission != null) {
                PermissionChecker checker = permissionCheckerProvider.getIfAvailable();
                if (checker == null) {
                    log.warn("PermissionChecker 尚未实现，跳过权限点校验：{}", requirePermission.value());
                } else if (!checker.hasPermission(roleId, requirePermission.value())) {
                    throw new BizException(ErrorCode.FORBIDDEN);
                }
            }
            return true;
        } catch (BizException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn("Invalid session value, session={}", session);
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
