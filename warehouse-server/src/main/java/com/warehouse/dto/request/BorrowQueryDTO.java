package com.warehouse.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BorrowQueryDTO {
    private String recordNo;

    private Long productId;

    private String borrower;

    private Integer status;

    /** P0-4: 与实体/DTO 对齐为 LocalDateTime */
    private LocalDateTime startDate;

    private LocalDateTime endDate;
}
