 package com.taxi.mapper;

import com.taxi.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单Mapper接口
 */
@Mapper
public interface OrderMapper {

    /** 根据ID查询订单 */
    Order selectById(@Param("id") Long id);

    /** 根据订单号查询订单 */
    Order selectByOrderNumber(@Param("orderNumber") String orderNumber);

    /** 根据乘客ID查询订单 */
    List<Order> selectByPassengerId(@Param("passengerId") Long passengerId);

    /** 根据司机ID查询订单 */
    List<Order> selectByDriverId(@Param("driverId") Long driverId);

    /** 插入订单 */
    int insert(Order order);

    /** 更新订单 */
    int updateById(Order order);

    /** 删除订单 */
    int deleteById(@Param("id") Long id);

    /** 查询所有订单 */
    List<Order> selectAll();

    /** 根据状态查询订单 */
    List<Order> selectByStatus(@Param("status") String status);

    /** 查询司机当前进行中的订单 */
    Order selectCurrentOrderByDriverId(@Param("driverId") Long driverId);
    
    /** 查询乘客当前进行中的订单 */
    Order selectCurrentOrderByPassengerId(@Param("passengerId") Long passengerId);
    
    /** 查询待激活的预约单 */
    List<Order> selectPendingScheduledOrders();
    
    /** 查询乘客的预约单列表 */
    List<Order> selectScheduledOrdersByPassengerId(@Param("passengerId") Long passengerId);
    
    /** 查询可用的预约单（司机端） */
    List<Order> selectAvailableScheduledOrders(@Param("startTime") LocalDateTime startTime, 
                                              @Param("endTime") LocalDateTime endTime);
    
    /** 查询已激活的预约单（司机端） */
    List<Order> selectActivatedScheduledOrders(@Param("startTime") LocalDateTime startTime, 
                                              @Param("endTime") LocalDateTime endTime);
}