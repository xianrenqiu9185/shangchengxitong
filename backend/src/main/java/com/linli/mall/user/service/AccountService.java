package com.linli.mall.user.service;

import com.linli.mall.common.api.PageResult;
import com.linli.mall.user.dto.AccountCreateRequest;
import com.linli.mall.user.dto.AccountView;

public interface AccountService {

    PageResult<AccountView> list(String keyword, long pageNum, long pageSize);

    AccountView create(AccountCreateRequest request);

    AccountView updateStatus(Long accountId, Integer status, Long operatorId);
}
