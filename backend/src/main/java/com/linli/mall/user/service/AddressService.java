package com.linli.mall.user.service;

import com.linli.mall.user.dto.AddressRequest;
import com.linli.mall.user.dto.AddressView;

import java.util.List;

public interface AddressService {

    List<AddressView> list(Long userId);

    AddressView create(Long userId, AddressRequest request);

    AddressView update(Long userId, Long addressId, AddressRequest request);

    void delete(Long userId, Long addressId);
}
