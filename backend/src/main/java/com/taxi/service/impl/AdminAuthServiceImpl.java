package com.taxi.service.impl;

import com.taxi.dto.AdminLoginRequest;
import com.taxi.dto.AdminLoginResponse;
import com.taxi.dto.AdminProfile;
import com.taxi.entity.AdminUser;
import com.taxi.entity.User;
import com.taxi.mapper.AdminUserMapper;
import com.taxi.mapper.UserMapper;
import com.taxi.service.AdminAuthService;
import com.taxi.util.JwtUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 管理员认证服务实现类
 */
@Service
public class AdminAuthServiceImpl implements AdminAuthService {

    @Autowired
    private AdminUserMapper adminUserMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public AdminLoginResponse login(AdminLoginRequest request, String ipAddress, String userAgent) {
        // 1. 验证用户名和密码
        User user = userMapper.selectByUsername(request.getUsername());
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 2. 验证是否为管理员
        if (!"ADMIN".equals(user.getUserType())) {
            throw new RuntimeException("非管理员用户无法登录");
        }

        // 3. 检查用户状态
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("账号已被禁用");
        }

        // 4. 获取管理员详细信息
        AdminUser adminUser = adminUserMapper.selectByUserId(user.getId());
        if (adminUser == null) {
            throw new RuntimeException("管理员信息不存在");
        }

        if (!adminUser.getIsActive()) {
            throw new RuntimeException("管理员账号已被禁用");
        }

        // 5. 更新登录信息
        adminUserMapper.updateLoginInfo(adminUser.getId(), LocalDateTime.now(), ipAddress);

        // 6. 生成JWT Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), "ADMIN");

        // 7. 构建响应
        AdminLoginResponse response = new AdminLoginResponse();
        response.setToken(token);
        response.setExpiresAt(LocalDateTime.now().plusHours(24)); // Token 24小时过期

        // 8. 设置管理员信息
        AdminProfile adminProfile = buildAdminProfile(adminUser, user);
        response.setAdminInfo(adminProfile);

        // 9. 设置权限和菜单
        response.setPermissions(adminUser.getPermissions());
        response.setMenus(buildAdminMenus(adminUser.getRole(), adminUser.getPermissions()));

