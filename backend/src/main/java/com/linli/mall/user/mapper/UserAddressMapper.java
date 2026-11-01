package com.linli.mall.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linli.mall.user.entity.UserAddress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserAddressMapper extends BaseMapper<UserAddress> {

    @Update("""
            UPDATE t_user_address
            SET is_default = 0,
                update_time = NOW()
            WHERE user_id = #{userId}
              AND deleted = 0
            """)
    int clearDefault(@Param("userId") Long userId);
}
