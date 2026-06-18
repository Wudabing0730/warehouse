package com.warehouse.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员重置用户密码(P1-5)
 *
 * 与 PasswordChangeDTO 的区别:管理员重置场景不需要 oldPassword
 * 前端 use case:管理员在用户列表点"重置密码",只需提供新密码
 */
@Data
public class PasswordResetDTO {

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度 6-32 位")
    private String newPassword;
}