package com.taxi.controller;

import com.taxi.common.Result;
import com.taxi.dto.AdminCreateRequest;
import com.taxi.dto.AdminPasswordResetRequest;
import com.taxi.entity.AdminUser;
import com.taxi.entity.User;
import com.taxi.mapper.AdminUserMapper;
import com.taxi.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理员管理控制器（用于创建和管理管理员账号）
 */
@RestController
@RequestMapping("/api/admin/management")
@CrossOrigin(origins = "*")
public class AdminManagementController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AdminUserMapper adminUserMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 创建管理员账号
     */
    @PostMapping("/create")
    public Result<Map<String, Object>> createAdmin(@RequestBody AdminCreateRequest request) {
        try {
            // 检查用户名是否已存在
            User existingUser = userMapper.selectByUsername(request.getUsername());
            if (existingUser != null) {
                return Result.error("用户名已存在");
            }

            // 创建用户记录
            User user = new User();
            user.setUsername(request.getUsername());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            // 处理空字符串的phone，避免唯一约束冲突
            user.setPhone(request.getPhone() != null && !request.getPhone().trim().isEmpty() ? request.getPhone() : null);
            user.setEmail(request.getEmail() != null && !request.getEmail().trim().isEmpty() ? request.getEmail() : null);
            user.setRealName(request.getRealName());
            user.setUserType("ADMIN");
            user.setStatus("ACTIVE");
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());

            userMapper.insert(user);

            // 创建管理员详细信息
            AdminUser adminUser = new AdminUser();
            adminUser.setUserId(user.getId());
            adminUser.setRole(request.getRole());
            adminUser.setDepartment(request.getDepartment());
            adminUser.setPosition(request.getPosition());
            adminUser.setPermissions(Arrays.asList(request.getPermissions().split(",")));
            adminUser.setIsActive(true);

            adminUserMapper.insert(adminUser);

            Map<String, Object> result = new HashMap<>();
            result.put("userId", user.getId());
            result.put("adminId", adminUser.getId());
            result.put("username", user.getUsername());
            result.put("realName", user.getRealName());
            result.put("role", adminUser.getRole());

            return Result.success(result);

        } catch (Exception e) {
            return Result.error("创建管理员失败: " + e.getMessage());
        }
    }

    /**
     * 重置管理员密码
     */
    @PostMapping("/reset-password")
    public Result<String> resetPassword(@RequestBody AdminPasswordResetRequest request) {
        try {
            User user = userMapper.selectByUsername(request.getUsername());
            if (user == null) {
                return Result.error("用户不存在");
            }

            if (!"ADMIN".equals(user.getUserType())) {
                return Result.error("该用户不是管理员");
            }

            // 更新密码
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(user);

            return Result.success("密码重置成功");

        } catch (Exception e) {
            return Result.error("密码重置失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有管理员列表
     */
    @GetMapping("/list")
    public Result<Object> getAdminList() {
        try {
            // 获取所有管理员用户
            Map<String, Object> result = new HashMap<>();
            
            // 从users表获取管理员用户
            result.put("message", "请使用 /api/users 接口获取用户列表，筛选 userType = 'ADMIN'");
            
            return Result.success(result);

        } catch (Exception e) {
            return Result.error("获取管理员列表失败: " + e.getMessage());
        }
    }

    /**
     * 生成密码哈希（用于测试）
     */
    @PostMapping("/generate-hash")
    public Result<Map<String, String>> generatePasswordHash(@RequestBody Map<String, String> request) {
        try {
            String password = request.get("password");
            if (password == null || password.isEmpty()) {
                return Result.error("密码不能为空");
            }

            String hash = passwordEncoder.encode(password);
            
            Map<String, String> result = new HashMap<>();
            result.put("password", password);
            result.put("hash", hash);
            
            return Result.success(result);

        } catch (Exception e) {
            return Result.error("生成密码哈希失败: " + e.getMessage());
        }
    }
}