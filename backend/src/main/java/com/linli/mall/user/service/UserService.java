package com.linli.mall.user.service;

import com.linli.mall.user.dto.AuthResponse;
import com.linli.mall.user.dto.LoginRequest;
import com.linli.mall.user.dto.RegisterRequest;
import com.linli.mall.user.dto.UpdateProfileRequest;
import com.linli.mall.user.dto.UserProfileResponse;

public interface UserService {

    Long register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    void logout(String token);

    UserProfileResponse getProfile(Long userId);

    UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request);
}
