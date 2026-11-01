package com.linli.mall.user.dto;

import java.util.List;

public record AuthResponse(String token, UserView user, List<String> permissions) {
}
