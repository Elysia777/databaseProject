package com.taxi.mapper;

import com.taxi.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 评价数据访问层
 */
@Mapper
public interface ReviewMapper {

    /** 根据ID查询评价 */
    Review selectById(Long id);

    /** 根据订单ID查询评价 */
    Review selectByOrderId(Long orderId);

    /** 根据乘客ID查询评价列表 */
    List<Review> selectByPassengerId(@Param("passengerId") Long passengerId, 
                                    @Param("offset") int offset, 
                                    @Param("size") int size);

    /** 根据司机ID查询评价列表 */
    List<Review> selectByDriverId(@Param("driverId") Long driverId, 
                                 @Param("offset") int offset, 
                                 @Param("size") int size);

    /** 插入评价 */
    int insert(Review review);

    /** 根据ID更新评价 */
    int updateById(Review review);

    /** 根据ID删除评价 */
    int deleteById(Long id);

    /** 统计司机的评价数据 */
    Map<String, Object> selectDriverRatingStats(Long driverId);

    /** 检查订单是否已评价 */
    int countByOrderId(Long orderId);

    /** 获取所有评价（带用户名称） */
    List<Map<String, Object>> selectAllWithNames();

    /** 检查用户ID是否存在 */
    boolean checkUserExists(@Param("userId") Long userId);

    /** 通过乘客ID获取用户ID */
    Long getPassengerUserIdByPassengerId(@Param("passengerId") Long passengerId);

    /** 通过司机ID获取用户ID */
    Long getDriverUserIdByDriverId(@Param("driverId") Long driverId);
}