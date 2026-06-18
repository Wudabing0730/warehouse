package com.warehouse.dto.request;

import lombok.Data;

/**
 * 供应商列表查询条件(P1-2 修复)
 *
 * Bug:Controller.list 只接 page/size,前端搜索框输入的 supplierCode/supplierName 被丢弃
 * Fix:Controller 接 SupplierQueryDTO,Service 用 LambdaQueryWrapper 拼接条件
 */
@Data
public class SupplierQueryDTO {

    private String supplierCode;

    private String supplierName;

    private Integer status;
}