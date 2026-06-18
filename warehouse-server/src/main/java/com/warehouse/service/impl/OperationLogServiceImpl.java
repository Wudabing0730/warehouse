package com.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.warehouse.dto.request.OperationLogQueryDTO;
import com.warehouse.entity.OperationLog;
import com.warehouse.mapper.OperationLogMapper;
import com.warehouse.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {

    @Override
    public IPage<OperationLog> page(Page<OperationLog> page, OperationLogQueryDTO query) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            wrapper.eq(query.getUserId() != null, OperationLog::getUserId, query.getUserId())
                   .like(StringUtils.hasText(query.getModule()), OperationLog::getModule, query.getModule())
                   .eq(query.getStatus() != null, OperationLog::getStatus, query.getStatus())
                   .ge(query.getStartTime() != null, OperationLog::getOperateTime, query.getStartTime())
                   .le(query.getEndTime() != null, OperationLog::getOperateTime, query.getEndTime());
        }
        wrapper.orderByDesc(OperationLog::getOperateTime);

        return baseMapper.selectPage(page, wrapper);
    }

    @Override
    public void saveAsync(OperationLog log) {
        save(log);
    }
}
