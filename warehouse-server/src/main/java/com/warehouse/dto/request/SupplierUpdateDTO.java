package com.warehouse.dto.request;

import lombok.Data;

@Data
public class SupplierUpdateDTO {
    private String supplierName;

    private String contactPerson;

    private String contactPhone;

    private String contactEmail;

    private String address;

    private Integer status;
}
