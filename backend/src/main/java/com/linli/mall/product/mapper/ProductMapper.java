package com.linli.mall.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linli.mall.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    @Update("""
            UPDATE t_product
            SET status = #{status},
                update_time = NOW()
            WHERE id = #{id}
              AND deleted = 0
            """)
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
