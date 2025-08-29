package com.taxi.service;

import com.taxi.entity.Order;
import com.taxi.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 预约单服务
 */
@Slf4j
@Service
public class ScheduledOrderService {

    @Autowired
    private OrderMapper orderMapper;
    
    @Autowired
    private OrderDispatchService orderDispatchService;
    
    @Autowired
    private WebSocketNotificationService webSocketNotificationService;
    
    @Autowired
    private DriverRedisService driverRedisService;
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    /**
     * 创建预约单
     */
    public Order createScheduledOrder(Order order) {
        log.info("创建预约单: passengerId={}, scheduledTime={}", 
                order.getPassengerId(), order.getScheduledTime());
        
        // 1. 检查乘客是否有进行中的订单
        if (hasActiveOrder(order.getPassengerId())) {
            throw new RuntimeException("您已有进行中的订单，请完成后再预约");
        }
        
        // 2. 验证预约时间
        validateScheduledTime(order.getScheduledTime());
        
        // 3. 设置订单属性
        order.setOrderType("RESERVATION");
        // 预约单初始状态为SCHEDULED，而不是PENDING
        order.setStatus("SCHEDULED");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        
        // 4. 保存订单
        orderMapper.insert(order);
        
        // 5. 安排定时激活任务
        scheduleOrderActivation(order);
        
        log.info("预约单创建成功: orderId={}, orderNumber={}", 
                order.getId(), order.getOrderNumber());
        
        return order;
    }
    
    /**
     * 检查乘客是否有进行中的订单
     */
    public boolean hasActiveOrder(Long passengerId) {
        Order activeOrder = orderMapper.selectCurrentOrderByPassengerId(passengerId);
        boolean hasActive = activeOrder != null;
        
        log.debug("检查乘客活跃订单: passengerId={}, hasActive={}", passengerId, hasActive);
        
        return hasActive;
    }
    
    /**
     * 验证预约时间
     */
    private void validateScheduledTime(LocalDateTime scheduledTime) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime minTime = now.plusMinutes(30); // 至少30分钟后
        
        if (scheduledTime.isBefore(minTime)) {
            throw new RuntimeException("预约时间至少需要30分钟后");
        }
        
