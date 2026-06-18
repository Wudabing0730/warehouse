package com.warehouse.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class InventoryCheckCreateDTO {
    @NotNull
    private Long productId;

    @NotNull
    private BigDecimal actualQuantity;

    @NotNull
    private LocalDate checkDate;

    private String remark;
}
