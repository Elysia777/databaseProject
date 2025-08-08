package com.taxi.dto;

import lombok.Data;

/**
 * 管理员密码重置请求DTO
 */
@Data
public class AdminPasswordResetRequest {

    /** 用户名 */
    private String username;

    /** 新密码 */
    private String newPassword;
}