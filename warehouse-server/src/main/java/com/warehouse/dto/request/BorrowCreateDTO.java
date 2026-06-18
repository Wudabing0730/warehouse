package com.warehouse.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BorrowCreateDTO {
    @NotNull
    private Long productId;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal quantity;

    @NotBlank
    private String borrower;

    @NotNull
    private LocalDate borrowDate;

    @NotNull
    private LocalDate expectedReturnDate;

    private String remark;
}
