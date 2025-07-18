 package com.taxi.controller;

import com.taxi.common.Result;
import com.taxi.dto.LoginRequest;
import com.taxi.dto.RegisterRequest;
import com.taxi.dto.UserInfo;
import com.taxi.entity.User;
import com.taxi.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

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

    /** 获取用户信息 */
    @GetMapping("/{id}")
    public Result<UserInfo> getUserInfo(@PathVariable Long id) {
        try {
            UserInfo userInfo = userService.getUserInfo(id);
            return Result.success(userInfo);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /** 更新用户信息 */
    @PutMapping("/{id}")
    public Result<UserInfo> updateUserInfo(@PathVariable Long id, @RequestBody User user) {
        try {
            UserInfo userInfo = userService.updateUserInfo(id, user);
            return Result.success(userInfo);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /** 删除用户 */
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /** 获取所有用户 */
    @GetMapping("/list")
    public Result<List<UserInfo>> getAllUsers() {
        try {
            List<UserInfo> users = userService.getAllUsers();
            return Result.success(users);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /** 根据用户类型获取用户列表 */
    @GetMapping("/list/type/{userType}")
    public Result<List<UserInfo>> getUsersByType(@PathVariable String userType) {
        try {
            List<UserInfo> users = userService.getUsersByType(userType);
            return Result.success(users);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}