package com.warehouse.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OutboundOrderDetailVO {
    private Long detailId;

    private Long productId;

    private String productCode;

    private String productName;

    private String productUnit;

    private BigDecimal quantity;

    private BigDecimal unitPrice;

    private Long locationId;

    private String locationCode;

    private String locationName;
}
