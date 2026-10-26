package com.linli.mall.common.idempotency;

public record IdempotencyClaim(String bizNo, boolean acquired, boolean completed) {
}
