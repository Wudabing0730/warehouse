package com.warehouse.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SupplierCreateDTO {
    @NotBlank
    private String supplierCode;

    @NotBlank
    private String supplierName;

    private String contactPerson;

    private String contactPhone;

    private String contactEmail;

    private String address;
}
