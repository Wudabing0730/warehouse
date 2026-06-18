package com.warehouse.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RoleVO {

    private Long roleId;
    private String roleName;
    private String roleDesc;
    private Integer status;
    private List<Long> permissionIds;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @JsonProperty("id")
    public Long getId() {
        return roleId;
    }
}