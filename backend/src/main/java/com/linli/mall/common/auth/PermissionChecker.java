package com.linli.mall.common.auth;

/**
 * 权限点校验契约。
 *
 * <p>由用户/RBAC 模块提供实现（例如基于 t_role_permission 与缓存），
 * AuthInterceptor 通过 ObjectProvider 可选注入；未提供实现时仅做登录校验。</p>
 */
public interface PermissionChecker {

    boolean hasPermission(Long roleId, String permissionCode);
}
