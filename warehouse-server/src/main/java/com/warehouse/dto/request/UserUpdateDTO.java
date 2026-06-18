package com.warehouse.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class UserUpdateDTO {

    private String username;
    private String realName;
    private String phone;
    private String email;
    private Integer status;
    private String password;
    private List<Long> roleIds;
}