        return response;
    }

    @Override
    public void logout(Long adminId) {
        // 这里可以实现Token黑名单机制
        // 目前简单记录登出日志
        logOperation(adminId, "LOGOUT", "管理员登出", "ADMIN", adminId, null, null);
    }

    @Override
    public AdminProfile getAdminProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        AdminUser adminUser = adminUserMapper.selectByUserId(userId);
        if (adminUser == null) {
            throw new RuntimeException("管理员信息不存在");
        }

        return buildAdminProfile(adminUser, user);
    }

    @Override
    public AdminLoginResponse refreshToken(String token) {
        try {
            Long userId = jwtUtil.getUserIdFromToken(token);
            String username = jwtUtil.getUsernameFromToken(token);
            
            // 生成新Token
            String newToken = jwtUtil.generateToken(userId, username, "ADMIN");
            
            AdminLoginResponse response = new AdminLoginResponse();
            response.setToken(newToken);
            response.setExpiresAt(LocalDateTime.now().plusHours(24));
            
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Token刷新失败");
        }
    }

    @Override
    public void changePassword(Long adminId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(adminId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("当前密码不正确");
        }

        // 更新新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        // 记录操作日志
        logOperation(adminId, "CHANGE_PASSWORD", "修改密码", "ADMIN", adminId, null, null);
    }

    @Override
    public boolean hasPermission(Long adminId, String permission) {
        AdminUser adminUser = adminUserMapper.selectByUserId(adminId);
        if (adminUser == null) {
            return false;
        }

        List<String> permissions = adminUser.getPermissions();
        if (permissions == null) {
            return false;
        }

        // 超级管理员拥有所有权限
        if ("SUPER_ADMIN".equals(adminUser.getRole()) || permissions.contains("ALL")) {
            return true;
        }

        return permissions.contains(permission);
    }

    @Override
    public void logOperation(Long adminId, String operationType, String operationDesc, 
                           String targetType, Long targetId, String ipAddress, String userAgent) {
        // TODO: 实现操作日志记录
        // 这里可以异步记录操作日志
        System.out.println("管理员操作日志: " + adminId + " - " + operationType + " - " + operationDesc);
    }

    /**
     * 构建管理员个人信息
     */
    private AdminProfile buildAdminProfile(AdminUser adminUser, User user) {
        AdminProfile profile = new AdminProfile();
        profile.setId(adminUser.getId());
        profile.setUserId(user.getId());
        profile.setUsername(user.getUsername());
        profile.setRealName(user.getRealName());
        profile.setEmail(user.getEmail());
        profile.setPhone(user.getPhone());
        profile.setAvatar(user.getAvatar());
        profile.setRole(adminUser.getRole());
        profile.setRoleName(getRoleName(adminUser.getRole()));
        profile.setDepartment(adminUser.getDepartment());
        profile.setPosition(adminUser.getPosition());
        profile.setPermissions(adminUser.getPermissions());
        profile.setLastLoginAt(adminUser.getLastLoginAt());
        profile.setLastLoginIp(adminUser.getLastLoginIp());
        profile.setLoginCount(adminUser.getLoginCount());
        profile.setCreatedAt(adminUser.getCreatedAt());
        profile.setUpdatedAt(adminUser.getUpdatedAt());
        return profile;
    }

    /**
     * 获取角色名称
     */
    private String getRoleName(String role) {
        switch (role) {
            case "SUPER_ADMIN":
                return "超级管理员";
            case "ADMIN":
                return "管理员";
            case "OPERATOR":
                return "操作员";
            default:
                return "未知角色";
        }
    }

    /**
     * 构建管理员菜单
     */
    private List<AdminLoginResponse.AdminMenu> buildAdminMenus(String role, List<String> permissions) {
        List<AdminLoginResponse.AdminMenu> menus = new ArrayList<>();

        // 仪表板 - 所有管理员都有
        AdminLoginResponse.AdminMenu dashboard = new AdminLoginResponse.AdminMenu();
        dashboard.setId("dashboard");
        dashboard.setName("仪表板");
        dashboard.setPath("/admin/dashboard");
        dashboard.setIcon("dashboard");
        dashboard.setSort(1);
        menus.add(dashboard);

        // 用户管理
        if (hasMenuPermission(role, permissions, "USER_MANAGEMENT")) {
            AdminLoginResponse.AdminMenu userMgmt = new AdminLoginResponse.AdminMenu();
            userMgmt.setId("user-management");
            userMgmt.setName("用户管理");
            userMgmt.setPath("/admin/users");
            userMgmt.setIcon("user");
            userMgmt.setSort(2);
            menus.add(userMgmt);
        }

        // 订单管理
        if (hasMenuPermission(role, permissions, "ORDER_MANAGEMENT")) {
            AdminLoginResponse.AdminMenu orderMgmt = new AdminLoginResponse.AdminMenu();
            orderMgmt.setId("order-management");
            orderMgmt.setName("订单管理");
            orderMgmt.setPath("/admin/orders");
            orderMgmt.setIcon("order");
            orderMgmt.setSort(3);
            menus.add(orderMgmt);
        }

        // 评价管理
        if (hasMenuPermission(role, permissions, "REVIEW_MANAGEMENT")) {
            AdminLoginResponse.AdminMenu reviewMgmt = new AdminLoginResponse.AdminMenu();
            reviewMgmt.setId("review-management");
            reviewMgmt.setName("评价管理");
            reviewMgmt.setPath("/admin/reviews");
            reviewMgmt.setIcon("star");
            reviewMgmt.setSort(4);
            menus.add(reviewMgmt);
        }

        // 数据统计
        if (hasMenuPermission(role, permissions, "STATISTICS")) {
            AdminLoginResponse.AdminMenu statistics = new AdminLoginResponse.AdminMenu();
            statistics.setId("statistics");
            statistics.setName("数据统计");
            statistics.setPath("/admin/statistics");
            statistics.setIcon("chart");
            statistics.setSort(5);
            menus.add(statistics);
        }

        // 系统监控 - 仅超级管理员
        if ("SUPER_ADMIN".equals(role)) {
            AdminLoginResponse.AdminMenu monitor = new AdminLoginResponse.AdminMenu();
            monitor.setId("system-monitor");
            monitor.setName("系统监控");
            monitor.setPath("/admin/monitor");
            monitor.setIcon("monitor");
            monitor.setSort(6);
            menus.add(monitor);
        }

        return menus;
    }

    /**
     * 检查菜单权限
     */
    private boolean hasMenuPermission(String role, List<String> permissions, String requiredPermission) {
        if ("SUPER_ADMIN".equals(role)) {
            return true;
        }
        if (permissions == null) {
            return false;
        }
        return permissions.contains("ALL") || permissions.contains(requiredPermission);
    }
}