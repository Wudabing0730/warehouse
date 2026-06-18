package com.warehouse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.dto.request.OperationLogQueryDTO;
import com.warehouse.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {

    @Select("SELECT * FROM t_operation_log WHERE " +
            "#{query.userId} IS NULL OR user_id = #{query.userId} " +
            "ORDER BY operate_time DESC")
    IPage<OperationLog> selectPage(Page<OperationLog> page, @Param("query") OperationLogQueryDTO query);
}
