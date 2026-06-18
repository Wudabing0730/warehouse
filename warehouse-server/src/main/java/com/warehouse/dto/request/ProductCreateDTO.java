package com.warehouse.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductCreateDTO {
    @NotBlank
    private String productCode;

    @NotBlank
    private String productName;

    @NotNull
    private Long categoryId;

    @NotBlank
    private String unit;

    private String spec;

    private BigDecimal upperLimit;

    private BigDecimal lowerLimit;

    private Long defaultLocationId;
}
