package com.taxi.dto;

import lombok.Data;

/**
 * 创建管理员请求DTO
 */
@Data
public class AdminCreateRequest {

    /** 用户名 */
    private String username;

    /** 密码 */
    private String password;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 真实姓名 */
    private String realName;

    /** 管理员角色 */
    private String role = "ADMIN";

    /** 部门 */
    private String department;

    /** 职位 */
    private String position;

    /** 权限列表（逗号分隔） */
    private String permissions = "USER_MANAGEMENT,ORDER_MANAGEMENT,REVIEW_MANAGEMENT,STATISTICS";
}