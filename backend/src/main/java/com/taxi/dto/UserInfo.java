 package com.taxi.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息DTO
 */
@Data
public class UserInfo {

    private Long id;
    private String username;
    private String phone;
    private String email;
    private String realName;
    private String idCard;
    private String avatar;
    private String userType;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** JWT Token */
    private String token;
}