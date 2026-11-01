package com.linli.mall.user;

import com.linli.mall.common.api.ErrorCode;
import com.linli.mall.common.exception.BizException;
import com.linli.mall.user.auth.LoginGuardService;
import com.linli.mall.user.auth.RolePermissionService;
import com.linli.mall.user.auth.TokenSessionService;
import com.linli.mall.user.dto.AuthResponse;
import com.linli.mall.user.dto.LoginRequest;
import com.linli.mall.user.dto.RegisterRequest;
import com.linli.mall.user.entity.Role;
import com.linli.mall.user.entity.User;
import com.linli.mall.user.mapper.RoleMapper;
import com.linli.mall.user.mapper.UserMapper;
import com.linli.mall.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserMapper userMapper;
    @Mock private RoleMapper roleMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenSessionService tokenSessionService;
    @Mock private LoginGuardService loginGuardService;
    @Mock private RolePermissionService rolePermissionService;

    @InjectMocks private UserServiceImpl userService;

    @Test
    void registerShouldCreateMemberWithoutSession() {
        Role role = role(1L, "MEMBER", "会员");
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(roleMapper.selectOne(any())).thenReturn(role);
        when(passwordEncoder.encode("abcd1234")).thenReturn("hashed");
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            return 1;
        });

        Long userId = userService.register(new RegisterRequest("13812345678", "abcd1234", "小邻"));

        assertThat(userId).isEqualTo(10L);
        verify(tokenSessionService, never()).createSession(any(), any(), any());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("hashed");
        assertThat(captor.getValue().getRoleId()).isEqualTo(1L);
    }

    @Test
    void registerShouldRejectDuplicatePhone() {
        when(userMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> userService.register(
                new RegisterRequest("13812345678", "abcd1234", "小邻")))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.PHONE_REGISTERED);
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void loginShouldIssueSessionForValidPassword() {
        User user = user(3L, 1, 1L, 1, "hash");
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("123456", "hash")).thenReturn(true);
        when(roleMapper.selectById(1L)).thenReturn(role(1L, "MEMBER", "会员"));
        when(tokenSessionService.createSession(3L, 1, 1L)).thenReturn("token-3");
        when(rolePermissionService.getPermissions(1L)).thenReturn(List.of("cart:item:manage"));

        AuthResponse response = userService.login(new LoginRequest("13800000003", "123456"));

        assertThat(response.token()).isEqualTo("token-3");
        assertThat(response.user().nickname()).isEqualTo("会员3");
        verify(loginGuardService).clear("13800000003");
    }

    @Test
    void loginShouldCountFailureForWrongPassword() {
        when(userMapper.selectOne(any())).thenReturn(user(3L, 1, 1L, 1, "hash"));
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);
        doThrow(new BizException(ErrorCode.INVALID_CREDENTIALS))
                .when(loginGuardService).recordFailure("13800000003");

        assertThatThrownBy(() -> userService.login(new LoginRequest("13800000003", "bad")))
                .isInstanceOf(BizException.class);
        verify(tokenSessionService, never()).createSession(any(), any(), any());
    }

    @Test
    void disabledUserShouldLoseAllSessions() {
        when(userMapper.selectOne(any())).thenReturn(user(3L, 1, 1L, 0, "hash"));
        when(passwordEncoder.matches("123456", "hash")).thenReturn(true);

        assertThatThrownBy(() -> userService.login(new LoginRequest("13800000003", "123456")))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_DISABLED);
        verify(tokenSessionService).revokeAll(3L);
    }

    private Role role(Long id, String code, String name) {
        Role role = new Role();
        role.setId(id);
        role.setCode(code);
        role.setName(name);
        role.setStatus(1);
        return role;
    }

    private User user(Long id, int userType, Long roleId, int status, String password) {
        User user = new User();
        user.setId(id);
        user.setPhone("13800000003");
        user.setNickname("会员3");
        user.setUserType(userType);
        user.setRoleId(roleId);
        user.setStatus(status);
        user.setPassword(password);
        return user;
    }
}
