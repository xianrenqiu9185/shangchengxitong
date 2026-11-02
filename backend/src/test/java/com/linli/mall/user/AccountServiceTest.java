package com.linli.mall.user;

import com.linli.mall.common.api.ErrorCode;
import com.linli.mall.common.exception.BizException;
import com.linli.mall.user.auth.TokenSessionService;
import com.linli.mall.user.dto.AccountCreateRequest;
import com.linli.mall.user.dto.AccountView;
import com.linli.mall.user.entity.Role;
import com.linli.mall.user.entity.User;
import com.linli.mall.user.mapper.RoleMapper;
import com.linli.mall.user.mapper.UserMapper;
import com.linli.mall.user.service.impl.AccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock private UserMapper userMapper;
    @Mock private RoleMapper roleMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenSessionService tokenSessionService;

    private AccountServiceImpl accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountServiceImpl(userMapper, roleMapper, passwordEncoder, tokenSessionService);
    }

    @Test
    void createShouldHashPasswordAndPersistOperator() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(roleMapper.selectOne(any())).thenReturn(role(2L, "OPERATOR", "商家运营"));
        when(passwordEncoder.encode("abc12345")).thenReturn("bcrypt-hash");
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(21L);
            return 1;
        });

        AccountView view = accountService.create(
                new AccountCreateRequest("13812345678", "仓库运营", "operator", "abc12345"));

        assertThat(view.id()).isEqualTo(21L);
        assertThat(view.role()).isEqualTo("operator");
        assertThat(view.userType()).isEqualTo(2);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("bcrypt-hash");
        assertThat(captor.getValue().getRoleId()).isEqualTo(2L);
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
    }

    @Test
    void createShouldRejectRegisteredPhone() {
        when(userMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> accountService.create(
                new AccountCreateRequest("13812345678", "仓库运营", "operator", "abc12345")))
                .isInstanceOf(BizException.class)
                .extracting(error -> ((BizException) error).getErrorCode())
                .isEqualTo(ErrorCode.PHONE_REGISTERED);
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void cannotDisableCurrentLoginAccount() {
        when(userMapper.selectById(1L)).thenReturn(account(1L, 3, 3L, 1));

        assertThatThrownBy(() -> accountService.updateStatus(1L, 0, 1L))
                .isInstanceOf(BizException.class)
                .extracting(error -> ((BizException) error).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
        verify(userMapper, never()).updateById(any(User.class));
        verify(tokenSessionService, never()).revokeAll(any());
    }

    @Test
    void cannotDisableLastActiveAdmin() {
        when(userMapper.selectById(3L)).thenReturn(account(3L, 3, 3L, 1));
        when(userMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> accountService.updateStatus(3L, 0, 1L))
                .isInstanceOf(BizException.class)
                .extracting(error -> ((BizException) error).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void disablingOperatorShouldRevokeAllSessions() {
        User operator = account(2L, 2, 2L, 1);
        when(userMapper.selectById(2L)).thenReturn(operator);
        when(roleMapper.selectById(2L)).thenReturn(role(2L, "OPERATOR", "商家运营"));

        AccountView view = accountService.updateStatus(2L, 0, 1L);

        assertThat(view.status()).isZero();
        verify(userMapper).updateById(operator);
        verify(tokenSessionService).revokeAll(2L);
    }

    @Test
    void repeatedDisableOfDisabledAdminShouldBeIdempotent() {
        User admin = account(3L, 3, 3L, 0);
        when(userMapper.selectById(3L)).thenReturn(admin);
        when(roleMapper.selectById(3L)).thenReturn(role(3L, "ADMIN", "平台管理员"));

        AccountView view = accountService.updateStatus(3L, 0, 1L);

        assertThat(view.status()).isZero();
        verify(userMapper, never()).updateById(any(User.class));
        verify(userMapper, never()).selectCount(any());
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

    private User account(Long id, int userType, Long roleId, int status) {
        User user = new User();
        user.setId(id);
        user.setPhone("1380000000" + id);
        user.setNickname("账号" + id);
        user.setUserType(userType);
        user.setRoleId(roleId);
        user.setStatus(status);
        return user;
    }
}
