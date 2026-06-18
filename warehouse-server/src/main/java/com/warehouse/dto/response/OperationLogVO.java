package com.warehouse.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationLogVO {
    private Long logId;

    private Long userId;

    private String username;

    private String operation;

    private String module;

    private String requestMethod;

    private String requestUrl;

    private String requestParams;

    private String responseResult;

    private Integer executionTime;

    private String ipAddress;

    private String userAgent;

    private Integer status;

    private LocalDateTime operateTime;
}
