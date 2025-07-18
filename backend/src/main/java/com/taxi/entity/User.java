 package com.taxi.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class User {

    private Long id;

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

    /** 身份证号 */
    private String idCard;

    /** 头像URL */
    private String avatar;

    /** 用户类型：ADMIN-管理员，DRIVER-司机，PASSENGER-乘客 */
    private String userType;

    /** 用户状态：ACTIVE-激活，INACTIVE-未激活，BANNED-禁用 */
    private String status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}