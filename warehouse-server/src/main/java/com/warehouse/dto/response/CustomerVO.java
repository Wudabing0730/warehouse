package com.warehouse.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CustomerVO {

    @JsonProperty("customerId")
    private Long customerId;

    private String customerCode;

    private String customerName;

    private String contactPerson;

    private String contactPhone;

    private String contactEmail;

    private String address;

    private Integer status;

    private LocalDateTime createTime;

    @JsonProperty("id")
    public Long getId() {
        return customerId;
    }
}