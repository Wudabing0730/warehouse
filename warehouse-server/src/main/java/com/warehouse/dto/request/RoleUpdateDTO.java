package com.warehouse.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class RoleUpdateDTO {

    private String roleName;
    private String roleDesc;
    private Integer status;
    private List<Long> permissionIds;
}
