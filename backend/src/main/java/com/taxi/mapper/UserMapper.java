 package com.taxi.mapper;

import com.taxi.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户Mapper接口
 */
@Mapper
public interface UserMapper {

    /** 根据ID查询用户 */
    User selectById(@Param("id") Long id);

    /** 根据用户名查询用户 */
    User selectByUsername(@Param("username") String username);

    /** 根据手机号查询用户 */
    User selectByPhone(@Param("phone") String phone);

    /** 根据邮箱查询用户 */
    User selectByEmail(@Param("email") String email);

    /** 插入用户 */
    int insert(User user);

    /** 更新用户 */
    int updateById(User user);

    /** 更新用户资料（只更新传递的字段） */
    int updateProfile(@Param("id") Long id, 
                     @Param("realName") String realName,
                     @Param("phone") String phone,
                     @Param("email") String email,
                     @Param("avatar") String avatar,
                     @Param("idCard") String idCard,
                     @Param("updatedAt") java.time.LocalDateTime updatedAt);

    /** 删除用户 */
    int deleteById(@Param("id") Long id);

    /** 查询所有用户 */
    List<User> selectAll();

    /** 根据用户类型查询用户 */
    List<User> selectByUserType(@Param("userType") String userType);
}