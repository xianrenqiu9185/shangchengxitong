package com.linli.mall.aftersale.service;

import com.linli.mall.aftersale.dto.AfterSaleAuditRequest;
import com.linli.mall.aftersale.dto.AfterSaleCreateRequest;
import com.linli.mall.aftersale.dto.AfterSaleView;
import com.linli.mall.common.api.PageResult;

public interface AfterSaleService {

    AfterSaleView apply(Long userId, String idempotencyKey, AfterSaleCreateRequest request);

    PageResult<AfterSaleView> listUser(Long userId, String status, long pageNum, long pageSize);

    AfterSaleView getUser(Long userId, String saleNo);

    PageResult<AfterSaleView> listAdmin(String status, long pageNum, long pageSize);

    AfterSaleView audit(Long operatorId, String saleNo, String idempotencyKey, AfterSaleAuditRequest request);
}
