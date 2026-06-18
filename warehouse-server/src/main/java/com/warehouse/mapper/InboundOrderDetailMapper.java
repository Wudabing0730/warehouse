package com.warehouse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.warehouse.entity.InboundOrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InboundOrderDetailMapper extends BaseMapper<InboundOrderDetail> {

    @Select("SELECT * FROM t_inbound_order_detail WHERE order_id = #{orderId}")
    List<InboundOrderDetail> selectByOrderId(@Param("orderId") Long orderId);
}
