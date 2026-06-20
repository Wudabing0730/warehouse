package com.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.warehouse.common.BusinessException;
import com.warehouse.dto.request.LocationCreateDTO;
import com.warehouse.dto.request.LocationQueryDTO;
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

@Service
public class WarehouseLocationServiceImpl extends ServiceImpl<WarehouseLocationMapper, WarehouseLocation>
        implements WarehouseLocationService {

    @Resource
    private ProductMapper productMapper;

    @Override
    public IPage<LocationVO> page(Page<WarehouseLocation> page, LocationQueryDTO query) {
        QueryWrapper<WarehouseLocation> wrapper = new QueryWrapper<>();
        if (query != null) {
            if (query.getLocationCode() != null && !query.getLocationCode().isEmpty()) {
                wrapper.like("location_code", query.getLocationCode());
            }
            if (query.getLocationName() != null && !query.getLocationName().isEmpty()) {
                wrapper.like("location_name", query.getLocationName());
            }
            if (query.getZone() != null && !query.getZone().isEmpty()) {
                wrapper.like("zone", query.getZone());
            }
            if (query.getStatus() != null) {
                wrapper.eq("status", query.getStatus());
            }
        }
        wrapper.orderByDesc("create_time");
        // 修复:依赖 MybatisPlusConfig.mybatisPlusInterceptor() 注册的 PaginationInnerInterceptor
        // 由拦截器自动执行 COUNT(*) + LIMIT,删除此前手动 selectCount + wrapper.last("LIMIT ...")
        // (后者会与全局拦截器重复拼接 LIMIT,触发 BadSqlGrammarException)
        IPage<WarehouseLocation> locationPage = baseMapper.selectPage(page, wrapper);
        return locationPage.convert(this::convertToVO);
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
        // 修复:DTO 现在含 status;null 时兜底为 1(启用),保持与 DB 默认值一致
        if (location.getStatus() == null) {
            location.setStatus(1);
        }
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
        // Check no products reference this location as default_location_id (FK 引用检查保留)
        QueryWrapper<Product> productWrapper = new QueryWrapper<>();
        productWrapper.eq("default_location_id", locationId);
        if (productMapper.selectCount(productWrapper) > 0) {
            throw new BusinessException(400, "该库位被产品引用，无法删除");
        }
        baseMapper.deleteById(locationId);
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
