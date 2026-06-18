package com.warehouse.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BorrowReturnDTO {
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal returnQuantity;

    private LocalDate actualReturnDate;
}
