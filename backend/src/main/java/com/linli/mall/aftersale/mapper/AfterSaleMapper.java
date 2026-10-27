package com.linli.mall.aftersale.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linli.mall.aftersale.entity.AfterSale;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AfterSaleMapper extends BaseMapper<AfterSale> {

    @Update("""
            UPDATE t_after_sale
            SET status = 1, refund_no = #{refundNo}, audit_time = NOW(),
                remark = #{remark}, update_time = NOW()
            WHERE id = #{id} AND status = 0 AND deleted = 0
            """)
    int approve(@Param("id") Long id, @Param("refundNo") String refundNo, @Param("remark") String remark);

    @Update("""
            UPDATE t_after_sale
            SET status = 2, audit_time = NOW(), remark = #{remark}, update_time = NOW()
            WHERE id = #{id} AND status = 0 AND deleted = 0
            """)
    int reject(@Param("id") Long id, @Param("remark") String remark);

    @Update("""
            UPDATE t_after_sale
            SET status = 3, update_time = NOW()
            WHERE id = #{id} AND status IN (1, 3) AND deleted = 0
            """)
    int markRefunding(@Param("id") Long id);

    @Update("""
            UPDATE t_after_sale
            SET status = 4, update_time = NOW()
            WHERE id = #{id} AND status IN (1, 3) AND deleted = 0
            """)
    int complete(@Param("id") Long id);
}
