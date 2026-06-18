package com.warehouse.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

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

    private java.math.BigDecimal upperLimit;

    private java.math.BigDecimal lowerLimit;

    private Long defaultLocationId;

    private String defaultLocationName;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @JsonProperty("id")
    public Long getId() {
        return productId;
    }
}