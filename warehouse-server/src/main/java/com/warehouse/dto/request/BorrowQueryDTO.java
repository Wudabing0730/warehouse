package com.warehouse.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class BorrowQueryDTO {
    private String recordNo;

    private Long productId;

    private String borrower;

    private Integer status;

    private LocalDate startDate;

    private LocalDate endDate;
}
