package com.taxi.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员用户实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class AdminUser {

    private Long id;

    /** 用户ID */
    private Long userId;

    /** 管理员角色 */
    private String role;

    /** 权限配置 */
    private List<String> permissions;

    /** 部门 */
    private String department;

    /** 职位 */
    private String position;

    /** 最后登录时间 */
    private LocalDateTime lastLoginAt;

    /** 最后登录IP */
    private String lastLoginIp;

    /** 登录次数 */
    private Integer loginCount;

    /** 是否激活 */
    private Boolean isActive;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    // 关联的用户信息
    private User user;
}