package com.linli.mall.order.integration;

import lombok.Data;

@Data
public class MemberOrderStatsRow {

    private Long userId;
    private Long orderCount;
    private Long totalSpent;
}
