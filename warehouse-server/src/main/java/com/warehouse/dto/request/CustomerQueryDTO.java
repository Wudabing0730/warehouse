package com.warehouse.dto.request;

import lombok.Data;

/**
 * 客户列表查询条件(P1-2 修复)
 *
 * Bug:Controller.list 只接 page/size,前端搜索框输入的 customerCode/customerName 被丢弃
 * Fix:Controller 接 CustomerQueryDTO,Service 用 LambdaQueryWrapper 拼接条件
 */
@Data
public class CustomerQueryDTO {

    private String customerCode;

    private String customerName;

    private Integer status;
}