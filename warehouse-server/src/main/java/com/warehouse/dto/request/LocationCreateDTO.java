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
}
