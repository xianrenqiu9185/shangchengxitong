package com.linli.mall.common.idempotency;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface IdempotentMapper extends BaseMapper<IdempotentRecord> {

    @Insert("""
            INSERT IGNORE INTO t_idempotent
                (idem_key, user_id, biz_type, biz_no, status, expire_time)
            VALUES
                (#{idemKey}, #{userId}, #{bizType}, #{bizNo}, 0, DATE_ADD(NOW(), INTERVAL 24 HOUR))
            """)
    int insertIgnore(@Param("idemKey") String idemKey,
                     @Param("userId") Long userId,
                     @Param("bizType") String bizType,
                     @Param("bizNo") String bizNo);

    @Update("""
            UPDATE t_idempotent
            SET status = 1, biz_no = #{bizNo}, update_time = NOW()
            WHERE user_id = #{userId} AND biz_type = #{bizType} AND idem_key = #{idemKey}
            """)
    int markSuccess(@Param("userId") Long userId,
                    @Param("bizType") String bizType,
                    @Param("idemKey") String idemKey,
                    @Param("bizNo") String bizNo);

    @Update("""
            UPDATE t_idempotent
            SET status = 2, update_time = NOW()
            WHERE user_id = #{userId} AND biz_type = #{bizType} AND idem_key = #{idemKey} AND status = 0
            """)
    int markFailed(@Param("userId") Long userId,
                   @Param("bizType") String bizType,
                   @Param("idemKey") String idemKey);
}
