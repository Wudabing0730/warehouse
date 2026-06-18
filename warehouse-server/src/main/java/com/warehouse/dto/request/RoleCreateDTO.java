package com.warehouse.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class RoleCreateDTO {

    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    private String roleDesc;
    private Integer status;
    private List<Long> permissionIds;
}
