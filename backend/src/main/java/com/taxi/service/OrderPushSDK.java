package com.taxi.service;

import com.taxi.entity.Order;
import com.taxi.entity.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单推送SDK
 * 负责选择最合适的司机并推送订单
 * 参照真实网约车业务架构设计
 */
@Service
public class OrderPushSDK {

    @Autowired
    private DriverRedisService driverRedisService;
    
    @Autowired
    private WebSocketNotificationService webSocketNotificationService;

    // 推送策略配置
    private static final double MAX_PUSH_RADIUS_KM = 5.0; // 最大推送半径
    private static final int MAX_PUSH_DRIVERS = 3; // 最大推送司机数量
    private static final double MIN_DRIVER_RATING = 4.0; // 最低司机评分

    /**
     * 选择最合适的司机并推送订单
     */
    public boolean pushOrderToSuitableDrivers(Order order) {
        System.out.println("=== 订单推送SDK开始为订单 " + order.getId() + " 选择司机 ===");
        
        try {
            // 1. 从地理位置系统获取附近司机
            List<Driver> nearbyDrivers = driverRedisService.getNearbyOnlineDrivers(
                order.getPickupLatitude(),
                order.getPickupLongitude(),
                MAX_PUSH_RADIUS_KM
            );
            
            if (nearbyDrivers.isEmpty()) {
                System.out.println("附近没有可用司机");
                return false;
            }
            
            // 2. 筛选符合条件的司机
            List<Driver> suitableDrivers = filterSuitableDrivers(nearbyDrivers);
            
            if (suitableDrivers.isEmpty()) {
                System.out.println("没有符合条件的司机");
                return false;
            }
            
            // 3. 按优先级排序司机
            List<Driver> sortedDrivers = sortDriversByPriority(suitableDrivers, order);
            
            // 4. 推送订单给最合适的司机们
            int pushCount = Math.min(sortedDrivers.size(), MAX_PUSH_DRIVERS);
            boolean hasPushed = false;
            
            for (int i = 0; i < pushCount; i++) {
                Driver driver = sortedDrivers.get(i);
                if (pushOrderToDriver(driver, order)) {
                    hasPushed = true;
                }
            }
            
            System.out.println("订单推送完成，共推送给 " + pushCount + " 个司机");
            return hasPushed;
            
        } catch (Exception e) {
            System.err.println("订单推送失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 筛选符合条件的司机
     */
    private List<Driver> filterSuitableDrivers(List<Driver> drivers) {
        return drivers.stream()
            .filter(driver -> {
                // 检查司机是否忙碌
                if (driverRedisService.isDriverBusy(driver.getId())) {
                    return false;
                }
                
                // 检查司机评分
                if (driver.getRating() != null && driver.getRating().doubleValue() < MIN_DRIVER_RATING) {
                    return false;
                }
                
                // 检查司机是否有TCP连接
                if (!isDriverConnected(driver.getId())) {
                    return false;
                }
                
                return true;
            })
            .toList();
    }

    /**
     * 按优先级排序司机
     */
    private List<Driver> sortDriversByPriority(List<Driver> drivers, Order order) {
        return drivers.stream()
            .sorted((d1, d2) -> {
                // 计算距离
                double dist1 = calculateDistance(
                    order.getPickupLatitude().doubleValue(),
                    order.getPickupLongitude().doubleValue(),
                    d1.getCurrentLatitude().doubleValue(),
                    d1.getCurrentLongitude().doubleValue()
                );
                
                double dist2 = calculateDistance(
                    order.getPickupLatitude().doubleValue(),
                    order.getPickupLongitude().doubleValue(),
                    d2.getCurrentLatitude().doubleValue(),
                    d2.getCurrentLongitude().doubleValue()
                );
                
                // 综合考虑距离和评分
                double score1 = calculateDriverScore(d1, dist1);
                double score2 = calculateDriverScore(d2, dist2);
                
                return Double.compare(score2, score1); // 分数高的排前面
            })
            .toList();
    }

    /**
     * 计算司机综合评分
     */
    private double calculateDriverScore(Driver driver, double distance) {
        double distanceScore = Math.max(0, 5000 - distance) / 5000 * 60; // 距离分数(60%)
        double ratingScore = driver.getRating() != null ? driver.getRating().doubleValue() * 8 : 40; // 评分分数(40%)
        
        return distanceScore + ratingScore;
    }

    /**
     * 推送订单给指定司机
     */
    private boolean pushOrderToDriver(Driver driver, Order order) {
        try {
            // 计算距离
            double distance = calculateDistance(
                order.getPickupLatitude().doubleValue(),
                order.getPickupLongitude().doubleValue(),
                driver.getCurrentLatitude().doubleValue(),
                driver.getCurrentLongitude().doubleValue()
            );
            
            // 构建订单消息
            Map<String, Object> orderMessage = buildOrderMessage(order, driver, distance);
            
            // 通过WebSocket推送订单（主要通知方式）
            webSocketNotificationService.notifyDriverNewOrder(driver.getId(), order, distance);
            
            // TCP长连接推送的逻辑移到WebSocket服务中处理，避免循环依赖
            System.out.println("订单消息已通过WebSocket推送给司机");
            
            System.out.println("订单 " + order.getId() + " 已推送给司机 " + driver.getId() + 
                             " (距离: " + String.format("%.2f", distance/1000) + "km)");
            
            return true;
            
        } catch (Exception e) {
            System.err.println("推送订单给司机 " + driver.getId() + " 失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 构建订单消息
     */
    private Map<String, Object> buildOrderMessage(Order order, Driver driver, double distance) {
        Map<String, Object> message = new HashMap<>();
        message.put("orderId", order.getId());
        message.put("orderNumber", order.getOrderNumber());
        message.put("passengerId", order.getPassengerId());
        message.put("pickupAddress", order.getPickupAddress());
        message.put("pickupLatitude", order.getPickupLatitude());
        message.put("pickupLongitude", order.getPickupLongitude());
        message.put("destinationAddress", order.getDestinationAddress());
        message.put("destinationLatitude", order.getDestinationLatitude());
        message.put("destinationLongitude", order.getDestinationLongitude());
        message.put("estimatedFare", order.getEstimatedFare());
        message.put("distance", distance);
        message.put("distanceKm", String.format("%.2f", distance/1000));
        message.put("driverId", driver.getId());
        message.put("timestamp", System.currentTimeMillis());
        message.put("expireTime", System.currentTimeMillis() + 30000); // 30秒过期
        
        return message;
    }

    /**
     * 检查司机是否有TCP连接
     * 暂时通过Redis状态检查，避免循环依赖
     */
    private boolean isDriverConnected(Long driverId) {
        // 通过Redis检查司机是否在线且空闲
        return driverRedisService.isDriverOnlineAndFree(driverId);
    }

    /**
     * 处理司机拒单
     */
    public void handleDriverRejectOrder(Long driverId, Long orderId, String reason) {
        System.out.println("司机 " + driverId + " 拒绝订单 " + orderId + ", 原因: " + reason);
        
        // 记录拒单信息，用于评估司机接单效率
        recordDriverRejectOrder(driverId, orderId, reason);
        
        // 可以考虑将订单重新推送给其他司机
        // 或者扩大搜索范围重新分配
    }

    /**
     * 记录司机拒单信息
     */
    private void recordDriverRejectOrder(Long driverId, Long orderId, String reason) {
        try {
            // 这里可以记录到数据库或Redis中
            // 用于后续分析司机的接单效率和服务质量
            System.out.println("记录司机拒单信息 - 司机: " + driverId + ", 订单: " + orderId + ", 原因: " + reason);
            
            // 可以更新司机的拒单率统计
            // updateDriverRejectRate(driverId);
            
        } catch (Exception e) {
            System.err.println("记录司机拒单信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取司机推送统计
     */
    public Map<String, Object> getDriverPushStats(Long driverId) {
        Map<String, Object> stats = new HashMap<>();
        
        // 这里可以从数据库或Redis获取司机的推送统计信息
        stats.put("driverId", driverId);
        stats.put("totalPushed", 0); // 总推送次数
        stats.put("totalAccepted", 0); // 总接单次数
        stats.put("totalRejected", 0); // 总拒单次数
        stats.put("acceptRate", 0.0); // 接单率
        stats.put("avgResponseTime", 0); // 平均响应时间
        
        return stats;
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