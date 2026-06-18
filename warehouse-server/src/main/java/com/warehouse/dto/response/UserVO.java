package com.warehouse.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserVO {

    private Long userId;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private Integer status;
    private List<Long> roleIds;
    private List<String> roleNames;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * 前端表格/按钮 row.id 与后端 userId 字段别名(P0-3)。
     * 通过 getter 暴露 id,既保留 userId 语义又兼容 row.id 访问。
     */
    @JsonProperty("id")
    public Long getId() {
        return userId;
    }
}