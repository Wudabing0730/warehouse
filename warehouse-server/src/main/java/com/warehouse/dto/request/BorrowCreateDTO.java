package com.warehouse.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BorrowCreateDTO {
    @NotNull
    private Long productId;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal quantity;

    @NotBlank
    private String borrower;

    /**
     * P0-4 修复:前端 el-date-picker type=datetime 提交 "YYYY-MM-DD HH:mm:ss",
     * 后端原本是 LocalDate,Jackson 反序列化失败 400。改为 LocalDateTime,
     * 并显式指定格式以容忍多种分隔符(空格/T)。
     */
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime borrowDate;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expectedReturnDate;

    private String remark;
}