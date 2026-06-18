package com.warehouse.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InboundQueryDTO {
    private String orderNo;

    private Long supplierId;

    private Integer status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
