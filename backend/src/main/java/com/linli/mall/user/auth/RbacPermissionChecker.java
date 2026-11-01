package com.linli.mall.user.auth;

import com.linli.mall.user.mapper.PermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * 权限点缓存 auth:perm:{roleId}。角色权限变更时由管理端调用 evict 立即失效。
 */
@Component
@RequiredArgsConstructor
public class RbacPermissionChecker implements RolePermissionService {

    private static final String PERMISSION_PREFIX = "auth:perm:";
    private static final String EMPTY_MARK = "__NONE__";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;
    private final PermissionMapper permissionMapper;

    @Override
    public boolean hasPermission(Long roleId, String permissionCode) {
        return permissionCode != null && getPermissions(roleId).contains(permissionCode);
    }

    @Override
    public List<String> getPermissions(Long roleId) {
        if (roleId == null) {
            return List.of();
        }
        String key = PERMISSION_PREFIX + roleId;
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return decode(cached);
        }
        List<String> permissions = List.copyOf(permissionMapper.selectCodesByRoleId(roleId));
        redisTemplate.opsForValue().set(key, permissions.isEmpty() ? EMPTY_MARK : String.join(",", permissions), TTL);
        return permissions;
    }

    @Override
    public void evict(Long roleId) {
        if (roleId != null) {
            redisTemplate.delete(PERMISSION_PREFIX + roleId);
        }
    }

    private List<String> decode(String cached) {
        if (cached.isBlank() || EMPTY_MARK.equals(cached)) {
            return List.of();
        }
        return Arrays.asList(cached.split(","));
    }
}
