package com.linli.mall.user.controller;

import com.linli.mall.common.api.Result;
import com.linli.mall.common.auth.RequirePermission;
import com.linli.mall.common.auth.UserContext;
import com.linli.mall.user.dto.AddressRequest;
import com.linli.mall.user.dto.AddressView;
import com.linli.mall.user.dto.UpdateProfileRequest;
import com.linli.mall.user.dto.UserProfileResponse;
import com.linli.mall.user.service.AddressService;
import com.linli.mall.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AddressService addressService;

    @GetMapping
    public Result<UserProfileResponse> me() {
        return Result.ok(userService.getProfile(UserContext.userId()));
    }

    @PutMapping
    @RequirePermission("user:profile:update")
    public Result<UserProfileResponse> update(@Valid @RequestBody UpdateProfileRequest request) {
        return Result.ok(userService.updateProfile(UserContext.userId(), request));
    }

    @GetMapping("/addresses")
    @RequirePermission("address:info:manage")
    public Result<List<AddressView>> addresses() {
        return Result.ok(addressService.list(UserContext.userId()));
    }

    @PostMapping("/addresses")
    @RequirePermission("address:info:manage")
    public Result<AddressView> createAddress(@Valid @RequestBody AddressRequest request) {
        return Result.ok(addressService.create(UserContext.userId(), request));
    }

    @PutMapping("/addresses/{id}")
    @RequirePermission("address:info:manage")
    public Result<AddressView> updateAddress(@PathVariable Long id,
                                             @Valid @RequestBody AddressRequest request) {
        return Result.ok(addressService.update(UserContext.userId(), id, request));
    }

    @DeleteMapping("/addresses/{id}")
    @RequirePermission("address:info:manage")
    public Result<Void> deleteAddress(@PathVariable Long id) {
        addressService.delete(UserContext.userId(), id);
        return Result.ok();
    }
}
