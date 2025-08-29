package com.taxi.service;

import com.taxi.entity.Order;
import com.taxi.entity.Driver;
import com.taxi.entity.User;
import com.taxi.mapper.OrderMapper;
import com.taxi.mapper.DriverMapper;
import com.taxi.mapper.UserMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.Comparator;

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
    private DriverMapper driverMapper;
    
    @Autowired
    private UserMapper userMapper;
    
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
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private org.springframework.scheduling.TaskScheduler taskScheduler;

    // 搜索半径（公里）
    private static final double SEARCH_RADIUS_KM = 5.0;
    
    // 最大通知司机数量
    private static final int MAX_NOTIFY_DRIVERS = 5;
    
    // 订单锁过期时间（秒）
    private static final int ORDER_LOCK_EXPIRE_SECONDS = 30;
    
    // Redis key前缀
    private static final String ORDER_NOTIFIED_DRIVERS_KEY = "order_notified_drivers:"; // 订单已通知的司机列表
    private static final String DRIVER_REJECT_COUNT_KEY = "driver_reject_count:"; // 司机拒单次数
    private static final String ORDER_RETRY_INFO_KEY = "order_retry_info:"; // 订单重试信息
    
    // 重试配置
    private static final int[] RETRY_INTERVALS = {30, 60, 120, 300}; // 重试间隔：30秒、1分钟、2分钟、5分钟
    private static final double[] RETRY_RADIUS_MULTIPLIERS = {1.0, 1.5, 2.0, 3.0}; // 搜索半径倍数
    private static final int[] RETRY_MAX_DRIVERS = {3, 5, 8, 10}; // 每轮最大通知司机数

    // 分层延迟（秒）
    private static final int DELAY_HIGH_TIER_SECONDS = 0;
    private static final int DELAY_MID_TIER_SECONDS = 5;
    private static final int DELAY_LOW_TIER_SECONDS = 10;

    // 订单 -> 已调度的定时任务列表（用于接单后立即取消未触达的通知）
    private final Map<Long, List<ScheduledFuture<?>>> orderScheduledTasks = new ConcurrentHashMap<>();

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
            
            if (!"PENDING".equals(order.getStatus())&&!"SCHEDULED".equals(order.getStatus())) {
                System.out.println("订单状态不是PENDING或SHEDULED: " + order.getStatus());
                return;
            }
            
            // 2. 检查订单位置信息
            if (order.getPickupLatitude() == null || order.getPickupLongitude() == null) {
                System.out.println("订单缺少位置信息");
                return;
            }
            
            // 3. 将订单加入持久化待分配队列（确保后续上线的司机也能看到）
            pendingOrderService.addPendingOrder(orderId);
            
            // 4. 初始化订单重试信息
            initOrderRetryInfo(orderId);
            
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
            
            // 5. 按评分分层并分批延迟推送
            layeredNotifyDriversByRating(order, nearbyDrivers);
            
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
            if (!"PENDING".equals(order.getStatus())&&!"SCHEDULED".equals(order.getStatus())) {
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
            
            // 7. 通知乘客订单已被接受（包含司机详细信息）
            Driver driver = driverMapper.selectById(driverId);
            if (driver != null) {
                // 获取司机的用户信息
                User driverUser = userMapper.selectById(driver.getUserId());
                webSocketNotificationService.notifyPassengerOrderAssigned(order.getPassengerId(), order, driver, driverUser);
                
                // 设置司机当前执行的订单
                driverRedisService.setDriverCurrentOrder(driverId, order.getId());
            } else {
                // 备用通知方式
                notifyPassengerOrderAccepted(order);
            }
            
            // 8. 发送接单成功消息到队列（用于其他业务处理）
            sendOrderAssignedMessage(order, driverId);

            // 9. 取消尚未执行的延迟推送任务，并清理去重集合（可选保留一段TTL）
            cancelScheduledTasks(orderId);
            
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
     * 取消该订单下所有已调度但未执行的通知任务
     */
    private void cancelScheduledTasks(Long orderId) {
        try {
            List<ScheduledFuture<?>> futures = orderScheduledTasks.remove(orderId);
            if (futures != null) {
                for (ScheduledFuture<?> f : futures) {
                    try { if (f != null) f.cancel(false); } catch (Exception ignore) {}
                }
            }
        } catch (Exception e) {
            System.err.println("取消延迟通知任务失败: " + e.getMessage());
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
            // 统一去重与校验的安全通知
            notifyDriverSafely(order, driver);
            
        } catch (Exception e) {
            System.err.println("通知司机失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 统一原子去重的安全通知入口（供分层推送与扩大范围复用）
     */
    private void notifyDriverSafely(Order order, Driver driver) {
        try {
            // 订单必须仍待分配
            if (!"PENDING".equals(order.getStatus()) && !"SCHEDULED".equals(order.getStatus())) {
                return;
            }

            // 黑名单过滤
            if (driverRedisService.isDriverInOrderBlacklist(order.getId(), driver.getId())) {
                return;
            }

            // 原子去重：SADD 返回 1 才是首次
            String key = ORDER_NOTIFIED_DRIVERS_KEY + order.getId();
            Long added = redisTemplate.opsForSet().add(key, driver.getId().toString());
            if (added == null || added == 0L) {
                return;
            }
            // 设置过期时间（保障内存与去重）
            redisTemplate.expire(key, 2, java.util.concurrent.TimeUnit.HOURS);

            // 计算距离
            double distance = calculateDistance(
                order.getPickupLatitude().doubleValue(),
                order.getPickupLongitude().doubleValue(),
                driver.getCurrentLatitude().doubleValue(),
                driver.getCurrentLongitude().doubleValue()
            );

            // WebSocket 推送
            webSocketNotificationService.notifyDriverNewOrder(driver.getId(), order, distance);

            // 备选：发送MQ
            Map<String, Object> rabbitMessage = new HashMap<>();
            rabbitMessage.put("driverId", driver.getId());
            rabbitMessage.put("orderId", order.getId());
            rabbitMessage.put("orderNumber", order.getOrderNumber());
            rabbitMessage.put("orderType", order.getOrderType());
            rabbitMessage.put("pickupAddress", order.getPickupAddress());
            rabbitMessage.put("destinationAddress", order.getDestinationAddress());
            rabbitMessage.put("distance", distance);
            rabbitMessage.put("estimatedFare", order.getEstimatedFare());
            rabbitMessage.put("scheduledTime", order.getScheduledTime());
            rabbitMessage.put("timestamp", System.currentTimeMillis());
            rabbitTemplate.convertAndSend("driver_notification_queue", rabbitMessage);

            System.out.println("✅ 安全通知：司机 " + driver.getId() + " 订单 " + order.getId());
        } catch (Exception ex) {
            System.err.println("安全通知失败: " + ex.getMessage());
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
                    // 使用安全入口，避免重复
                    notifyDriverSafely(order, driver);
                    System.out.println("已(安全)通知司机 " + driverId + " 待分配订单 " + order.getId());
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
                
                // 过滤掉已通知过的司机
                List<Driver> availableDrivers = new java.util.ArrayList<>();
                for (Driver driver : nearbyDrivers) {
                    if (!hasNotifiedDriver(orderId, driver.getId())) {
                        availableDrivers.add(driver);
                    }
                }

                // 统一通过分层延迟与安全通知入口
                layeredNotifyDriversByRating(order, availableDrivers);
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
     * 检查是否已经通知过某个司机
     */
    private boolean hasNotifiedDriver(Long orderId, Long driverId) {
        try {
            String key = ORDER_NOTIFIED_DRIVERS_KEY + orderId;
            return redisTemplate.opsForSet().isMember(key, driverId.toString());
        } catch (Exception e) {
            System.err.println("检查司机通知记录失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 按评分分层并分批（延迟）通知司机
     */
    private void layeredNotifyDriversByRating(Order order, List<Driver> candidateDrivers) {
        if (candidateDrivers == null || candidateDrivers.isEmpty()) return;

        // 限制本轮最大通知人数
        int limit = Math.min(candidateDrivers.size(), MAX_NOTIFY_DRIVERS);
        List<Driver> drivers = new ArrayList<>(candidateDrivers.subList(0, limit));

        // 仅按评分字段排序（降序）
        drivers.sort(Comparator.comparingDouble(d -> -getDriverRatingValue(d)));

        // 分层（简单三层切分：前30% 高层，中间40% 中层，后30% 低层）
        int n = drivers.size();
        int highEnd = Math.max(1, (int)Math.ceil(n * 0.3));
        int midEnd = Math.max(highEnd + 1, (int)Math.ceil(n * 0.7));

        List<Driver> highTier = drivers.subList(0, highEnd);
        List<Driver> midTier = drivers.subList(highEnd, Math.min(midEnd, n));
        List<Driver> lowTier = drivers.subList(Math.min(midEnd, n), n);

        scheduleTier(order, highTier, DELAY_HIGH_TIER_SECONDS);
        scheduleTier(order, midTier, DELAY_MID_TIER_SECONDS);
        scheduleTier(order, lowTier, DELAY_LOW_TIER_SECONDS);
    }

    private void scheduleTier(Order order, List<Driver> tier, int delaySeconds) {
        if (tier == null || tier.isEmpty()) return;
        List<ScheduledFuture<?>> futures = orderScheduledTasks.computeIfAbsent(order.getId(), k -> new ArrayList<>());
        for (Driver d : tier) {
            ScheduledFuture<?> f = taskScheduler.schedule(() -> {
                try {
                    // 订单仍需派发才推送
                    Order latest = orderMapper.selectById(order.getId());
                    if (latest != null && ("PENDING".equals(latest.getStatus()) || "SCHEDULED".equals(latest.getStatus()))) {
                        notifyDriverSafely(latest, d);
                    }
                } catch (Exception ex) {
                    System.err.println("分层延迟通知失败: " + ex.getMessage());
                }
            }, new java.util.Date(System.currentTimeMillis() + delaySeconds * 1000L));
            futures.add(f);
        }
    }

    private double getDriverRatingValue(Driver d) {
        return d.getRating() != null ? d.getRating().doubleValue() : 0.0;
    }

    /**
     * 记录已通知的司机
     */
    private void recordDriverNotification(Long orderId, Long driverId) {
        try {
            String key = ORDER_NOTIFIED_DRIVERS_KEY + orderId;
            redisTemplate.opsForSet().add(key, driverId.toString());
            // 设置过期时间为2小时
            redisTemplate.expire(key, 2, java.util.concurrent.TimeUnit.HOURS);
        } catch (Exception e) {
            System.err.println("记录司机通知失败: " + e.getMessage());
        }
    }

    /**
     * 司机拒单处理
     */
    public void handleDriverRejectOrder(Long orderId, Long driverId, String reason) {
        System.out.println("司机 " + driverId + " 拒绝订单 " + orderId + ", 原因: " + reason);
        
        try {
            // 1. 记录司机拒单次数
            String rejectKey = DRIVER_REJECT_COUNT_KEY + driverId;
            redisTemplate.opsForValue().increment(rejectKey);
            redisTemplate.expire(rejectKey, 24, java.util.concurrent.TimeUnit.HOURS);
            
            // 2. 检查订单状态
            Order order = orderMapper.selectById(orderId);
            if (order == null || !"PENDING".equals(order.getStatus())) {
                System.out.println("订单状态已变更，无需重新分配");
                return;
            }
            
            // 3. 重新分配给其他司机
            redistributeOrder(orderId);
            
        } catch (Exception e) {
            System.err.println("处理司机拒单失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 重新分配订单给其他司机
     */
    private void redistributeOrder(Long orderId) {
        System.out.println("=== 重新分配订单 " + orderId + " ===");
        
        try {
            Order order = orderMapper.selectById(orderId);
            if (order == null || !"PENDING".equals(order.getStatus())) {
                return;
            }
            
            // 1. 获取附近的在线司机
            List<Driver> nearbyDrivers = driverRedisService.getNearbyOnlineDrivers(
                order.getPickupLatitude(), 
                order.getPickupLongitude(), 
                SEARCH_RADIUS_KM
            );
            
            if (nearbyDrivers.isEmpty()) {
                System.out.println("没有找到附近的司机，尝试扩大搜索范围");
                retryDispatchWithLargerRadius(orderId);
                return;
            }
            
            // 2. 过滤掉已经通知过的司机
            List<Driver> availableDrivers = new java.util.ArrayList<>();
            for (Driver driver : nearbyDrivers) {
                if (!hasNotifiedDriver(orderId, driver.getId())) {
                    availableDrivers.add(driver);
                }
            }
            
            if (availableDrivers.isEmpty()) {
                System.out.println("所有附近司机都已通知过，尝试扩大搜索范围");
                retryDispatchWithLargerRadius(orderId);
                return;
            }
            
            // 3. 分层延迟通知新的司机
            layeredNotifyDriversByRating(order, availableDrivers);
            
            System.out.println("重新分配订单 " + orderId + "，按分层策略安排了通知");
            
        } catch (Exception e) {
            System.err.println("重新分配订单失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 清理订单通知记录（订单完成或取消时调用）
     */
    public void cleanOrderNotificationRecord(Long orderId) {
        try {
            String key = ORDER_NOTIFIED_DRIVERS_KEY + orderId;
            redisTemplate.delete(key);
            System.out.println("已清理订单 " + orderId + " 的通知记录");
        } catch (Exception e) {
            System.err.println("清理订单通知记录失败: " + e.getMessage());
        }
    }

    /**
     * 获取司机拒单次数
     */
    public int getDriverRejectCount(Long driverId) {
        try {
            String key = DRIVER_REJECT_COUNT_KEY + driverId;
            Object count = redisTemplate.opsForValue().get(key);
            return count != null ? Integer.parseInt(count.toString()) : 0;
        } catch (Exception e) {
            System.err.println("获取司机拒单次数失败: " + e.getMessage());
            return 0;
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

    // ==================== 订单重试机制 ====================

    /**
     * 初始化订单重试信息
     */
    private void initOrderRetryInfo(Long orderId) {
        try {
            String key = ORDER_RETRY_INFO_KEY + orderId;
            Map<String, Object> retryInfo = new HashMap<>();
            retryInfo.put("orderId", orderId);
            retryInfo.put("retryCount", 0);
            retryInfo.put("createTime", System.currentTimeMillis());
            retryInfo.put("lastRetryTime", System.currentTimeMillis());
            
            redisTemplate.opsForHash().putAll(key, retryInfo);
            redisTemplate.expire(key, 2, java.util.concurrent.TimeUnit.HOURS);
            
            // 安排第一次重试
            scheduleOrderRetry(orderId, 0);
            
            System.out.println("订单 " + orderId + " 重试机制已初始化");
            
        } catch (Exception e) {
            System.err.println("初始化订单重试信息失败: " + e.getMessage());
        }
    }

    /**
     * 安排订单重试
     */
    private void scheduleOrderRetry(Long orderId, int retryRound) {
        if (retryRound >= RETRY_INTERVALS.length) {
            System.out.println("订单 " + orderId + " 已达到最大重试次数，停止重试");
            return;
        }
        
        try {
            int delaySeconds = RETRY_INTERVALS[retryRound];
            
            // 使用TaskScheduler安排延迟任务
            taskScheduler.schedule(() -> {
                executeOrderRetry(orderId, retryRound);
            }, new java.util.Date(System.currentTimeMillis() + delaySeconds * 1000L));
            
            System.out.println("订单 " + orderId + " 已安排第 " + (retryRound + 1) + " 轮重试，" + delaySeconds + " 秒后执行");
            
        } catch (Exception e) {
            System.err.println("安排订单重试失败: " + e.getMessage());
        }
    }

    /**
     * 执行订单重试
     */
    private void executeOrderRetry(Long orderId, int retryRound) {
        System.out.println("=== 执行订单 " + orderId + " 第 " + (retryRound + 1) + " 轮重试 ===");
        
        try {
            // 1. 检查订单状态
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                System.out.println("订单不存在，停止重试");
                return;
            }
            
            if (!"PENDING".equals(order.getStatus())) {
                System.out.println("订单状态已变更为 " + order.getStatus() + "，停止重试");
                cleanOrderRetryInfo(orderId);
                return;
            }
            
            // 2. 更新重试信息
            updateRetryInfo(orderId, retryRound);
            
            // 3. 根据重试轮次调整策略
            double searchRadius = SEARCH_RADIUS_KM * RETRY_RADIUS_MULTIPLIERS[retryRound];
            int maxDrivers = RETRY_MAX_DRIVERS[retryRound];
            
            System.out.println("第 " + (retryRound + 1) + " 轮重试策略: 搜索半径=" + searchRadius + "km, 最大通知司机数=" + maxDrivers);
            
            // 4. 查找附近司机
            List<Driver> nearbyDrivers = driverRedisService.getNearbyOnlineDrivers(
                order.getPickupLatitude(), 
                order.getPickupLongitude(), 
                searchRadius
            );
            
            if (nearbyDrivers.isEmpty()) {
                System.out.println("第 " + (retryRound + 1) + " 轮重试未找到司机，安排下一轮重试");
                scheduleOrderRetry(orderId, retryRound + 1);
                return;
            }
            
            // 5. 过滤掉已通知过的司机
            List<Driver> availableDrivers = new java.util.ArrayList<>();
            for (Driver driver : nearbyDrivers) {
                if (!hasNotifiedDriver(orderId, driver.getId())) {
                    availableDrivers.add(driver);
                }
            }
            
            if (availableDrivers.isEmpty()) {
                System.out.println("第 " + (retryRound + 1) + " 轮重试: 所有司机都已通知过，安排下一轮重试");
                scheduleOrderRetry(orderId, retryRound + 1);
                return;
            }
            
            // 6. 分层通知新司机（内部会限流 MAX_NOTIFY_DRIVERS）
            layeredNotifyDriversByRating(order, availableDrivers);
            
            System.out.println("第 " + (retryRound + 1) + " 轮重试完成，已按分层策略安排通知");
            
            // 7. 安排下一轮重试
            scheduleOrderRetry(orderId, retryRound + 1);
            
        } catch (Exception e) {
            System.err.println("执行订单重试失败: " + e.getMessage());
            e.printStackTrace();
            
            // 出错时也要安排下一轮重试
            scheduleOrderRetry(orderId, retryRound + 1);
        }
    }

    /**
     * 更新重试信息
     */
    private void updateRetryInfo(Long orderId, int retryRound) {
        try {
            String key = ORDER_RETRY_INFO_KEY + orderId;
            redisTemplate.opsForHash().put(key, "retryCount", retryRound + 1);
            redisTemplate.opsForHash().put(key, "lastRetryTime", System.currentTimeMillis());
        } catch (Exception e) {
            System.err.println("更新重试信息失败: " + e.getMessage());
        }
    }

    /**
     * 清理订单重试信息（订单完成或取消时调用）
     */
    public void cleanOrderRetryInfo(Long orderId) {
        try {
            String key = ORDER_RETRY_INFO_KEY + orderId;
            redisTemplate.delete(key);
            System.out.println("已清理订单 " + orderId + " 的重试信息");
        } catch (Exception e) {
            System.err.println("清理订单重试信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取订单重试信息
     */
    public Map<Object, Object> getOrderRetryInfo(Long orderId) {
        try {
            String key = ORDER_RETRY_INFO_KEY + orderId;
            return redisTemplate.opsForHash().entries(key);
        } catch (Exception e) {
            System.err.println("获取订单重试信息失败: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * 手动触发订单重试（管理员功能）
     */
    public void manualRetryOrder(Long orderId) {
        System.out.println("=== 手动触发订单 " + orderId + " 重试 ===");
        
        try {
            Order order = orderMapper.selectById(orderId);
            if (order == null || !"PENDING".equals(order.getStatus())) {
                System.out.println("订单状态不符合重试条件");
                return;
            }
            
            // 立即执行重试，使用最大搜索范围
            executeOrderRetry(orderId, RETRY_INTERVALS.length - 1);
            
        } catch (Exception e) {
            System.err.println("手动重试订单失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}