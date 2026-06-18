package com.warehouse.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.warehouse.dto.request.InboundConfirmDTO;
import com.warehouse.dto.request.InboundCreateDTO;
import com.warehouse.dto.request.InboundQueryDTO;
import com.warehouse.dto.response.InboundOrderVO;
import com.warehouse.entity.InboundOrder;

public interface InboundService extends IService<InboundOrder> {

    /**
     * Paginated query of inbound orders with optional filters
     */
    IPage<InboundOrderVO> page(Page<InboundOrder> page, InboundQueryDTO query);

    /**
     * Get inbound order with details by order ID
     */
    InboundOrderVO getById(Long orderId);

    /**
     * Create a new inbound order with details
     */
    InboundOrderVO create(InboundCreateDTO dto);

    /**
     * Confirm (approve or reject) a pending inbound order
     */
    void confirm(Long orderId, InboundConfirmDTO dto);
}
