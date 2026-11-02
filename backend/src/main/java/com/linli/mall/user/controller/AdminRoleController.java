package com.linli.mall.user.controller;

import com.linli.mall.common.api.Result;
import com.linli.mall.common.auth.RequirePermission;
import com.linli.mall.user.dto.PermissionView;
import com.linli.mall.user.dto.RoleCreateRequest;
import com.linli.mall.user.dto.RolePermissionUpdateRequest;
import com.linli.mall.user.dto.RoleView;
import com.linli.mall.user.service.RoleAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminRoleController {

    private final RoleAdminService roleAdminService;

    @GetMapping("/roles")
    @RequirePermission("system:role:manage")
    public Result<List<RoleView>> roles() {
        return Result.ok(roleAdminService.listRoles());
    }

    @GetMapping("/permissions")
    @RequirePermission("system:role:manage")
    public Result<List<PermissionView>> permissions() {
        return Result.ok(roleAdminService.listPermissions());
    }

    @PostMapping("/roles")
    @RequirePermission("system:role:manage")
    public Result<RoleView> create(@Valid @RequestBody RoleCreateRequest request) {
        return Result.ok(roleAdminService.createRole(request));
    }

    @PutMapping("/roles/{id}/permissions")
    @RequirePermission("system:role:manage")
    public Result<RoleView> assignPermissions(@PathVariable Long id,
                                               @Valid @RequestBody RolePermissionUpdateRequest request) {
        return Result.ok(roleAdminService.assignPermissions(id, request.permissions()));
    }
}
