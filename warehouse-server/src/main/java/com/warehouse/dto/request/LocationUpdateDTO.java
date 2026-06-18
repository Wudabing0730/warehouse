package com.warehouse.dto.request;

import lombok.Data;

@Data
public class LocationUpdateDTO {
    private String locationName;

    private String zone;

    private Integer status;
}
