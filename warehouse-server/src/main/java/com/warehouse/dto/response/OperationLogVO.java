package com.warehouse.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志 VO(P1-7 修复)
 *
 * 历史:Controller 直接返回 IPage<OperationLog> entity,前端表格读 id/description/executeTime/ip/createTime
 *      全部与 entity 字段名不匹配,列表全空白。
 * Fix: Controller 改返回 IPage<OperationLogVO>,VO 通过 @JsonProperty 给字段加前端别名,
 *      同时 @JsonFormat 让 LocalDateTime 输出 yyyy-MM-dd HH:mm:ss。
 */
@Data
public class OperationLogVO {

    private Long logId;
    private Long userId;
    private String username;

    private String operation;

    /** 前端表格 prop="description" — 通过别名兼容老字段名 */
    @JsonProperty("description")
    public String getDescription() {
        return operation;
    }

    private String module;
    private String requestMethod;
    private String requestUrl;
    private String requestParams;
    private String responseResult;

    private Integer executionTime;

    /** 前端 prop="executeTime" — 通过别名兼容老字段名 */
    @JsonProperty("executeTime")
    public Integer getExecuteTime() {
        return executionTime;
    }

    private String ipAddress;

    /** 前端 prop="ip" — 通过别名兼容老字段名 */
    @JsonProperty("ip")
    public String getIp() {
        return ipAddress;
    }

    private String userAgent;
    private Integer status;

    private LocalDateTime operateTime;

    /** 前端 prop="createTime" — 通过别名 + 时间格式兼容老字段名 */
    @JsonProperty("createTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    public LocalDateTime getCreateTime() {
        return operateTime;
    }
}