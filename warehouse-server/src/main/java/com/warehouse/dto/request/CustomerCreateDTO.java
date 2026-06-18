package com.warehouse.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerCreateDTO {
    @NotBlank
    private String customerCode;

    @NotBlank
    private String customerName;

    private String contactPerson;

    private String contactPhone;

    private String contactEmail;

    private String address;
}
