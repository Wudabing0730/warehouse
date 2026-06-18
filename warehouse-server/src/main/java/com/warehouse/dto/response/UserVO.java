package com.warehouse.dto.response;

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
}
