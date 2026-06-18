package com.warehouse.dto.request;

import lombok.Data;

@Data
public class StockQueryDTO {
    private Long productId;

    private Long locationId;

    private String productCode;

    private String productName;
}
