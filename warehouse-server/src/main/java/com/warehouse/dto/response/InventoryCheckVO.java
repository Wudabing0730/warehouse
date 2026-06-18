package com.warehouse.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class InventoryCheckVO {
    private Long checkId;

    private String checkNo;

    private Long productId;

    private String productCode;

    private String productName;

    private String productUnit;

    private BigDecimal bookQuantity;

    private BigDecimal actualQuantity;

    private BigDecimal diffQuantity;

    private Long operatorId;

    private String operatorName;

    private LocalDate checkDate;

    private Integer status;

    private String remark;

    private LocalDateTime createTime;
}
