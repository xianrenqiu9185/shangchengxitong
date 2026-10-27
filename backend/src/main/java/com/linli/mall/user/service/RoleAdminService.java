package com.linli.mall.user.service;

import com.linli.mall.user.dto.PermissionView;
import com.linli.mall.user.dto.RoleCreateRequest;
import com.linli.mall.user.dto.RoleView;

import java.util.List;

public interface RoleAdminService {

    List<RoleView> listRoles();

    List<PermissionView> listPermissions();

    RoleView createRole(RoleCreateRequest request);

    RoleView assignPermissions(Long roleId, List<String> permissionCodes);
}
