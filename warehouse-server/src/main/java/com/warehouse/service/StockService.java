package com.warehouse.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.warehouse.dto.request.StockQueryDTO;
import com.warehouse.dto.response.StockVO;
import com.warehouse.entity.Stock;

import java.math.BigDecimal;
import java.util.List;

public interface StockService extends IService<Stock> {

    IPage<StockVO> page(Page<Stock> page, StockQueryDTO query);

    void init(Long productId, Long locationId, BigDecimal quantity);

    List<StockVO> getAlerts();
}
