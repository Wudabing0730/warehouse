package com.warehouse.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_operation_log")
public class OperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
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

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
