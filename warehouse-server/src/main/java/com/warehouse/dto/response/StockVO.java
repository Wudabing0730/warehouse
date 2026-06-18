package com.warehouse.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StockVO {
    private Long stockId;

    private Long productId;

    private String productCode;

    private String productName;

    private String productUnit;

    private Long locationId;

    private String locationCode;

    private String locationName;

    private BigDecimal quantity;

    private BigDecimal upperLimit;

    private BigDecimal lowerLimit;

    private LocalDateTime updateTime;

    /** 预警消息 (用于getAlerts接口) */
    private String alertMessage;
}
