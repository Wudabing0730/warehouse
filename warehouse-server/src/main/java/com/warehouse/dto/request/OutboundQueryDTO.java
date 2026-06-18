package com.warehouse.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OutboundQueryDTO {
    private String orderNo;

    private Long customerId;

    private Integer status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
