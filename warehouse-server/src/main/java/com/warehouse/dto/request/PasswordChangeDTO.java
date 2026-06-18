package com.warehouse.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户自助修改密码(P1-5 拆分自原 PasswordResetDTO)
 *
 * use case:用户在个人中心修改自己的密码,必须提供旧密码
 */
@Data
public class PasswordChangeDTO {

    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度 6-32 位")
    private String newPassword;
}