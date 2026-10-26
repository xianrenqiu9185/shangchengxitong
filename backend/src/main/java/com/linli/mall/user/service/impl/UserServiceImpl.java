package com.linli.mall.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linli.mall.common.api.ErrorCode;
import com.linli.mall.common.exception.BizException;
import com.linli.mall.user.auth.LoginGuardService;
import com.linli.mall.user.auth.RolePermissionService;
import com.linli.mall.user.auth.TokenSessionService;
import com.linli.mall.user.dto.AuthResponse;
import com.linli.mall.user.dto.LoginRequest;
import com.linli.mall.user.dto.RegisterRequest;
import com.linli.mall.user.dto.UpdateProfileRequest;
import com.linli.mall.user.dto.UserProfileResponse;
import com.linli.mall.user.dto.UserView;
import com.linli.mall.user.entity.Role;
import com.linli.mall.user.entity.User;
import com.linli.mall.user.mapper.RoleMapper;
import com.linli.mall.user.mapper.UserMapper;
import com.linli.mall.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenSessionService tokenSessionService;
    private final LoginGuardService loginGuardService;
    private final RolePermissionService rolePermissionService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getPhone, request.phone()));
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.PHONE_REGISTERED);
        }
        Role memberRole = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getCode, "MEMBER")
                .eq(Role::getStatus, 1));
        if (memberRole == null) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "会员角色未初始化");
        }

        User user = new User();
        user.setPhone(request.phone());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setNickname(normalizeNickname(request.nickname(), request.phone()));
        user.setUserType(1);
        user.setRoleId(memberRole.getId());
        user.setStatus(1);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.PHONE_REGISTERED);
        }
        return issueSession(user, memberRole);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        loginGuardService.assertAllowed(request.phone());
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, request.phone()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            loginGuardService.recordFailure(request.phone());
            throw new BizException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            tokenSessionService.revokeAll(user.getId());
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }
        loginGuardService.clear(request.phone());
        return issueSession(user, findRole(user.getRoleId()));
    }

    @Override
    public void logout(String token) {
        tokenSessionService.revoke(token);
    }

    @Override
    public UserProfileResponse getProfile(Long userId) {
        User user = requireActiveUser(userId);
        return new UserProfileResponse(toView(user, findRole(user.getRoleId())),
                rolePermissionService.getPermissions(user.getRoleId()));
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = requireActiveUser(userId);
        user.setNickname(request.nickname().trim());
        userMapper.updateById(user);
        return new UserProfileResponse(toView(user, findRole(user.getRoleId())),
                rolePermissionService.getPermissions(user.getRoleId()));
    }

    private AuthResponse issueSession(User user, Role role) {
        String token = tokenSessionService.createSession(user.getId(), user.getUserType(), user.getRoleId());
        List<String> permissions = rolePermissionService.getPermissions(user.getRoleId());
        return new AuthResponse(token, toView(user, role), permissions);
    }

    private User requireActiveUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            tokenSessionService.revokeAll(userId);
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }
        return user;
    }

    private Role findRole(Long roleId) {
        return roleId == null ? null : roleMapper.selectById(roleId);
    }

    private UserView toView(User user, Role role) {
        String roleCode = role == null ? null : role.getCode().toLowerCase(Locale.ROOT);
        String roleName = role == null ? null : role.getName();
        return new UserView(user.getId(), user.getPhone(), user.getNickname(), user.getUserType(),
                user.getRoleId(), roleCode, roleName, user.getStatus());
    }

    private String normalizeNickname(String nickname, String phone) {
        if (nickname == null || nickname.isBlank()) {
            return "邻里会员" + phone.substring(phone.length() - 4);
        }
        return nickname.trim();
    }
}
