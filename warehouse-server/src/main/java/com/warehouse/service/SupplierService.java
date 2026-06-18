package com.warehouse.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.warehouse.dto.request.SupplierCreateDTO;
import com.warehouse.dto.request.SupplierQueryDTO;
import com.warehouse.dto.request.SupplierUpdateDTO;
import com.warehouse.dto.response.SupplierVO;
import com.warehouse.entity.Supplier;

import java.util.List;

public interface SupplierService extends IService<Supplier> {

    IPage<SupplierVO> page(Page<Supplier> page, SupplierQueryDTO query);

    SupplierVO getById(Long supplierId);

    SupplierVO create(SupplierCreateDTO dto);

    SupplierVO update(Long supplierId, SupplierUpdateDTO dto);

    void delete(Long supplierId);

    List<Supplier> listEnabled();
}
