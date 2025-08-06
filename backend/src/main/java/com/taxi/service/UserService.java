 package com.taxi.service;

import com.taxi.dto.LoginRequest;
import com.taxi.dto.RegisterRequest;
import com.taxi.dto.UserInfo;
import com.taxi.entity.User;

import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService {

    /** 用户注册 */
    UserInfo register(RegisterRequest request);

    /** 用户登录 */
    UserInfo login(LoginRequest request);

    /** 根据ID获取用户信息 */
    UserInfo getUserInfo(Long id);

    /** 更新用户信息 */
    UserInfo updateUserInfo(Long id, User user);

    /** 删除用户 */
    void deleteUser(Long id);

    /** 获取所有用户 */
    List<UserInfo> getAllUsers();

    /** 根据用户类型获取用户列表 */
    List<UserInfo> getUsersByType(String userType);

    /** 更新用户头像 */
    void updateUserAvatar(Long userId, String avatarUrl);

    /** 获取用户头像URL */
    String getUserAvatarUrl(Long userId);

    /** 修改密码 */
    void changePassword(Long userId, String oldPassword, String newPassword);
}