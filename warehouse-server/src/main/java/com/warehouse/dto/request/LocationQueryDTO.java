package com.warehouse.dto.request;

import lombok.Data;

/**
 * 库位列表查询条件(P1-2 修复)
 *
 * Bug:Controller.list 只接 page/size,前端搜索框输入的 locationCode/locationName 被丢弃
 * Fix:Controller 接 LocationQueryDTO,Service 用 LambdaQueryWrapper 拼接条件
 */
@Data
public class LocationQueryDTO {

    private String locationCode;

    private String locationName;

    private String zone;

    private Integer status;
}