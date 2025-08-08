package com.taxi.service;

import com.taxi.dto.AdminLoginRequest;
import com.taxi.dto.AdminLoginResponse;
import com.taxi.dto.AdminProfile;

/**
 * 管理员认证服务接口
 */
public interface AdminAuthService {

    /**
     * 管理员登录
     */
    AdminLoginResponse login(AdminLoginRequest request, String ipAddress, String userAgent);

    /**
     * 管理员登出
     */
    void logout(Long adminId);

    /**
     * 获取管理员个人信息
     */
    AdminProfile getAdminProfile(Long adminId);

    /**
     * 刷新Token
     */
    AdminLoginResponse refreshToken(String token);

    /**
     * 修改密码
     */
    void changePassword(Long adminId, String oldPassword, String newPassword);

    /**
     * 验证管理员权限
     */
    boolean hasPermission(Long adminId, String permission);

    /**
     * 记录操作日志
     */
    void logOperation(Long adminId, String operationType, String operationDesc, 
                     String targetType, Long targetId, String ipAddress, String userAgent);
}