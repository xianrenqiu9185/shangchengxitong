package com.linli.mall.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linli.mall.common.api.ErrorCode;
import com.linli.mall.common.api.PageResult;
import com.linli.mall.common.exception.BizException;
import com.linli.mall.user.auth.TokenSessionService;
import com.linli.mall.user.dto.AccountCreateRequest;
import com.linli.mall.user.dto.AccountView;
import com.linli.mall.user.entity.Role;
import com.linli.mall.user.entity.User;
import com.linli.mall.user.mapper.RoleMapper;
import com.linli.mall.user.mapper.UserMapper;
import com.linli.mall.user.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private static final int MEMBER_USER_TYPE = 1;
    private static final int OPERATOR_USER_TYPE = 2;
    private static final int ADMIN_USER_TYPE = 3;

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenSessionService tokenSessionService;

    @Override
    public PageResult<AccountView> list(String keyword, long pageNum, long pageSize) {
        long safePageNum = Math.max(pageNum, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 100);
        String safeKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<User>()
                .in(User::getUserType, List.of(OPERATOR_USER_TYPE, ADMIN_USER_TYPE));
        if (safeKeyword != null) {
            query.and(wrapper -> wrapper.like(User::getPhone, safeKeyword)
                    .or().like(User::getNickname, safeKeyword));
        }
        query.orderByDesc(User::getCreateTime).orderByDesc(User::getId);
        Page<User> page = userMapper.selectPage(new Page<>(safePageNum, safePageSize), query);
        List<Long> roleIds = page.getRecords().stream().map(User::getRoleId).distinct().toList();
        Map<Long, Role> roles = roleIds.isEmpty() ? Map.of()
                : roleMapper.selectBatchIds(roleIds).stream()
                .collect(Collectors.toMap(Role::getId, Function.identity()));
        List<AccountView> views = page.getRecords().stream()
                .map(user -> toView(user, roles.get(user.getRoleId())))
                .toList();
        return PageResult.of(views, page.getTotal(), safePageNum, safePageSize);
    }

    @Override
    @Transactional
    public AccountView create(AccountCreateRequest request) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getPhone, request.phone()));
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.PHONE_REGISTERED);
        }
        String roleCode = request.role().trim().toUpperCase(Locale.ROOT);
        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getCode, roleCode)
                .eq(Role::getStatus, 1));
        if (role == null) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "后台角色未初始化");
        }
        User user = new User();
        user.setPhone(request.phone());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname().trim());
        user.setRoleId(role.getId());
        user.setUserType("ADMIN".equals(roleCode) ? ADMIN_USER_TYPE : OPERATOR_USER_TYPE);
        user.setStatus(1);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.PHONE_REGISTERED);
        }
        return toView(user, role);
    }

    @Override
    @Transactional
    public AccountView updateStatus(Long accountId, Integer status, Long operatorId) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "状态只能为 0 或 1");
        }
        User user = userMapper.selectById(accountId);
        if (user == null || Integer.valueOf(MEMBER_USER_TYPE).equals(user.getUserType())) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        if (status == 0 && accountId.equals(operatorId)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "不能禁用当前登录账号");
        }
        boolean disablingActiveAccount = status == 0 && Integer.valueOf(1).equals(user.getStatus());
        if (disablingActiveAccount && Integer.valueOf(ADMIN_USER_TYPE).equals(user.getUserType())) {
            Long activeAdmins = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getUserType, ADMIN_USER_TYPE)
                    .eq(User::getStatus, 1));
            if (activeAdmins != null && activeAdmins <= 1) {
                throw new BizException(ErrorCode.BAD_REQUEST, "至少保留一个启用的平台管理员");
            }
        }
        if (!status.equals(user.getStatus())) {
            user.setStatus(status);
            userMapper.updateById(user);
        }
        if (status == 0) {
            tokenSessionService.revokeAll(accountId);
        }
        return toView(user, roleMapper.selectById(user.getRoleId()));
    }

    private AccountView toView(User user, Role role) {
        String roleCode = role == null || role.getCode() == null
                ? null : role.getCode().toLowerCase(Locale.ROOT);
        return new AccountView(user.getId(), user.getPhone(), user.getNickname(), roleCode,
                user.getUserType(), user.getRoleId(), role == null ? null : role.getName(),
                user.getStatus(), null, user.getCreateTime());
    }
}
