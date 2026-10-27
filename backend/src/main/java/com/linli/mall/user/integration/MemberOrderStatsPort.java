package com.linli.mall.user.integration;

import java.util.Collection;
import java.util.Map;

/**
 * 会员管理读取交易聚合数据的单向 Port，由订单模块实现。
 */
public interface MemberOrderStatsPort {

    Map<Long, MemberOrderStats> findByUserIds(Collection<Long> userIds);
}
