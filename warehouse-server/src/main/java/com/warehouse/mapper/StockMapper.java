package com.warehouse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.warehouse.entity.Stock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface StockMapper extends BaseMapper<Stock> {

    @Select("SELECT * FROM t_stock WHERE product_id = #{productId} AND location_id = #{locationId} FOR UPDATE")
    Stock selectForUpdate(@Param("productId") Long productId, @Param("locationId") Long locationId);
}
