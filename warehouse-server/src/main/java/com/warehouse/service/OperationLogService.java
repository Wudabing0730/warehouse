package com.warehouse.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.warehouse.dto.request.OperationLogQueryDTO;
import com.warehouse.entity.OperationLog;

public interface OperationLogService extends IService<OperationLog> {

    IPage<OperationLog> page(Page<OperationLog> page, OperationLogQueryDTO query);

    void saveAsync(OperationLog log);
}
