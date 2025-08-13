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

    /** 查询司机的历史订单（分页） */
    List<Order> selectDriverOrders(@Param("driverId") Long driverId,
                                   @Param("offset") int offset,
                                   @Param("size") int size);

    /** 按状态查询司机的订单（分页） */
    List<Order> selectDriverOrdersByStatus(@Param("driverId") Long driverId,
                                           @Param("status") String status,
                                           @Param("offset") int offset,
                                           @Param("size") int size);

    /** 按日期范围查询司机的订单（分页） */
    List<Order> selectDriverOrdersByDateRange(@Param("driverId") Long driverId,
                                              @Param("startDate") String startDate,
                                              @Param("endDate") String endDate,
                                              @Param("offset") int offset,
                                              @Param("size") int size);

    /** 按状态和日期范围查询司机的订单（分页） */
    List<Order> selectDriverOrdersByStatusAndDateRange(@Param("driverId") Long driverId,
                                                       @Param("status") String status,
                                                       @Param("startDate") String startDate,
                                                       @Param("endDate") String endDate,
                                                       @Param("offset") int offset,
                                                       @Param("size") int size);

    /** 统计司机订单数量 */
    int countDriverOrders(@Param("driverId") Long driverId, @Param("status") String status);

    /** 查询订单详情（包含司机车辆信息） */
    java.util.Map<String, Object> selectOrderWithDriverVehicle(@Param("orderId") Long orderId);

    /** 查询所有订单（包含乘客和司机姓名） */
    List<java.util.Map<String, Object>> selectAllWithUserNames();

    /** 获取司机月度收入汇总 */
    java.util.Map<String, Object> selectDriverMonthlySummary(@Param("driverId") Long driverId,
                                                             @Param("month") String month);

    /** 获取司机收入汇总统计 */
    java.util.Map<String, Object> selectDriverEarningsSummary(@Param("driverId") Long driverId,
                                                              @Param("month") String month);

    /** 获取司机每日收入记录 */
    List<java.util.Map<String, Object>> selectDriverDailyEarnings(@Param("driverId") Long driverId,
                                                                  @Param("month") String month,
                                                                  @Param("offset") int offset,
                                                                  @Param("size") int size);

    /** 统计司机每日收入记录数量 */
    int countDriverDailyEarnings(@Param("driverId") Long driverId, @Param("month") String month);

    /** 获取司机每日收入图表数据 */
    List<java.util.Map<String, Object>> selectDriverDailyEarningsChart(@Param("driverId") Long driverId,
                                                                       @Param("month") String month);

    /** 获取司机每周收入图表数据 */
    List<java.util.Map<String, Object>> selectDriverWeeklyEarnings(@Param("driverId") Long driverId,
                                                                   @Param("month") String month);
}