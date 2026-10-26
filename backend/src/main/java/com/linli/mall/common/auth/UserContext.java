package com.linli.mall.common.auth;

/**
 * 当前登录用户上下文（由 AuthInterceptor 写入，请求结束后清理）。
 */
public final class UserContext {

    public record CurrentUser(Long userId, Integer userType, Long roleId) {
    }

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static Long userId() {
        CurrentUser user = HOLDER.get();
        return user == null ? null : user.userId();
    }

    public static Integer userType() {
        CurrentUser user = HOLDER.get();
        return user == null ? null : user.userType();
    }

    public static Long roleId() {
        CurrentUser user = HOLDER.get();
        return user == null ? null : user.roleId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
