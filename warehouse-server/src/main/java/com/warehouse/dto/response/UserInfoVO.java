package com.warehouse.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserInfoVO {
    private Long userId;

    private String username;

    private String realName;

    private String phone;

    private String email;

    private Integer status;

    private List<String> roleNames;

    private LocalDateTime createTime;
}
