package com.linli.mall.common.api;

import java.util.List;

/**
 * 统一分页结果。
 */
public record PageResult<T>(List<T> list, long total, long pageNum, long pageSize) {

    public static <T> PageResult<T> of(List<T> list, long total, long pageNum, long pageSize) {
        return new PageResult<>(list, total, pageNum, pageSize);
    }
}
