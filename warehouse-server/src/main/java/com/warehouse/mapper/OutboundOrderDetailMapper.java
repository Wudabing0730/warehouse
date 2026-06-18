package com.warehouse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.warehouse.entity.OutboundOrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OutboundOrderDetailMapper extends BaseMapper<OutboundOrderDetail> {

    @Select("SELECT * FROM t_outbound_order_detail WHERE order_id = #{orderId}")
    List<OutboundOrderDetail> selectByOrderId(@Param("orderId") Long orderId);
}
