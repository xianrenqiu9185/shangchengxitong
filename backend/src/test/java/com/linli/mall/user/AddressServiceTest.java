package com.linli.mall.user;

import com.linli.mall.common.api.ErrorCode;
import com.linli.mall.common.exception.BizException;
import com.linli.mall.user.dto.AddressRequest;
import com.linli.mall.user.entity.UserAddress;
import com.linli.mall.user.mapper.UserAddressMapper;
import com.linli.mall.user.service.impl.AddressServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock private UserAddressMapper addressMapper;
    @InjectMocks private AddressServiceImpl addressService;

    @Test
    void defaultAddressShouldClearPreviousDefault() {
        when(addressMapper.insert(any(UserAddress.class))).thenAnswer(invocation -> {
            UserAddress address = invocation.getArgument(0);
            address.setId(1L);
            return 1;
        });

        addressService.create(3L, new AddressRequest("林女士", "13800000003", "上海市", "上海市",
                "浦东新区", "邻里社区 3 号楼", "家", true));

        verify(addressMapper).clearDefault(3L);
        ArgumentCaptor<UserAddress> captor = ArgumentCaptor.forClass(UserAddress.class);
        verify(addressMapper).insert(captor.capture());
        assertThat(captor.getValue().getIsDefault()).isEqualTo(1);
        assertThat(captor.getValue().getDetail()).isEqualTo("上海市上海市浦东新区邻里社区 3 号楼");
    }

    @Test
    void cannotUpdateAnotherUsersAddress() {
        when(addressMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> addressService.update(3L, 9L,
                new AddressRequest("林女士", "13800000003", null, null, null, "地址", null, false)))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }
}
