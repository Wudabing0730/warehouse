package com.warehouse.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InboundDetailDTO {
    @NotNull
    private Long productId;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal quantity;

    private BigDecimal unitPrice;

    private Long locationId;
}
