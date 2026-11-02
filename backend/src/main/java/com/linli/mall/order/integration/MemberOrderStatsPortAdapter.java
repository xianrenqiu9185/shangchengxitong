package com.linli.mall.order.integration;

import com.linli.mall.order.mapper.OrderMapper;
import com.linli.mall.user.integration.MemberOrderStats;
import com.linli.mall.user.integration.MemberOrderStatsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberOrderStatsPortAdapter implements MemberOrderStatsPort {

    private final OrderMapper orderMapper;

    @Override
    public Map<Long, MemberOrderStats> findByUserIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return orderMapper.selectMemberStats(userIds).stream()
                .collect(Collectors.toMap(MemberOrderStatsRow::getUserId,
                        row -> new MemberOrderStats(value(row.getOrderCount()), value(row.getTotalSpent()))));
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }
}
