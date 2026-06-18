package com.warehouse.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.warehouse.dto.request.BorrowCreateDTO;
import com.warehouse.dto.request.BorrowQueryDTO;
import com.warehouse.dto.request.BorrowReturnDTO;
import com.warehouse.dto.response.BorrowRecordVO;
import com.warehouse.entity.BorrowRecord;

public interface BorrowService extends IService<BorrowRecord> {

    /**
     * Paginated query of borrow records with optional filters
     */
    IPage<BorrowRecordVO> page(Page<BorrowRecord> page, BorrowQueryDTO query);

    /**
     * Get borrow record by record ID
     */
    BorrowRecordVO getById(Long recordId);

    /**
     * Create a new borrow record (also deducts stock)
     */
    BorrowRecordVO create(BorrowCreateDTO dto);

    /**
     * Return borrowed items (restore stock partially or fully)
     */
    BorrowRecordVO returnItem(Long recordId, BorrowReturnDTO dto);
}
