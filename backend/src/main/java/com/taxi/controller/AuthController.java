package com.taxi.controller;

import com.taxi.common.Result;
import com.taxi.dto.LoginRequest;
import com.taxi.dto.RegisterRequest;
import com.taxi.dto.UserInfo;
import com.taxi.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserService userService;

    /** 用户登录 */
    @PostMapping("/login")
    public Result<UserInfo> login(@Valid @RequestBody LoginRequest request) {
        try {
            UserInfo userInfo = userService.login(request);
            return Result.success(userInfo);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /** 用户注册 */
    @PostMapping("/register")
    public Result<UserInfo> register(@Valid @RequestBody RegisterRequest request) {
        try {
            UserInfo userInfo = userService.register(request);
            return Result.success(userInfo);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /** 用户登出 */
    @PostMapping("/logout")
    public Result<Void> logout() {
        // 这里可以添加登出逻辑，比如清除Redis中的token等
        return Result.success(null);
    }
}