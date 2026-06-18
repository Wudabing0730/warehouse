package com.warehouse.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductUpdateDTO {
    private String productName;

    private Long categoryId;

    private String unit;

    private String spec;

    private BigDecimal upperLimit;

    private BigDecimal lowerLimit;

    private Long defaultLocationId;

    private Integer status;
}
