package com.warehouse.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class InboundCreateDTO {
    // 修复:补 @NotNull,与前端 formRules.supplierId 对齐
    @NotNull(message = "供应商不能为空")
    private Long supplierId;

    @NotNull
    private LocalDateTime orderTime;

    private String remark;

    @NotEmpty
    private List<InboundDetailDTO> details;
}
