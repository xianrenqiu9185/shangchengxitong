package com.linli.mall.user.service;

import com.linli.mall.common.api.PageResult;
import com.linli.mall.user.dto.MemberView;

public interface MemberService {

    PageResult<MemberView> list(String keyword, Integer status, long pageNum, long pageSize);

    MemberView updateStatus(Long memberId, Integer status);
}
