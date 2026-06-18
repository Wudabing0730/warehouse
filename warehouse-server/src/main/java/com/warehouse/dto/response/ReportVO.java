package com.warehouse.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReportVO {
    private String name;

    private BigDecimal totalQuantity;

    private BigDecimal totalAmount;

    private Long totalCount;
}
