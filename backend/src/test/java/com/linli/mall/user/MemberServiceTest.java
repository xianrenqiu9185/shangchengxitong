package com.linli.mall.user;

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
import com.linli.mall.user.service.impl.MemberServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock private UserMapper userMapper;
    @Mock private TokenSessionService tokenSessionService;
    @Mock private MemberOrderStatsPort memberOrderStatsPort;

    private MemberServiceImpl memberService;

    @BeforeEach
    void setUp() {
        memberService = new MemberServiceImpl(userMapper, tokenSessionService, memberOrderStatsPort);
    }

    @Test
    void listShouldExposeOrderAggregationForMembersOnly() {
        User member = member(3L, 1, 1);
        Page<User> page = new Page<>(1, 10);
        page.setRecords(List.of(member));
        page.setTotal(1);
        when(userMapper.selectPage(any(), any())).thenReturn(page);
        when(memberOrderStatsPort.findByUserIds(List.of(3L)))
                .thenReturn(Map.of(3L, new MemberOrderStats(8, 126_800L)));

        PageResult<MemberView> result = memberService.list("小邻", null, 1, 10);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.list()).singleElement().satisfies(view -> {
            assertThat(view.phone()).isEqualTo("13800000003");
            assertThat(view.orderCount()).isEqualTo(8);
            assertThat(view.totalSpent()).isEqualTo(126_800L);
            assertThat(view.level()).isEqualTo("鲜叶会员");
        });
    }

    @Test
    void disablingMemberShouldRevokeAllSessions() {
        User member = member(3L, 1, 1);
        when(userMapper.selectById(3L)).thenReturn(member);
        when(memberOrderStatsPort.findByUserIds(List.of(3L))).thenReturn(Map.of());

        MemberView view = memberService.updateStatus(3L, 0);

        assertThat(view.status()).isZero();
        verify(userMapper).updateById(member);
        verify(tokenSessionService).revokeAll(3L);
    }

    @Test
    void enablingMemberShouldNotTouchExistingSessions() {
        User member = member(3L, 1, 0);
        when(userMapper.selectById(3L)).thenReturn(member);
        when(memberOrderStatsPort.findByUserIds(List.of(3L))).thenReturn(Map.of());

        MemberView view = memberService.updateStatus(3L, 1);

        assertThat(view.status()).isEqualTo(1);
        verify(tokenSessionService, never()).revokeAll(any());
    }

    @Test
    void nonMemberAccountCannotBeManagedAsMember() {
        when(userMapper.selectById(9L)).thenReturn(member(9L, 2, 1));

        assertThatThrownBy(() -> memberService.updateStatus(9L, 0))
                .isInstanceOf(BizException.class)
                .extracting(error -> ((BizException) error).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
        verify(userMapper, never()).updateById(any(User.class));
        verify(tokenSessionService, never()).revokeAll(any());
    }

    private User member(Long id, int userType, int status) {
        User user = new User();
        user.setId(id);
        user.setPhone("13800000003");
        user.setNickname("小邻");
        user.setUserType(userType);
        user.setRoleId(1L);
        user.setStatus(status);
        user.setCreateTime(LocalDateTime.of(2026, 9, 1, 10, 0));
        return user;
    }
}
