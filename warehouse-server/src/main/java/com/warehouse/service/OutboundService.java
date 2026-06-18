package com.warehouse.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.warehouse.dto.request.OutboundConfirmDTO;
import com.warehouse.dto.request.OutboundCreateDTO;
import com.warehouse.dto.request.OutboundQueryDTO;
import com.warehouse.dto.response.OutboundOrderVO;
import com.warehouse.entity.OutboundOrder;

public interface OutboundService extends IService<OutboundOrder> {

    /**
     * Paginated query of outbound orders with optional filters
     */
    IPage<OutboundOrderVO> page(Page<OutboundOrder> page, OutboundQueryDTO query);

    /**
     * Get outbound order with details by order ID
     */
    OutboundOrderVO getById(Long orderId);

    /**
     * Create a new outbound order with details
     */
    OutboundOrderVO create(OutboundCreateDTO dto);

    /**
     * Confirm (approve or reject) a pending outbound order
     */
    void confirm(Long orderId, OutboundConfirmDTO dto);
}
