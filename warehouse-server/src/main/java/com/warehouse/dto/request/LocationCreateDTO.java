package com.warehouse.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LocationCreateDTO {
    @NotBlank
    private String locationCode;

    @NotBlank
    private String locationName;

    private String zone;

    // 修复:补 status 字段,前端 form.status 默认 1;null 时 Service 兜底为 1
    private Integer status;
}
