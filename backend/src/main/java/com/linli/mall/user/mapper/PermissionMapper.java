package com.linli.mall.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linli.mall.user.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    @Select("""
            SELECT p.code
            FROM t_permission p
            JOIN t_role_permission rp ON rp.permission_id = p.id
            JOIN t_role r ON r.id = rp.role_id
            WHERE rp.role_id = #{roleId}
              AND p.status = 1
              AND p.deleted = 0
              AND r.status = 1
              AND r.deleted = 0
            ORDER BY p.code
            """)
    List<String> selectCodesByRoleId(@Param("roleId") Long roleId);
}
