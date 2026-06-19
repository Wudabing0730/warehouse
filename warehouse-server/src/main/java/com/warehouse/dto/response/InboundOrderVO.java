package com.warehouse.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class InboundOrderVO {

    private Long orderId;

    private String orderNo;

    private Long supplierId;

    private String supplierName;

    private Long operatorId;

    private String operatorName;

    private Long confirmOperatorId;

    private String confirmOperatorName;

    private LocalDateTime orderTime;

    private LocalDateTime confirmTime;

    private Integer status;

    private String remark;

    private List<InboundOrderDetailVO> details;

    private LocalDateTime createTime;

    @JsonProperty("id")
    public Long getId() {
        return orderId;
    }

    /** P2-2: 前端表格/详情读 auditorName,后端字段叫 confirmOperatorName,加 getter 别名 */
    @JsonProperty("auditorName")
    public String getAuditorName() {
        return confirmOperatorName;
    }

    /** P2-2: 前端表格/详情读 auditTime,后端字段叫 confirmTime,加 getter 别名 */
    @JsonProperty("auditTime")
    public java.time.LocalDateTime getAuditTime() {
        return confirmTime;
    }
}