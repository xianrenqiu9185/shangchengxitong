package com.linli.mall.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linli.mall.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
