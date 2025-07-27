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
    
    /** 乘客ID（当用户类型为PASSENGER时） */
    private Long passengerId;
    
    /** 司机ID（当用户类型为DRIVER时） */
    private Long driverId;
}