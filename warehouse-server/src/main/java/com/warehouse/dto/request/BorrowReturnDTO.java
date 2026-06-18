package com.warehouse.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BorrowReturnDTO {
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal returnQuantity;

    /**
     * P0-4 修复:前端 el-date-picker type=datetime 提交 "YYYY-MM-dd HH:mm:ss",
     * 后端原本是 LocalDate,Jackson 反序列化失败 400。改为 LocalDateTime 精确记录归还时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime actualReturnDate;
}