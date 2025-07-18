package com.taxi.service;

import com.taxi.entity.Order;
import com.taxi.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 待分配订单管理服务
 * 使用Redis持久化存储待分配订单，确保后续上线的司机也能看到
 */
@Service
public class PendingOrderService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private OrderMapper orderMapper;

    // Redis key
    private static final String PENDING_ORDERS_KEY = "pending_orders";
    private static final String ORDER_EXPIRE_KEY = "order_expire:";

    /**
     * 添加待分配订单到持久化队列
     */
    public void addPendingOrder(Long orderId) {
        try {
            // 1. 添加到Redis有序集合（按时间排序）
            long timestamp = System.currentTimeMillis();
            redisTemplate.opsForZSet().add(PENDING_ORDERS_KEY, orderId, timestamp);
            
            // 2. 设置订单过期时间（30分钟后自动移除）
            redisTemplate.opsForValue().set(ORDER_EXPIRE_KEY + orderId, timestamp, 30, TimeUnit.MINUTES);
            
            System.out.println("订单 " + orderId + " 已加入待分配队列，时间戳: " + timestamp);
            
        } catch (Exception e) {
            System.err.println("添加待分配订单失败: " + e.getMessage());
        }
    }

    /**
     * 从待分配队列移除订单（订单被分配后调用）
     */
    public void removePendingOrder(Long orderId) {
        try {
            redisTemplate.opsForZSet().remove(PENDING_ORDERS_KEY, orderId);
            redisTemplate.delete(ORDER_EXPIRE_KEY + orderId);
            System.out.println("订单 " + orderId + " 已从待分配队列移除");
            
        } catch (Exception e) {
            System.err.println("移除待分配订单失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有待分配订单（司机上线时调用）
     */
    public List<Order> getAllPendingOrders() {
        List<Order> pendingOrders = new ArrayList<>();
        
        try {
            // 1. 清理过期订单
            cleanExpiredOrders();
            
            // 2. 获取所有待分配订单ID（按时间排序）
            Set<Object> orderIds = redisTemplate.opsForZSet().range(PENDING_ORDERS_KEY, 0, -1);
            
            if (orderIds != null && !orderIds.isEmpty()) {
                for (Object orderIdObj : orderIds) {
                    Long orderId = Long.valueOf(orderIdObj.toString());
                    
                    // 3. 从数据库获取订单详情
                    Order order = orderMapper.selectById(orderId);
                    if (order != null && "PENDING".equals(order.getStatus())) {
                        pendingOrders.add(order);
                    } else {
                        // 订单状态已变更，从队列中移除
                        removePendingOrder(orderId);
                    }
                }
            }
            
            System.out.println("获取到 " + pendingOrders.size() + " 个待分配订单");
            
        } catch (Exception e) {
            System.err.println("获取待分配订单失败: " + e.getMessage());
        }
        
        return pendingOrders;
    }

    /**
     * 获取指定区域的待分配订单
     */
    public List<Order> getPendingOrdersNearby(double latitude, double longitude, double radiusKm) {
        List<Order> allPendingOrders = getAllPendingOrders();
        List<Order> nearbyOrders = new ArrayList<>();
        
        for (Order order : allPendingOrders) {
            if (order.getPickupLatitude() != null && order.getPickupLongitude() != null) {
                double distance = calculateDistance(
                    latitude, longitude,
                    order.getPickupLatitude().doubleValue(),
                    order.getPickupLongitude().doubleValue()
                );
                
                if (distance <= radiusKm) {
                    nearbyOrders.add(order);
                }
            }
        }
        
        System.out.println("在 " + radiusKm + "km 范围内找到 " + nearbyOrders.size() + " 个待分配订单");
        return nearbyOrders;
    }

    /**
     * 清理过期订单
     */
    private void cleanExpiredOrders() {
        try {
            long currentTime = System.currentTimeMillis();
            long expireTime = currentTime - (30 * 60 * 1000); // 30分钟前
            
            // 移除30分钟前的订单
            Long removedCount = redisTemplate.opsForZSet().removeRangeByScore(PENDING_ORDERS_KEY, 0, expireTime);
            
            if (removedCount != null && removedCount > 0) {
                System.out.println("清理了 " + removedCount + " 个过期的待分配订单");
            }
            
        } catch (Exception e) {
            System.err.println("清理过期订单失败: " + e.getMessage());
        }
    }

    /**
     * 计算两点间距离（公里）
     */
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371; // 地球半径（公里）
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLng/2) * Math.sin(dLng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    /**
     * 获取待分配订单数量
     */
    public long getPendingOrderCount() {
        try {
            cleanExpiredOrders();
            Long count = redisTemplate.opsForZSet().count(PENDING_ORDERS_KEY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
            return count != null ? count : 0;
        } catch (Exception e) {
            System.err.println("获取待分配订单数量失败: " + e.getMessage());
            return 0;
        }
    }
}