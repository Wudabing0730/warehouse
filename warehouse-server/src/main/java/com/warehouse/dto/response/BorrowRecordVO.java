package com.warehouse.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BorrowRecordVO {

    @JsonProperty("recordId")
    private Long recordId;

    private String recordNo;

    private Long productId;

    private String productCode;

    private String productName;

    private String productUnit;

    private BigDecimal quantity;

    private String borrower;

    /** P0-4: 改为 LocalDateTime,与 Entity 和 DTO 一致 */
    private LocalDateTime borrowDate;

    private LocalDateTime expectedReturnDate;

    private LocalDateTime actualReturnDate;

    private BigDecimal returnQuantity;

    private Long operatorId;

    private String operatorName;

    private Integer status;

    private String remark;

    private LocalDateTime createTime;

    /** P0-3: 前端 row.id 兼容 */
    @JsonProperty("id")
    public Long getId() {
        return recordId;
    }

    /**
     * P0-5: 前端归还对话框 :max="remaining" 计算依赖 borrowQuantity,
     * 后端字段叫 quantity,加 getter 别名后 row.borrowQuantity 不再是 undefined。
     */
    @JsonProperty("borrowQuantity")
    public BigDecimal getBorrowQuantity() {
        return quantity;
    }
}