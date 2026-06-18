package com.warehouse.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OutboundOrderVO {
    private Long orderId;

    private String orderNo;

    private Long customerId;

    private String customerName;

    private String department;

    private String applicant;

    private Long operatorId;

    private String operatorName;

    private Long confirmOperatorId;

    private String confirmOperatorName;

    private LocalDateTime orderTime;

    private LocalDateTime confirmTime;

    private Integer status;

    private String remark;

    private List<OutboundOrderDetailVO> details;

    private LocalDateTime createTime;
}
