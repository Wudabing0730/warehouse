package com.warehouse.dto.response;

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
}
