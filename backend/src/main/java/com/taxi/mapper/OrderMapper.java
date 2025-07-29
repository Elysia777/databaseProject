 package com.taxi.mapper;

import com.taxi.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}