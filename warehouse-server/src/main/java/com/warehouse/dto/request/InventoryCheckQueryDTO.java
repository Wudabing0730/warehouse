package com.warehouse.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class InventoryCheckQueryDTO {
    private String checkNo;

    private Long productId;

    private Integer status;

    private LocalDate startDate;

    private LocalDate endDate;
}
