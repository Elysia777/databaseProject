package com.taxi.mapper;

import com.taxi.entity.AdminUser;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员用户Mapper接口
 */
@Mapper
public interface AdminUserMapper {

    /**
     * 根据ID查询管理员
     */
    @Select("SELECT * FROM admin_users WHERE id = #{id}")
    @Results({
        @Result(property = "userId", column = "user_id"),
        @Result(property = "lastLoginAt", column = "last_login_at"),
        @Result(property = "lastLoginIp", column = "last_login_ip"),
        @Result(property = "loginCount", column = "login_count"),
        @Result(property = "isActive", column = "is_active"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at"),
        @Result(property = "permissions", column = "permissions", 
                typeHandler = com.taxi.handler.JsonListTypeHandler.class),
        @Result(property = "user", column = "user_id", 
                one = @One(select = "com.taxi.mapper.UserMapper.selectById"))
    })
    AdminUser selectById(Long id);

    /**
     * 根据用户ID查询管理员
     */
    @Select("SELECT * FROM admin_users WHERE user_id = #{userId}")
    @Results({
        @Result(property = "userId", column = "user_id"),
        @Result(property = "lastLoginAt", column = "last_login_at"),
        @Result(property = "lastLoginIp", column = "last_login_ip"),
        @Result(property = "loginCount", column = "login_count"),
        @Result(property = "isActive", column = "is_active"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at"),
        @Result(property = "permissions", column = "permissions", 
                typeHandler = com.taxi.handler.JsonListTypeHandler.class),
        @Result(property = "user", column = "user_id", 
                one = @One(select = "com.taxi.mapper.UserMapper.selectById"))
    })
    AdminUser selectByUserId(Long userId);

    /**
     * 查询所有管理员
     */
    @Select("SELECT * FROM admin_users WHERE is_active = true ORDER BY created_at DESC")
    @Results({
        @Result(property = "userId", column = "user_id"),
        @Result(property = "lastLoginAt", column = "last_login_at"),
        @Result(property = "lastLoginIp", column = "last_login_ip"),
        @Result(property = "loginCount", column = "login_count"),
        @Result(property = "isActive", column = "is_active"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at"),
        @Result(property = "permissions", column = "permissions", 
                typeHandler = com.taxi.handler.JsonListTypeHandler.class),
        @Result(property = "user", column = "user_id", 
                one = @One(select = "com.taxi.mapper.UserMapper.selectById"))
    })
    List<AdminUser> selectAll();

    /**
     * 插入管理员
     */
    @Insert("INSERT INTO admin_users (user_id, role, permissions, department, position, is_active) " +
            "VALUES (#{userId}, #{role}, #{permissions, typeHandler=com.taxi.handler.JsonListTypeHandler}, " +
            "#{department}, #{position}, #{isActive})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AdminUser adminUser);

    /**
     * 更新管理员信息
     */
    @Update("UPDATE admin_users SET role = #{role}, permissions = #{permissions, typeHandler=com.taxi.handler.JsonListTypeHandler}, " +
            "department = #{department}, position = #{position}, is_active = #{isActive}, " +
            "updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateById(AdminUser adminUser);

    /**
     * 更新登录信息
     */
    @Update("UPDATE admin_users SET last_login_at = #{lastLoginAt}, last_login_ip = #{lastLoginIp}, " +
            "login_count = login_count + 1, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateLoginInfo(@Param("id") Long id, 
                       @Param("lastLoginAt") LocalDateTime lastLoginAt, 
                       @Param("lastLoginIp") String lastLoginIp);

    /**
     * 删除管理员
     */
    @Delete("DELETE FROM admin_users WHERE id = #{id}")
    int deleteById(Long id);

    /**
     * 根据角色查询管理员
     */
    @Select("SELECT * FROM admin_users WHERE role = #{role} AND is_active = true")
    @Results({
        @Result(property = "userId", column = "user_id"),
        @Result(property = "lastLoginAt", column = "last_login_at"),
        @Result(property = "lastLoginIp", column = "last_login_ip"),
        @Result(property = "loginCount", column = "login_count"),
        @Result(property = "isActive", column = "is_active"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at"),
        @Result(property = "permissions", column = "permissions", 
                typeHandler = com.taxi.handler.JsonListTypeHandler.class),
        @Result(property = "user", column = "user_id", 
                one = @One(select = "com.taxi.mapper.UserMapper.selectById"))
    })
    List<AdminUser> selectByRole(String role);
}