package com.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.warehouse.common.BusinessException;
import com.warehouse.dto.request.SupplierCreateDTO;
import com.warehouse.dto.request.SupplierQueryDTO;
import com.warehouse.dto.request.SupplierUpdateDTO;
import com.warehouse.dto.response.SupplierVO;
import com.warehouse.entity.Supplier;
import com.warehouse.mapper.SupplierMapper;
import com.warehouse.service.SupplierService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SupplierServiceImpl extends ServiceImpl<SupplierMapper, Supplier> implements SupplierService {

    @Override
    public IPage<SupplierVO> page(Page<Supplier> page, SupplierQueryDTO query) {
        QueryWrapper<Supplier> wrapper = new QueryWrapper<>();
        if (query != null) {
            if (query.getSupplierCode() != null && !query.getSupplierCode().isEmpty()) {
                wrapper.like("supplier_code", query.getSupplierCode());
            }
            if (query.getSupplierName() != null && !query.getSupplierName().isEmpty()) {
                wrapper.like("supplier_name", query.getSupplierName());
            }
            if (query.getStatus() != null) {
                wrapper.eq("status", query.getStatus());
            }
        }
        wrapper.orderByDesc("create_time");
        IPage<Supplier> supplierPage = baseMapper.selectPage(page, wrapper);
        List<SupplierVO> voList = supplierPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        IPage<SupplierVO> voPage = new Page<>(supplierPage.getCurrent(), supplierPage.getSize(), supplierPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public SupplierVO getById(Long supplierId) {
        Supplier supplier = baseMapper.selectById(supplierId);
        if (supplier == null) {
            throw new BusinessException(404, "供应商不存在");
        }
        return convertToVO(supplier);
    }

    @Override
    @Transactional
    public SupplierVO create(SupplierCreateDTO dto) {
        QueryWrapper<Supplier> wrapper = new QueryWrapper<>();
        wrapper.eq("supplier_code", dto.getSupplierCode());
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(400, "供应商编码已存在");
        }
        Supplier supplier = new Supplier();
        BeanUtils.copyProperties(dto, supplier);
        supplier.setStatus(1);
        baseMapper.insert(supplier);
        return convertToVO(supplier);
    }

    @Override
    @Transactional
    public SupplierVO update(Long supplierId, SupplierUpdateDTO dto) {
        Supplier supplier = baseMapper.selectById(supplierId);
        if (supplier == null) {
            throw new BusinessException(404, "供应商不存在");
        }
        BeanUtils.copyProperties(dto, supplier, "supplierId", "supplierCode");
        baseMapper.updateById(supplier);
        return convertToVO(baseMapper.selectById(supplierId));
    }

    @Override
    @Transactional
    public void delete(Long supplierId) {
        Supplier supplier = baseMapper.selectById(supplierId);
        if (supplier == null) {
            throw new BusinessException(404, "供应商不存在");
        }
        supplier.setStatus(0);
        baseMapper.updateById(supplier);
    }

    @Override
    public List<Supplier> listEnabled() {
        QueryWrapper<Supplier> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        return baseMapper.selectList(wrapper);
    }

    private SupplierVO convertToVO(Supplier supplier) {
        SupplierVO vo = new SupplierVO();
        BeanUtils.copyProperties(supplier, vo);
        return vo;
    }
}
