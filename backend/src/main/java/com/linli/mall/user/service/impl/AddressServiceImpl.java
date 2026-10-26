package com.linli.mall.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linli.mall.common.api.ErrorCode;
import com.linli.mall.common.exception.BizException;
import com.linli.mall.user.dto.AddressRequest;
import com.linli.mall.user.dto.AddressView;
import com.linli.mall.user.entity.UserAddress;
import com.linli.mall.user.mapper.UserAddressMapper;
import com.linli.mall.user.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final UserAddressMapper addressMapper;

    @Override
    public List<AddressView> list(Long userId) {
        return addressMapper.selectList(new LambdaQueryWrapper<UserAddress>()
                        .eq(UserAddress::getUserId, userId)
                        .orderByDesc(UserAddress::getIsDefault)
                        .orderByDesc(UserAddress::getUpdateTime))
                .stream()
                .map(this::toView)
                .toList();
    }

    @Override
    @Transactional
    public AddressView create(Long userId, AddressRequest request) {
        UserAddress address = new UserAddress();
        address.setUserId(userId);
        fill(address, request);
        if (Boolean.TRUE.equals(request.isDefault())) {
            addressMapper.clearDefault(userId);
            address.setIsDefault(1);
        } else {
            address.setIsDefault(0);
        }
        addressMapper.insert(address);
        return toView(address);
    }

    @Override
    @Transactional
    public AddressView update(Long userId, Long addressId, AddressRequest request) {
        UserAddress address = requireOwned(userId, addressId);
        fill(address, request);
        if (Boolean.TRUE.equals(request.isDefault())) {
            addressMapper.clearDefault(userId);
            address.setIsDefault(1);
        } else if (Boolean.FALSE.equals(request.isDefault())) {
            address.setIsDefault(0);
        }
        addressMapper.updateById(address);
        return toView(address);
    }

    @Override
    public void delete(Long userId, Long addressId) {
        requireOwned(userId, addressId);
        addressMapper.deleteById(addressId);
    }

    private UserAddress requireOwned(Long userId, Long addressId) {
        UserAddress address = addressMapper.selectOne(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getId, addressId)
                .eq(UserAddress::getUserId, userId));
        if (address == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return address;
    }

    private void fill(UserAddress address, AddressRequest request) {
        address.setReceiver(request.receiver().trim());
        address.setPhone(request.phone().trim());
        address.setDetail(buildDetail(request));
    }

    private String buildDetail(AddressRequest request) {
        String prefix = safe(request.province()) + safe(request.city()) + safe(request.district());
        return (prefix + request.detail().trim()).trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private AddressView toView(UserAddress address) {
        return new AddressView(address.getId(), address.getReceiver(), address.getPhone(),
                "", "", "", address.getDetail(), "", Integer.valueOf(1).equals(address.getIsDefault()));
    }
}
