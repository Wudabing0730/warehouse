package com.warehouse.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.warehouse.dto.request.LocationCreateDTO;
import com.warehouse.dto.request.LocationUpdateDTO;
import com.warehouse.dto.response.LocationVO;
import com.warehouse.entity.WarehouseLocation;

import java.util.List;

public interface WarehouseLocationService extends IService<WarehouseLocation> {

    IPage<LocationVO> page(Page<WarehouseLocation> page);

    LocationVO getById(Long locationId);

    LocationVO create(LocationCreateDTO dto);

    LocationVO update(Long locationId, LocationUpdateDTO dto);

    void delete(Long locationId);

    List<WarehouseLocation> listEnabled();
}
