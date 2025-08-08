package com.taxi.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员登录响应DTO
 */
@Data
public class AdminLoginResponse {

    /** JWT Token */
    private String token;

    /** Token类型 */
    private String tokenType = "Bearer";

    /** Token过期时间 */
    private LocalDateTime expiresAt;

    /** 管理员信息 */
    private AdminProfile adminInfo;

    /** 权限列表 */
    private List<String> permissions;

    /** 菜单列表 */
    private List<AdminMenu> menus;

    @Data
    public static class AdminMenu {
        private String id;
        private String name;
        private String path;
        private String icon;
        private Integer sort;
        private List<AdminMenu> children;
    }
}