package com.warehouse.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class PermissionVO {

    private Long permissionId;
    private String permissionCode;
    private String permissionName;
    private String resourceType;
    private Long parentId;
    private Integer status;
    private List<PermissionVO> children;
}
