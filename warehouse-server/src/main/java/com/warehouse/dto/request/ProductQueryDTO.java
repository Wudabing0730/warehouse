package com.warehouse.dto.request;

import lombok.Data;

@Data
public class ProductQueryDTO {
    private String productCode;

    private String productName;

    private Long categoryId;

    private Integer status;
}
