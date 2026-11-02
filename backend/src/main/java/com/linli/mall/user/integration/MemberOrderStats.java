package com.linli.mall.user.integration;

public record MemberOrderStats(long orderCount, long totalSpent) {

    public static MemberOrderStats empty() {
        return new MemberOrderStats(0, 0);
    }
}
