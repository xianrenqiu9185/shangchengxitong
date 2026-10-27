package com.linli.mall.user;

import com.linli.mall.common.api.ErrorCode;
import com.linli.mall.common.exception.BizException;
import com.linli.mall.user.auth.RolePermissionService;
import com.linli.mall.user.dto.RoleCreateRequest;
import com.linli.mall.user.dto.RoleView;
import com.linli.mall.user.entity.Permission;
import com.linli.mall.user.entity.Role;
import com.linli.mall.user.entity.RolePermission;
import com.linli.mall.user.mapper.PermissionMapper;
import com.linli.mall.user.mapper.RoleMapper;
import com.linli.mall.user.mapper.RolePermissionMapper;
import com.linli.mall.user.service.impl.RoleAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleAdminServiceTest {

    @Mock private RoleMapper roleMapper;
    @Mock private PermissionMapper permissionMapper;
    @Mock private RolePermissionMapper rolePermissionMapper;
    @Mock private RolePermissionService rolePermissionService;

    private RoleAdminServiceImpl roleAdminService;

    @BeforeEach
    void setUp() {
        roleAdminService = new RoleAdminServiceImpl(roleMapper, permissionMapper,
                rolePermissionMapper, rolePermissionService);
    }

    @Test
    void operatorAssignmentShouldPersistAndEvictCache() {
        when(roleMapper.selectById(2L)).thenReturn(role(2L, "OPERATOR", "商家运营"));
        when(permissionMapper.selectList(any())).thenReturn(permissions());

        RoleView view = roleAdminService.assignPermissions(2L,
                List.of("product:goods:manage", "trade:order:ship"));

        assertThat(view.permissionCount()).isEqualTo(2);
        verify(rolePermissionMapper).deleteByRoleId(2L);
        ArgumentCaptor<RolePermission> captor = ArgumentCaptor.forClass(RolePermission.class);
        verify(rolePermissionMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertThat(captor.getAllValues()).extracting(RolePermission::getPermissionId)
                .containsExactly(1L, 2L);
        verify(rolePermissionService).evict(2L);
    }

    @Test
    void operatorShouldNotReceiveAdminOnlyPermission() {
        when(roleMapper.selectById(2L)).thenReturn(role(2L, "OPERATOR", "商家运营"));
        when(permissionMapper.selectList(any())).thenReturn(permissions());

        assertThatThrownBy(() -> roleAdminService.assignPermissions(2L,
                List.of("system:role:manage")))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
        verify(rolePermissionMapper, never()).deleteByRoleId(any());
    }

    @Test
    void operatorShouldNotReceiveMemberOnlyPermission() {
        when(roleMapper.selectById(2L)).thenReturn(role(2L, "OPERATOR", "商家运营"));
        when(permissionMapper.selectList(any())).thenReturn(permissions());

        assertThatThrownBy(() -> roleAdminService.assignPermissions(2L,
                List.of("review:goods:create")))
                .isInstanceOf(BizException.class)
                .extracting(error -> ((BizException) error).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
        verify(rolePermissionMapper, never()).deleteByRoleId(any());
    }

    @Test
    void memberRoleShouldNotReceiveBackendPermission() {
        when(roleMapper.selectById(1L)).thenReturn(role(1L, "MEMBER", "会员"));
        when(permissionMapper.selectList(any())).thenReturn(permissions());

        assertThatThrownBy(() -> roleAdminService.assignPermissions(1L,
                List.of("product:goods:manage")))
                .isInstanceOf(BizException.class)
                .extracting(error -> ((BizException) error).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
        verify(rolePermissionMapper, never()).deleteByRoleId(any());
    }

    @Test
    void adminRoleShouldAlwaysKeepAllPermissions() {
        when(roleMapper.selectById(3L)).thenReturn(role(3L, "ADMIN", "平台管理员"));
        when(permissionMapper.selectList(any())).thenReturn(permissions());

        RoleView view = roleAdminService.assignPermissions(3L, List.of());

        assertThat(view.permissions()).containsExactlyInAnyOrder(
                "product:goods:manage", "trade:order:ship", "system:role:manage",
                "review:goods:create");
        verify(rolePermissionService).evict(3L);
    }

    @Test
    void unknownPermissionShouldBeRejected() {
        when(roleMapper.selectById(2L)).thenReturn(role(2L, "OPERATOR", "商家运营"));
        when(permissionMapper.selectList(any())).thenReturn(permissions());

        assertThatThrownBy(() -> roleAdminService.assignPermissions(2L, List.of("unknown:code")))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    void createRoleShouldPersistNormalizedCodeAndPermissions() {
        when(roleMapper.selectCount(any())).thenReturn(0L);
        when(permissionMapper.selectList(any())).thenReturn(permissions());
        when(roleMapper.insert(any(Role.class))).thenAnswer(invocation -> {
            Role role = invocation.getArgument(0);
            role.setId(4L);
            return 1;
        });

        RoleView view = roleAdminService.createRole(new RoleCreateRequest(
                "仓储运营", "warehouse", "负责库存与履约", 1,
                List.of("product:goods:manage")));

        assertThat(view.id()).isEqualTo(4L);
        assertThat(view.code()).isEqualTo("WAREHOUSE");
        assertThat(view.permissions()).containsExactly("product:goods:manage");
        verify(rolePermissionMapper).insert(any(RolePermission.class));
        verify(rolePermissionService).evict(4L);
    }

    @Test
    void createRoleShouldRejectBuiltInCode() {
        assertThatThrownBy(() -> roleAdminService.createRole(new RoleCreateRequest(
                "管理员副本", "admin", null, 1, List.of())))
                .isInstanceOf(BizException.class)
                .extracting(error -> ((BizException) error).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
        verify(roleMapper, never()).insert(any(Role.class));
    }

    @Test
    void customRoleShouldNotReceiveAdminOnlyPermission() {
        when(roleMapper.selectCount(any())).thenReturn(0L);
        when(permissionMapper.selectList(any())).thenReturn(permissions());

        assertThatThrownBy(() -> roleAdminService.createRole(new RoleCreateRequest(
                "仓储运营", "warehouse", null, 1, List.of("system:role:manage"))))
                .isInstanceOf(BizException.class)
                .extracting(error -> ((BizException) error).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
        verify(roleMapper, never()).insert(any(Role.class));
    }

    private List<Permission> permissions() {
        return List.of(permission(1L, "product:goods:manage", "product", 10),
                permission(2L, "trade:order:ship", "trade", 20),
                permission(3L, "system:role:manage", "system", 30),
                permission(4L, "review:goods:create", "review", 40));
    }

    private Permission permission(Long id, String code, String module, int sort) {
        Permission permission = new Permission();
        permission.setId(id);
        permission.setCode(code);
        permission.setName(code);
        permission.setModule(module);
        permission.setSort(sort);
        permission.setStatus(1);
        return permission;
    }

    private Role role(Long id, String code, String name) {
        Role role = new Role();
        role.setId(id);
        role.setCode(code);
        role.setName(name);
        role.setStatus(1);
        return role;
    }
}
