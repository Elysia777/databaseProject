package com.taxi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理员登录请求DTO
 */
@Data
public class AdminLoginRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 验证码 */
    private String captcha;

    /** 验证码key */
    private String captchaKey;

    /** 记住我 */
    private Boolean rememberMe = false;
}