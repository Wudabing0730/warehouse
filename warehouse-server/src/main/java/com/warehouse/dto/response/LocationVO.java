package com.warehouse.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LocationVO {
    private Long locationId;

    private String locationCode;

    private String locationName;

    private String zone;

    private Integer status;

    private LocalDateTime createTime;
}