        LocalDateTime maxTime = now.plusDays(7); // 最多7天后
        if (scheduledTime.isAfter(maxTime)) {
            throw new RuntimeException("预约时间不能超过7天");
        }
    }
    
    /**
     * 安排预约单激活任务
     */
    public void scheduleOrderActivation(Order scheduledOrder) {
        LocalDateTime activationTime = scheduledOrder.getScheduledTime().minusMinutes(59);
        LocalDateTime now = LocalDateTime.now();
        
        if (activationTime.isBefore(now)) {
            // 如果激活时间已过，立即激活
            log.warn("预约单激活时间已过，立即激活: orderId={}", scheduledOrder.getId());
            activateScheduledOrder(scheduledOrder.getId());
            return;
        }
        
        long delayMillis = java.time.Duration.between(now, activationTime).toMillis();
        
        scheduler.schedule(() -> {
            try {
                activateScheduledOrder(scheduledOrder.getId());
            } catch (Exception e) {
                log.error("激活预约单失败: orderId={}", scheduledOrder.getId(), e);
            }
        }, delayMillis, TimeUnit.MILLISECONDS);
        
        log.info("已安排预约单激活任务: orderId={}, activationTime={}, delayMillis={}", 
                scheduledOrder.getId(), activationTime, delayMillis);
    }
    
    /**
     * 激活预约单
     */
    public void activateScheduledOrder(Long orderId) {
        log.info("开始激活预约单: orderId={}", orderId);
        
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            log.error("预约单不存在: orderId={}", orderId);
            return;
        }
        
        if (!"SCHEDULED".equals(order.getStatus())) {
            log.warn("预约单状态不正确，无法激活: orderId={}, status={}", orderId, order.getStatus());
            return;
        }
        
        if (!"RESERVATION".equals(order.getOrderType())) {
            log.warn("订单类型不是预约单: orderId={}, orderType={}", orderId, order.getOrderType());
            return;
        }
        
        try {
            // 首先将预约单状态从SCHEDULED改为PENDING
            log.info("将预约单状态从SCHEDULED改为PENDING: orderId={}", orderId);
            order.setStatus("PENDING");
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(order);
            
            log.info("✅ 预约单状态已更新为PENDING: orderId={}", orderId);
            
            // 通知乘客预约单已激活
            try {
                webSocketNotificationService.notifyPassengerOrderStatusChange(
                    order.getPassengerId(), 
                    orderId, 
                    "PENDING", 
                    "预约时间已到，正在为您寻找司机..."
                );
                log.info("✅ 已通知乘客预约单激活: passengerId={}, orderId={}", order.getPassengerId(), orderId);
            } catch (Exception e) {
                log.error("❌ 通知乘客预约单激活失败: passengerId={}, orderId={}", order.getPassengerId(), orderId, e);
            }
            
            // 开始分配司机
            log.info("开始为预约单分配司机: orderId={}", orderId);
            orderDispatchService.dispatchOrder(orderId);
            
            // 设置超时取消任务（30分钟后）
            scheduleOrderTimeout(orderId);
            
            log.info("✅ 预约单激活成功: orderId={}", orderId);
            
        } catch (Exception e) {
            log.error("❌ 激活预约单失败: orderId={}", orderId, e);
            
            // 激活失败，取消订单
            cancelScheduledOrder(orderId, "系统激活失败");
        }
    }
    
    /**
     * 设置预约单超时取消
     */
    private void scheduleOrderTimeout(Long orderId) {
        scheduler.schedule(() -> {
            try {
                Order order = orderMapper.selectById(orderId);
                if (order != null && "PENDING".equals(order.getStatus())) {
                    log.info("预约单超时，自动取消: orderId={}", orderId);
                    cancelScheduledOrder(orderId, "超时无司机接单");
                    
                    // 通知乘客
                    // TODO: 实现预约单超时通知
                    log.info("预约单超时通知: passengerId={}, orderId={}", order.getPassengerId(), orderId);
                }
            } catch (Exception e) {
                log.error("处理预约单超时失败: orderId={}", orderId, e);
            }
        }, 30, TimeUnit.MINUTES);
    }
    
    /**
     * 取消预约单
     */
    public void cancelScheduledOrder(Long orderId, String reason) {
        log.info("取消预约单: orderId={}, reason={}", orderId, reason);
        
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            log.error("预约单不存在: orderId={}", orderId);
            return;
        }
        
        // 记录原始状态和司机ID，用于通知
        String originalStatus = order.getStatus();
        Long driverId = order.getDriverId();
        
        order.setStatus("CANCELLED");
        order.setCancelReason(reason);
        order.setUpdatedAt(LocalDateTime.now());
        
        orderMapper.updateById(order);
        
        log.info("预约单取消成功: orderId={}, originalStatus={}, driverId={}", 
                orderId, originalStatus, driverId);
        
        // 如果订单已分配给司机，需要释放司机并通知司机
        if (driverId != null && ("ASSIGNED".equals(originalStatus) || "PICKUP".equals(originalStatus))) {
            log.info("预约单已分配给司机，释放司机状态并发送取消通知: driverId={}, orderId={}", driverId, orderId);
            
            try {
                // 🔧 关键修复：释放司机状态，让司机可以接收新订单
                driverRedisService.markDriverFree(driverId);
                log.info("✅ 已释放司机状态: driverId={}", driverId);
            } catch (Exception e) {
                log.error("❌ 释放司机状态失败: driverId={}", driverId, e);
            }
            
            try {
                webSocketNotificationService.notifyDriverOrderCancelled(driverId, orderId, reason);
                log.info("✅ 已通知司机预约单取消: driverId={}, orderId={}", driverId, orderId);
            } catch (Exception e) {
                log.error("❌ 通知司机预约单取消失败: driverId={}, orderId={}", driverId, orderId, e);
            }
        } else {
            log.info("预约单未分配给司机或状态不需要通知: driverId={}, status={}", driverId, originalStatus);
        }
        
        // 通知乘客预约单已取消
        try {
            webSocketNotificationService.notifyPassengerOrderStatusChange(
                order.getPassengerId(), 
                orderId, 
                "CANCELLED", 
                "预约单已取消：" + reason
            );
            log.info("✅ 已通知乘客预约单取消: passengerId={}, orderId={}", order.getPassengerId(), orderId);
        } catch (Exception e) {
            log.error("❌ 通知乘客预约单取消失败: passengerId={}, orderId={}", order.getPassengerId(), orderId, e);
        }
    }
    
    /**
     * 获取乘客的预约单列表
     */
    public List<Order> getPassengerScheduledOrders(Long passengerId) {
        return orderMapper.selectScheduledOrdersByPassengerId(passengerId);
    }
    
    /**
     * 获取司机可接的预约单列表
     */
    public List<Order> getAvailableScheduledOrdersForDriver(Long driverId) {
        // 只返回已激活的预约单（状态为PENDING的预约单）
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime maxTime = now.plusHours(2);
        
        // 查询已激活的预约单（状态为PENDING且预约时间在2小时内）
        return orderMapper.selectActivatedScheduledOrders(now, maxTime);
    }
    
    /**
     * 系统启动时恢复预约单任务
     */
    public void recoverScheduledOrders() {
        log.info("开始恢复预约单定时任务...");
        
        List<Order> pendingOrders = orderMapper.selectPendingScheduledOrders();
        
        for (Order order : pendingOrders) {
            LocalDateTime scheduledTime = order.getScheduledTime();
            LocalDateTime now = LocalDateTime.now();
            
            if (scheduledTime.isBefore(now.plusMinutes(20))) {
                // 预约时间已到或即将到达，立即激活
                log.info("恢复时发现预约单需要立即激活: orderId={}", order.getId());
                activateScheduledOrder(order.getId());
            } else {
                // 重新安排激活任务
                scheduleOrderActivation(order);
            }
        }
        
        log.info("预约单定时任务恢复完成，共恢复 {} 个订单", pendingOrders.size());
    }
}