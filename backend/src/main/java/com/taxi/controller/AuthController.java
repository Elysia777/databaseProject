package com.taxi.controller;

import com.taxi.common.Result;
import com.taxi.dto.LoginRequest;
import com.taxi.dto.RegisterRequest;
import com.taxi.dto.UserInfo;
import com.taxi.service.UserService;
import com.taxi.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
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

    @Autowired
    private JwtUtil jwtUtil;

    /** 健康检查接口 */
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("服务运行正常");
    }

    /** 获取所有用户列表 */
    @GetMapping("/users")
    public Result<java.util.List<UserInfo>> getAllUsers() {
        try {
            java.util.List<UserInfo> users = userService.getAllUsers();
            return Result.success(users);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /** 获取当前用户信息 */
    @GetMapping("/user")
    public Result<UserInfo> getCurrentUser(HttpServletRequest request) {
        try {
            // 从JWT token中获取用户ID
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                Long userId = jwtUtil.getUserIdFromToken(token);
                UserInfo userInfo = userService.getUserInfo(userId);
                return Result.success(userInfo);
            } else {
                return Result.error("未提供有效的认证token");
            }
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /** 根据ID获取用户信息 */
    @GetMapping("/user/{id}")
    public Result<UserInfo> getUserById(@PathVariable Long id) {
        try {
            UserInfo userInfo = userService.getUserInfo(id);
            return Result.success(userInfo);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /** 调试接口：检查用户和乘客关联 */
    @GetMapping("/debug/user/{id}")
    public Result<String> debugUserInfo(@PathVariable Long id) {
        try {
            UserInfo userInfo = userService.getUserInfo(id);
            StringBuilder debug = new StringBuilder();
            debug.append("用户ID: ").append(userInfo.getId()).append("\n");
            debug.append("用户名: ").append(userInfo.getUsername()).append("\n");
            debug.append("用户类型: ").append(userInfo.getUserType()).append("\n");
            debug.append("乘客ID: ").append(userInfo.getPassengerId()).append("\n");
            debug.append("司机ID: ").append(userInfo.getDriverId()).append("\n");
            
            return Result.success(debug.toString());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /** 更新用户信息 */
    @PutMapping("/user/{id}")
    public Result<UserInfo> updateUser(@PathVariable Long id, @RequestBody com.taxi.entity.User user) {
        try {
            UserInfo userInfo = userService.updateUserInfo(id, user);
            return Result.success(userInfo);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /** 修改密码 */
    @PostMapping("/change-password")
    public Result<String> changePassword(@RequestBody java.util.Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            String oldPassword = request.get("oldPassword").toString();
            String newPassword = request.get("newPassword").toString();
            
            userService.changePassword(userId, oldPassword, newPassword);
            return Result.success("密码修改成功");
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