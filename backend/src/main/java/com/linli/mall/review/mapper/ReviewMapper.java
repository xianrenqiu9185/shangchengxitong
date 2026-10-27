package com.linli.mall.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linli.mall.review.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ReviewMapper extends BaseMapper<Review> {

    @Update("""
            UPDATE t_review
            SET reply = #{reply}, reply_time = NOW(), update_time = NOW()
            WHERE id = #{id} AND deleted = 0
            """)
    int reply(@Param("id") Long id, @Param("reply") String reply);
}
