package com.warehouse.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.warehouse.dto.request.InventoryCheckConfirmDTO;
import com.warehouse.dto.request.InventoryCheckCreateDTO;
import com.warehouse.dto.request.InventoryCheckQueryDTO;
import com.warehouse.dto.response.InventoryCheckVO;
import com.warehouse.entity.InventoryCheck;

public interface InventoryCheckService extends IService<InventoryCheck> {

    /**
     * Paginated query of inventory check records with optional filters
     */
    IPage<InventoryCheckVO> page(Page<InventoryCheck> page, InventoryCheckQueryDTO query);

    /**
     * Get inventory check record by check ID
     */
    InventoryCheckVO getById(Long checkId);

    /**
     * Create a new inventory check record
     */
    InventoryCheckVO create(InventoryCheckCreateDTO dto);

    /**
     * Confirm (approve and adjust, or reject) a pending inventory check
     */
    void confirm(Long checkId, InventoryCheckConfirmDTO dto);
}
