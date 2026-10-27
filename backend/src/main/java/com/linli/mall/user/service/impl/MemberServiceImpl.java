package com.linli.mall.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linli.mall.common.api.ErrorCode;
import com.linli.mall.common.api.PageResult;
import com.linli.mall.common.exception.BizException;
import com.linli.mall.user.auth.TokenSessionService;
import com.linli.mall.user.dto.MemberView;
import com.linli.mall.user.entity.User;
import com.linli.mall.user.integration.MemberOrderStats;
import com.linli.mall.user.integration.MemberOrderStatsPort;
import com.linli.mall.user.mapper.UserMapper;
import com.linli.mall.user.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private static final int MEMBER_USER_TYPE = 1;
    private static final long GOLD_LEVEL_SPENT = 500_000L;
    private static final long LEAF_LEVEL_SPENT = 100_000L;

    private final UserMapper userMapper;
    private final TokenSessionService tokenSessionService;
    private final MemberOrderStatsPort memberOrderStatsPort;

    @Override
    public PageResult<MemberView> list(String keyword, Integer status, long pageNum, long pageSize) {
        long safePageNum = Math.max(pageNum, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 100);
        String safeKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<User>()
                .eq(User::getUserType, MEMBER_USER_TYPE)
                .eq(status != null, User::getStatus, status);
        if (safeKeyword != null) {
            query.and(wrapper -> wrapper.like(User::getPhone, safeKeyword)
                    .or().like(User::getNickname, safeKeyword));
        }
        query.orderByDesc(User::getCreateTime).orderByDesc(User::getId);
        Page<User> page = userMapper.selectPage(new Page<>(safePageNum, safePageSize), query);
        List<Long> userIds = page.getRecords().stream().map(User::getId).toList();
        Map<Long, MemberOrderStats> stats = memberOrderStatsPort.findByUserIds(userIds);
        List<MemberView> views = page.getRecords().stream()
                .map(user -> toView(user, stats.getOrDefault(user.getId(), MemberOrderStats.empty())))
                .toList();
        return PageResult.of(views, page.getTotal(), safePageNum, safePageSize);
    }

    @Override
    @Transactional
    public MemberView updateStatus(Long memberId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "状态只能为 0 或 1");
        }
        User user = userMapper.selectById(memberId);
        if (user == null || !Integer.valueOf(MEMBER_USER_TYPE).equals(user.getUserType())) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        if (!status.equals(user.getStatus())) {
            user.setStatus(status);
            userMapper.updateById(user);
        }
        if (status == 0) {
            tokenSessionService.revokeAll(memberId);
        }
        MemberOrderStats stats = memberOrderStatsPort.findByUserIds(List.of(memberId))
                .getOrDefault(memberId, MemberOrderStats.empty());
        return toView(user, stats);
    }

    private MemberView toView(User user, MemberOrderStats stats) {
        return new MemberView(user.getId(), user.getPhone(), user.getNickname(),
                level(stats.totalSpent()), user.getStatus(), stats.orderCount(),
                stats.totalSpent(), user.getCreateTime());
    }

    private String level(long totalSpent) {
        if (totalSpent >= GOLD_LEVEL_SPENT) {
            return "金穗会员";
        }
        if (totalSpent >= LEAF_LEVEL_SPENT) {
            return "鲜叶会员";
        }
        return "青芽会员";
    }
}
