package com.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.warehouse.common.BusinessException;
import com.warehouse.dto.request.LocationCreateDTO;
import com.warehouse.dto.request.LocationUpdateDTO;
import com.warehouse.dto.response.LocationVO;
import com.warehouse.entity.Product;
import com.warehouse.entity.WarehouseLocation;
import com.warehouse.mapper.ProductMapper;
import com.warehouse.mapper.WarehouseLocationMapper;
import com.warehouse.service.WarehouseLocationService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WarehouseLocationServiceImpl extends ServiceImpl<WarehouseLocationMapper, WarehouseLocation>
        implements WarehouseLocationService {

    @Resource
    private ProductMapper productMapper;

    @Override
    public IPage<LocationVO> page(Page<WarehouseLocation> page) {
        QueryWrapper<WarehouseLocation> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("create_time");
        IPage<WarehouseLocation> locationPage = baseMapper.selectPage(page, wrapper);
        List<LocationVO> voList = locationPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        IPage<LocationVO> voPage = new Page<>(locationPage.getCurrent(), locationPage.getSize(), locationPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public LocationVO getById(Long locationId) {
        WarehouseLocation location = baseMapper.selectById(locationId);
        if (location == null) {
            throw new BusinessException(404, "库位不存在");
        }
        return convertToVO(location);
    }

    @Override
    @Transactional
    public LocationVO create(LocationCreateDTO dto) {
        QueryWrapper<WarehouseLocation> wrapper = new QueryWrapper<>();
        wrapper.eq("location_code", dto.getLocationCode());
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(400, "库位编码已存在");
        }
        WarehouseLocation location = new WarehouseLocation();
        BeanUtils.copyProperties(dto, location);
        location.setStatus(1);
        baseMapper.insert(location);
        return convertToVO(location);
    }

    @Override
    @Transactional
    public LocationVO update(Long locationId, LocationUpdateDTO dto) {
        WarehouseLocation location = baseMapper.selectById(locationId);
        if (location == null) {
            throw new BusinessException(404, "库位不存在");
        }
        BeanUtils.copyProperties(dto, location, "locationId", "locationCode");
        baseMapper.updateById(location);
        return convertToVO(baseMapper.selectById(locationId));
    }

    @Override
    @Transactional
    public void delete(Long locationId) {
        WarehouseLocation location = baseMapper.selectById(locationId);
        if (location == null) {
            throw new BusinessException(404, "库位不存在");
        }
        // Check no products reference this location as default_location_id
        QueryWrapper<Product> productWrapper = new QueryWrapper<>();
        productWrapper.eq("default_location_id", locationId);
        if (productMapper.selectCount(productWrapper) > 0) {
            throw new BusinessException(400, "该库位被产品引用，无法删除");
        }
        location.setStatus(0);
        baseMapper.updateById(location);
    }

    @Override
    public List<WarehouseLocation> listEnabled() {
        QueryWrapper<WarehouseLocation> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        return baseMapper.selectList(wrapper);
    }

    private LocationVO convertToVO(WarehouseLocation location) {
        LocationVO vo = new LocationVO();
        BeanUtils.copyProperties(location, vo);
        return vo;
    }
}
