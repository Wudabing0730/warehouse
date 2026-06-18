package com.warehouse.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    /** P0-3: 前端 row.id 兼容 */
    @JsonProperty("id")
    public Long getId() {
        return checkId;
    }

    /**
     * P0-7: 前端盘点表格/详情读 checkUser,后端字段叫 operatorName,
     * 加 getter 别名后 row.checkUser 不再是 undefined。
     */
    @JsonProperty("checkUser")
    public String getCheckUser() {
        return operatorName;
    }
}