package com.warehouse.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductVO {
    private Long productId;

    private String productCode;

    private String productName;

    private Long categoryId;

    private String categoryName;

    private String unit;

    private String spec;

    private BigDecimal upperLimit;

    private BigDecimal lowerLimit;

    private Long defaultLocationId;

    private String defaultLocationName;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
