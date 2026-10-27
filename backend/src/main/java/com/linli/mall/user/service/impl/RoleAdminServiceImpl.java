package com.linli.mall.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linli.mall.common.api.ErrorCode;
import com.linli.mall.common.exception.BizException;
import com.linli.mall.user.auth.RolePermissionService;
import com.linli.mall.user.dto.PermissionView;
import com.linli.mall.user.dto.RoleCreateRequest;
import com.linli.mall.user.dto.RoleView;
import com.linli.mall.user.entity.Permission;
import com.linli.mall.user.entity.Role;
import com.linli.mall.user.entity.RolePermission;
import com.linli.mall.user.mapper.PermissionMapper;
import com.linli.mall.user.mapper.RoleMapper;
import com.linli.mall.user.mapper.RolePermissionMapper;
import com.linli.mall.user.service.RoleAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleAdminServiceImpl implements RoleAdminService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_OPERATOR = "OPERATOR";
    private static final String ROLE_MEMBER = "MEMBER";

    private static final Set<String> BUILT_IN_ROLE_CODES = Set.of(ROLE_MEMBER, ROLE_OPERATOR, ROLE_ADMIN);

    private static final Set<String> ADMIN_ONLY_PERMISSIONS = Set.of(
            "user:member:manage",
            "system:role:manage",
            "system:account:manage",
            "system:config:manage",
            "system:log:audit"
    );

    private static final Set<String> OPERATOR_ALLOWED_PERMISSIONS = Set.of(
            "product:category:manage",
            "product:goods:manage",
            "stock:info:view",
            "stock:info:adjust",
            "trade:order:query",
            "trade:order:ship",
            "trade:order:close",
            "after:sale:audit",
            "review:goods:reply",
            "marketing:coupon:manage",
            "statistics:dashboard:view"
    );

    private static final Set<String> MEMBER_ALLOWED_PERMISSIONS = Set.of(
            "user:profile:update",
            "address:info:manage",
            "product:goods:view",
            "cart:item:manage",
            "trade:order:create",
            "trade:payment:pay",
            "trade:order:query",
            "trade:order:cancel",
            "after:sale:apply",
            "review:goods:create",
            "marketing:coupon:receive"
    );

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final RolePermissionService rolePermissionService;

    @Override
    public List<RoleView> listRoles() {
        Map<String, Permission> permissionByCode = activePermissions().stream()
                .collect(Collectors.toMap(Permission::getCode, Function.identity()));
        return roleMapper.selectList(new LambdaQueryWrapper<Role>().orderByAsc(Role::getId))
                .stream()
                .map(role -> toView(role, permissionMapper.selectCodesByRoleId(role.getId()), permissionByCode))
                .toList();
    }

    @Override
    public List<PermissionView> listPermissions() {
        return activePermissions().stream()
                .map(permission -> new PermissionView(permission.getCode(), permission.getName(),
                        permission.getModule(), permission.getType(), permission.getSort()))
                .toList();
    }

    @Override
    @Transactional
    public RoleView createRole(RoleCreateRequest request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (BUILT_IN_ROLE_CODES.contains(code)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "系统内置角色编码不可重复创建");
        }
        Long existing = roleMapper.selectCount(new LambdaQueryWrapper<Role>().eq(Role::getCode, code));
        if (existing != null && existing > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "角色编码已存在");
        }

        List<Permission> activePermissions = activePermissions();
        Map<String, Permission> permissionByCode = activePermissions.stream()
                .collect(Collectors.toMap(Permission::getCode, Function.identity()));
        Set<String> requested = requestedCodes(request.permissions());
        validatePermissionCodes(requested, permissionByCode);

        Role role = new Role();
        role.setName(request.name().trim());
        role.setCode(code);
        role.setRemark(trimToNull(request.description()));
        role.setStatus(request.status() == null ? 1 : request.status());
        LocalDateTime now = LocalDateTime.now();
        role.setCreateTime(now);
        role.setUpdateTime(now);
        Set<String> assigned = resolveAllowedCodes(role, requested, activePermissions);
        try {
            roleMapper.insert(role);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.BAD_REQUEST, "角色编码已存在");
        }

        savePermissions(role.getId(), assigned, permissionByCode);
        evictAfterCommit(role.getId());
        return toView(role, List.copyOf(assigned), permissionByCode);
    }

    @Override
    @Transactional
    public RoleView assignPermissions(Long roleId, List<String> permissionCodes) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        List<Permission> activePermissions = activePermissions();
        Map<String, Permission> permissionByCode = activePermissions.stream()
                .collect(Collectors.toMap(Permission::getCode, Function.identity()));
        Set<String> requested = requestedCodes(permissionCodes);
        validatePermissionCodes(requested, permissionByCode);
        Set<String> assigned = resolveAllowedCodes(role, requested, activePermissions);

        rolePermissionMapper.deleteByRoleId(roleId);
        savePermissions(roleId, assigned, permissionByCode);
        evictAfterCommit(roleId);
        return toView(role, List.copyOf(assigned), permissionByCode);
    }

    private Set<String> requestedCodes(List<String> permissionCodes) {
        return permissionCodes == null ? Set.of()
                : permissionCodes.stream().map(String::trim)
                .filter(code -> !code.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void validatePermissionCodes(Set<String> requested, Map<String, Permission> permissionByCode) {
        for (String code : requested) {
            if (!permissionByCode.containsKey(code)) {
                throw new BizException(ErrorCode.BAD_REQUEST, "权限点不存在或已停用：" + code);
            }
        }
    }

    private void savePermissions(Long roleId, Set<String> assigned, Map<String, Permission> permissionByCode) {
        for (String code : assigned) {
            RolePermission relation = new RolePermission();
            relation.setRoleId(roleId);
            relation.setPermissionId(permissionByCode.get(code).getId());
            rolePermissionMapper.insert(relation);
        }
    }

    private Set<String> resolveAllowedCodes(Role role, Set<String> requested,
                                            List<Permission> activePermissions) {
        String roleCode = role.getCode() == null ? "" : role.getCode().toUpperCase(Locale.ROOT);
        if (ROLE_ADMIN.equals(roleCode)) {
            return activePermissions.stream().map(Permission::getCode)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        if (ROLE_OPERATOR.equals(roleCode)) {
            rejectAny(requested, difference(requested, OPERATOR_ALLOWED_PERMISSIONS),
                    "运营角色不能分配 C 端或系统管理权限");
            return requested;
        }
        if (ROLE_MEMBER.equals(roleCode)) {
            rejectAny(requested, difference(requested, MEMBER_ALLOWED_PERMISSIONS),
                    "会员角色不能分配后台权限");
            return requested;
        }
        rejectAny(requested, ADMIN_ONLY_PERMISSIONS, "自定义角色不能分配平台管理员专属权限");
        return requested;
    }

    private void rejectAny(Set<String> requested, Set<String> forbidden, String message) {
        Set<String> invalid = requested.stream().filter(forbidden::contains).findFirst().stream()
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!invalid.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, message + "：" + invalid.iterator().next());
        }
    }

    private Set<String> difference(Set<String> source, Set<String> allowed) {
        return source.stream().filter(code -> !allowed.contains(code))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private RoleView toView(Role role, List<String> permissions, Map<String, Permission> permissionByCode) {
        List<String> normalized = permissions.stream()
                .filter(permissionByCode::containsKey)
                .distinct()
                .sorted((left, right) -> comparePermissions(permissionByCode.get(left),
                        permissionByCode.get(right)))
                .toList();
        return new RoleView(role.getId(), role.getName(), role.getCode(), role.getRemark(),
                role.getStatus(), normalized.size(), normalized, role.getUpdateTime());
    }

    private int comparePermissions(Permission left, Permission right) {
        int module = nullSafe(left.getModule()).compareTo(nullSafe(right.getModule()));
        if (module != 0) {
            return module;
        }
        int sort = Integer.compare(nullSafe(left.getSort()), nullSafe(right.getSort()));
        if (sort != 0) {
            return sort;
        }
        return nullSafe(left.getCode()).compareTo(nullSafe(right.getCode()));
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }

    private List<Permission> activePermissions() {
        return permissionMapper.selectList(new LambdaQueryWrapper<Permission>()
                .eq(Permission::getStatus, 1)
                .orderByAsc(Permission::getModule)
                .orderByAsc(Permission::getSort)
                .orderByAsc(Permission::getId));
    }

    private void evictAfterCommit(Long roleId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    rolePermissionService.evict(roleId);
                }
            });
        } else {
            rolePermissionService.evict(roleId);
        }
    }
}
