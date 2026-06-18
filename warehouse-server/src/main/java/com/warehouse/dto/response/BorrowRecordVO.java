package com.warehouse.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class BorrowRecordVO {
    private Long recordId;

    private String recordNo;

    private Long productId;

    private String productCode;

    private String productName;

    private String productUnit;

    private BigDecimal quantity;

    private String borrower;

    private LocalDate borrowDate;

    private LocalDate expectedReturnDate;

    private LocalDate actualReturnDate;

    private BigDecimal returnQuantity;

    private Long operatorId;

    private String operatorName;

    private Integer status;

    private String remark;

    private LocalDateTime createTime;
}
