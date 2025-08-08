package com.taxi.controller;

import com.taxi.common.Result;
import com.taxi.dto.AdminLoginRequest;
import com.taxi.dto.AdminLoginResponse;
import com.taxi.dto.AdminProfile;
import com.taxi.service.AdminAuthService;
import com.taxi.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员认证控制器
 */
@RestController
@RequestMapping("/api/admin/auth")
@CrossOrigin(origins = "*")
public class AdminAuthController {

    @Autowired
    private AdminAuthService adminAuthService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 管理员登录
     */
    @PostMapping("/login")
    public Result<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request, 
                                          HttpServletRequest httpRequest) {
        try {
            // 获取客户端IP
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            
            AdminLoginResponse response = adminAuthService.login(request, ipAddress, userAgent);
            return Result.success(response);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 管理员登出
     */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                Long adminId = jwtUtil.getUserIdFromToken(token);
                adminAuthService.logout(adminId);
            }
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取管理员个人信息
     */
    @GetMapping("/profile")
    public Result<AdminProfile> getProfile(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                Long adminId = jwtUtil.getUserIdFromToken(token);
                AdminProfile profile = adminAuthService.getAdminProfile(adminId);
                return Result.success(profile);
            } else {
                return Result.error("未提供有效的认证token");
            }
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refresh")
    public Result<AdminLoginResponse> refreshToken(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                AdminLoginResponse response = adminAuthService.refreshToken(token);
                return Result.success(response);
            } else {
                return Result.error("未提供有效的认证token");
            }
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 修改密码
     */
    @PostMapping("/change-password")
    public Result<Void> changePassword(@RequestBody Map<String, String> request, 
                                     HttpServletRequest httpRequest) {
        try {
            String token = httpRequest.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                Long adminId = jwtUtil.getUserIdFromToken(token);
                
                String oldPassword = request.get("oldPassword");
                String newPassword = request.get("newPassword");
                
                adminAuthService.changePassword(adminId, oldPassword, newPassword);
                return Result.success(null);
            } else {
                return Result.error("未提供有效的认证token");
            }
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}