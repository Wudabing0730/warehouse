package com.warehouse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.dto.request.InboundQueryDTO;
import com.warehouse.entity.InboundOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InboundOrderMapper extends BaseMapper<InboundOrder> {

    IPage<InboundOrder> selectPageVO(Page<InboundOrder> page, @Param("query") InboundQueryDTO query);
}
