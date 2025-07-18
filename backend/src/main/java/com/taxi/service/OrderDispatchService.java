package com.taxi.service;

import com.taxi.entity.Order;
import com.taxi.entity.Driver;
import com.taxi.mapper.OrderMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Redis + RabbitMQ 混合订单分配服务
 * 1. 订单进入队列等待
 * 2. 从Redis GEO查找附近司机
 * 3. 通过RabbitMQ异步通知司机
 * 4. 使用Redis分布式锁防止重复接单
 */
@Service
public class OrderDispatchService {

    @Autowired
    private OrderMapper orderMapper;
    
    @Autowired
    private DriverRedisService driverRedisService;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    private WebSocketNotificationService webSocketNotificationService;
    
    @Autowired
    private OrderPushSDK orderPushSDK;
    
    @Autowired
    private PendingOrderService pendingOrderService;

    // 搜索半径（公里）
    private static final double SEARCH_RADIUS_KM = 5.0;
    
    // 最大通知司机数量
    private static final int MAX_NOTIFY_DRIVERS = 5;
    
    // 订单锁过期时间（秒）
    private static final int ORDER_LOCK_EXPIRE_SECONDS = 30;

    /**
     * 为新订单寻找并通知附近的司机
     */
    public void dispatchOrder(Long orderId) {
        System.out.println("=== 开始分配订单 " + orderId + " ===");
        
        try {
            // 1. 查询订单信息
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                System.out.println("订单不存在: " + orderId);
                return;
            }
            
            if (!"PENDING".equals(order.getStatus())) {
                System.out.println("订单状态不是PENDING: " + order.getStatus());
                return;
            }
            
            // 2. 检查订单位置信息
            if (order.getPickupLatitude() == null || order.getPickupLongitude() == null) {
                System.out.println("订单缺少位置信息");
                return;
            }
            
            // 3. 将订单加入持久化待分配队列（确保后续上线的司机也能看到）
            pendingOrderService.addPendingOrder(orderId);
            
            // 4. 从Redis查找附近的在线司机
            List<Driver> nearbyDrivers = driverRedisService.getNearbyOnlineDrivers(
                order.getPickupLatitude(), 
                order.getPickupLongitude(), 
                SEARCH_RADIUS_KM
            );
            
            if (nearbyDrivers.isEmpty()) {
                System.out.println("没有找到附近的在线司机，订单已加入待分配队列");
                handleNoDriversAvailable(order);
                return;
            }
            
            System.out.println("找到 " + nearbyDrivers.size() + " 个附近司机");
            
            // 5. 通知当前在线的司机
            int notifyCount = Math.min(nearbyDrivers.size(), MAX_NOTIFY_DRIVERS);
            for (int i = 0; i < notifyCount; i++) {
                Driver driver = nearbyDrivers.get(i);
                notifyDriver(driver, order);
            }
            
            System.out.println("已通知 " + notifyCount + " 个司机新订单");
            
        } catch (Exception e) {
            System.err.println("分配订单失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 司机接单（使用分布式锁防止重复接单）
     */
    public boolean acceptOrder(Long orderId, Long driverId) {
        System.out.println("司机 " + driverId + " 尝试接单 " + orderId);
        
        // 1. 尝试获取订单分布式锁
        if (!driverRedisService.tryLockOrder(orderId, driverId, ORDER_LOCK_EXPIRE_SECONDS)) {
            System.out.println("司机 " + driverId + " 获取订单 " + orderId + " 锁失败，可能已被其他司机接单");
            return false;
        }
        
        try {
            // 2. 检查司机状态
            if (driverRedisService.isDriverBusy(driverId)) {
                System.out.println("司机 " + driverId + " 正在忙碌中");
                return false;
            }
            
            // 3. 查询订单
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                System.out.println("订单不存在: " + orderId);
                return false;
            }
            
            // 4. 再次检查订单状态（双重检查）
            if (!"PENDING".equals(order.getStatus())) {
                System.out.println("订单已被其他司机接单，当前状态: " + order.getStatus());
                return false;
            }
            
            // 5. 分配订单
            order.setDriverId(driverId);
            order.setStatus("ASSIGNED");
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(order);
            
            // 6. 标记司机为忙碌状态
            driverRedisService.markDriverBusy(driverId);
            
            System.out.println("订单接单成功 - 订单: " + order.getOrderNumber() + ", 司机: " + driverId);
            
            // 7. 通知乘客订单已被接受
            notifyPassengerOrderAccepted(order);
            
            // 8. 发送接单成功消息到队列（用于其他业务处理）
            sendOrderAssignedMessage(order, driverId);
            
            return true;
            
        } catch (Exception e) {
            System.err.println("接单失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            // 9. 释放分布式锁
            driverRedisService.releaseLockOrder(orderId, driverId);
        }
    }

    /**
     * 司机完成订单，重新变为空闲状态
     */
    public void completeOrder(Long orderId, Long driverId) {
        try {
            // 1. 更新订单状态
            Order order = orderMapper.selectById(orderId);
            if (order != null) {
                order.setStatus("COMPLETED");
                order.setCompletionTime(LocalDateTime.now());
                order.setUpdatedAt(LocalDateTime.now());
                orderMapper.updateById(order);
            }
            
            // 2. 标记司机为空闲状态
            driverRedisService.markDriverFree(driverId);
            
            System.out.println("订单完成 - 订单: " + orderId + ", 司机: " + driverId + " 重新空闲");
            
        } catch (Exception e) {
            System.err.println("完成订单失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 通知司机有新订单（混合通知：RabbitMQ + WebSocket）
     */
    private void notifyDriver(Driver driver, Order order) {
        try {
            // 计算距离
            double distance = calculateDistance(
                order.getPickupLatitude().doubleValue(),
                order.getPickupLongitude().doubleValue(),
                driver.getCurrentLatitude().doubleValue(),
                driver.getCurrentLongitude().doubleValue()
            );
            
            // 1. 通过WebSocket实时通知司机
            webSocketNotificationService.notifyDriverNewOrder(driver.getId(), order, distance);
            
            // 2. 构建RabbitMQ消息（作为备用通知机制）
            Map<String, Object> rabbitMessage = new HashMap<>();
            rabbitMessage.put("driverId", driver.getId());
            rabbitMessage.put("orderId", order.getId());
            rabbitMessage.put("orderNumber", order.getOrderNumber());
            rabbitMessage.put("pickupAddress", order.getPickupAddress());
            rabbitMessage.put("destinationAddress", order.getDestinationAddress());
            rabbitMessage.put("distance", distance);
            rabbitMessage.put("estimatedFare", order.getEstimatedFare());
            rabbitMessage.put("timestamp", System.currentTimeMillis());
            
            // 发送到司机通知队列
            rabbitTemplate.convertAndSend("driver_notification_queue", rabbitMessage);
            
            System.out.println("已通过WebSocket和RabbitMQ通知司机 " + driver.getId() + " 新订单 " + order.getId());
            
        } catch (Exception e) {
            System.err.println("通知司机失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 处理没有司机可用的情况
     */
    private void handleNoDriversAvailable(Order order) {
        System.out.println("订单 " + order.getId() + " 暂时没有可用司机");
        
        // 可以实现以下策略：
        // 1. 扩大搜索半径
        // 2. 延迟重试
        // 3. 通知乘客等待
        // 4. 记录到待分配队列
        
        // 这里简单记录日志，实际可以根据业务需求实现
    }

    /**
     * 通知乘客订单已被接受
     */
    private void notifyPassengerOrderAccepted(Order order) {
        try {
            String message = String.format(
                "您的订单 %s 已被司机接受，司机正在前往上车地点",
                order.getOrderNumber()
            );
            
            // 通过WebSocket实时通知乘客
            webSocketNotificationService.notifyPassengerOrderUpdate(
                order.getPassengerId(), 
                order, 
                message
            );
            
            System.out.println("已通过WebSocket通知乘客订单被接受");
            
        } catch (Exception e) {
            System.err.println("通知乘客失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 发送接单成功消息到队列
     */
    private void sendOrderAssignedMessage(Order order, Long driverId) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("orderId", order.getId());
            message.put("orderNumber", order.getOrderNumber());
            message.put("driverId", driverId);
            message.put("status", "ASSIGNED");
            message.put("timestamp", System.currentTimeMillis());
            
            rabbitTemplate.convertAndSend("order_status_queue", message);
            System.out.println("已发送订单分配消息到队列");
            
        } catch (Exception e) {
            System.err.println("发送订单分配消息失败: " + e.getMessage());
        }
    }

    /**
     * 处理司机上线时的待分配订单
     * 当司机上线时，检查是否有等待的订单需要分配
     */
    public void handleDriverOnline(Long driverId) {
        System.out.println("=== 司机 " + driverId + " 上线，检查待分配订单 ===");
        
        try {
            // 获取司机信息
            Driver driver = driverRedisService.getDriverInfo(driverId);
            if (driver == null || driver.getCurrentLatitude() == null || driver.getCurrentLongitude() == null) {
                System.out.println("司机信息不完整，跳过待分配订单检查");
                return;
            }
            
            // 查询所有PENDING状态的订单
            List<Order> pendingOrders = orderMapper.selectByStatus("PENDING");
            if (pendingOrders == null || pendingOrders.isEmpty()) {
                System.out.println("没有待分配的订单");
                return;
            }
            
            System.out.println("找到 " + pendingOrders.size() + " 个待分配订单");
            
            // 为每个订单检查是否在司机服务范围内
            for (Order order : pendingOrders) {
                if (order.getPickupLatitude() == null || order.getPickupLongitude() == null) {
                    continue;
                }
                
                // 计算距离
                double distance = calculateDistance(
                    order.getPickupLatitude().doubleValue(),
                    order.getPickupLongitude().doubleValue(),
                    driver.getCurrentLatitude().doubleValue(),
                    driver.getCurrentLongitude().doubleValue()
                );
                
                // 如果在服务范围内，通知司机
                if (distance <= SEARCH_RADIUS_KM * 1000) {
                    notifyDriver(driver, order);
                    System.out.println("已通知司机 " + driverId + " 待分配订单 " + order.getId());
                }
            }
            
        } catch (Exception e) {
            System.err.println("处理司机上线待分配订单失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 扩大搜索范围重新分配订单
     */
    public void retryDispatchWithLargerRadius(Long orderId) {
        System.out.println("=== 扩大搜索范围重新分配订单 " + orderId + " ===");
        
        try {
            Order order = orderMapper.selectById(orderId);
            if (order == null || !"PENDING".equals(order.getStatus())) {
                return;
            }
            
            // 使用更大的搜索半径
            double largerRadius = SEARCH_RADIUS_KM * 2; // 扩大到10公里
            
            List<Driver> nearbyDrivers = driverRedisService.getNearbyOnlineDrivers(
                order.getPickupLatitude(), 
                order.getPickupLongitude(), 
                largerRadius
            );
            
            if (!nearbyDrivers.isEmpty()) {
                System.out.println("扩大搜索范围后找到 " + nearbyDrivers.size() + " 个司机");
                
                // 通知司机
                int notifyCount = Math.min(nearbyDrivers.size(), MAX_NOTIFY_DRIVERS);
                for (int i = 0; i < notifyCount; i++) {
                    Driver driver = nearbyDrivers.get(i);
                    notifyDriver(driver, order);
                }
            } else {
                System.out.println("扩大搜索范围后仍未找到可用司机");
                // 可以进一步处理，比如通知乘客暂时没有司机
            }
            
        } catch (Exception e) {
            System.err.println("扩大搜索范围重新分配失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 计算两点间距离（米）
     */
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);
        double a = radLat1 - radLat2;
        double b = Math.toRadians(lng1) - Math.toRadians(lng2);
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a/2), 2) +
                Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b/2), 2)));
        return s * 6378137.0; // 地球半径
    }
}