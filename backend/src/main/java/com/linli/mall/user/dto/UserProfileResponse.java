package com.linli.mall.user.dto;

import java.util.List;

public record UserProfileResponse(UserView user, List<String> permissions) {
}
