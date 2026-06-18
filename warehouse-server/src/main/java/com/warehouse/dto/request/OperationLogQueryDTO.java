package com.warehouse.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationLogQueryDTO {
    private Long userId;

    private String module;

    private Integer status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
