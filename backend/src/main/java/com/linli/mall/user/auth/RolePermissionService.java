package com.linli.mall.user.auth;

import com.linli.mall.common.auth.PermissionChecker;

import java.util.List;

public interface RolePermissionService extends PermissionChecker {

    List<String> getPermissions(Long roleId);

    void evict(Long roleId);
}
