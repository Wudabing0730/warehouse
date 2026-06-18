package com.warehouse.dto.request;

import lombok.Data;

@Data
public class CustomerUpdateDTO {
    private String customerName;

    private String contactPerson;

    private String contactPhone;

    private String contactEmail;

    private String address;

    private Integer status;
}
