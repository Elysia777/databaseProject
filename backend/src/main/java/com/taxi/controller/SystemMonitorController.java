package com.taxi.controller;

import com.taxi.common.Result;
import com.taxi.entity.Driver;
import com.taxi.entity.Order;
import com.taxi.mapper.OrderMapper;
import com.taxi.service.DriverRedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统监控控制器
 * 提供Redis + RabbitMQ混合架构的监控API
 */
@RestController
@RequestMapping("/api/monitor")
@CrossOrigin(origins = "*")
public class SystemMonitorController {

    @Autowired
    private DriverRedisService driverRedisService;
    
    @Autowired
    private OrderMapper orderMapper;

    /**
     * 获取系统整体状态
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> getSystemStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            
            // Redis状态
            long onlineDriverCount = driverRedisService.getOnlineDriverCount();
            status.put("onlineDrivers", onlineDriverCount);
            
            // 订单状态统计
            List<Order> pendingOrders = orderMapper.selectByStatus("PENDING");
            List<Order> assignedOrders = orderMapper.selectByStatus("ASSIGNED");
            List<Order> completedOrders = orderMapper.selectByStatus("COMPLETED");
            
            status.put("pendingOrders", pendingOrders != null ? pendingOrders.size() : 0);
            status.put("assignedOrders", assignedOrders != null ? assignedOrders.size() : 0);
            status.put("completedOrders", completedOrders != null ? completedOrders.size() : 0);
            
            // 系统健康状态
            status.put("systemHealth", "HEALTHY");
            status.put("timestamp", System.currentTimeMillis());
            
            return Result.success(status);
            
        } catch (Exception e) {
            return Result.error("获取系统状态失败: " + e.getMessage());
        }
    }

    /**
     * 获取Redis中的司机分布
     */
    @GetMapping("/drivers/distribution")
    public Result<Map<String, Object>> getDriverDistribution() {
        try {
            Map<String, Object> distribution = new HashMap<>();
            
            List<Driver> onlineDrivers = driverRedisService.getAllOnlineDrivers();
            distribution.put("totalOnline", onlineDrivers.size());
            
            // 统计忙碌和空闲司机
            long busyCount = onlineDrivers.stream()
                .mapToLong(driver -> driverRedisService.isDriverBusy(driver.getId()) ? 1 : 0)
                .sum();
            
            distribution.put("busyDrivers", busyCount);
            distribution.put("freeDrivers", onlineDrivers.size() - busyCount);
            
            return Result.success(distribution);
            
        } catch (Exception e) {
            return Result.error("获取司机分布失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定区域的司机密度
     */
    @GetMapping("/drivers/density")
    public Result<Map<String, Object>> getDriverDensity(@RequestParam Double latitude,
                                                       @RequestParam Double longitude,
                                                       @RequestParam(defaultValue = "5.0") Double radiusKm) {
        try {
            List<Driver> nearbyDrivers = driverRedisService.getNearbyOnlineDrivers(
                java.math.BigDecimal.valueOf(latitude),
                java.math.BigDecimal.valueOf(longitude),
                radiusKm
            );
            
            Map<String, Object> density = new HashMap<>();
            density.put("centerLatitude", latitude);
            density.put("centerLongitude", longitude);
            density.put("radiusKm", radiusKm);
            density.put("driverCount", nearbyDrivers.size());
            density.put("density", nearbyDrivers.size() / (Math.PI * radiusKm * radiusKm)); // 司机/平方公里
            
            return Result.success(density);
            
        } catch (Exception e) {
            return Result.error("获取司机密度失败: " + e.getMessage());
        }
    }

    /**
     * 获取订单处理效率统计
     */
    @GetMapping("/orders/efficiency")
    public Result<Map<String, Object>> getOrderEfficiency() {
        try {
            Map<String, Object> efficiency = new HashMap<>();
            
            // 这里可以添加更复杂的统计逻辑
            // 比如平均接单时间、完成率等
            
            List<Order> allOrders = orderMapper.selectAll();
            if (allOrders != null && !allOrders.isEmpty()) {
                long totalOrders = allOrders.size();
                long completedOrders = allOrders.stream()
                    .mapToLong(order -> "COMPLETED".equals(order.getStatus()) ? 1 : 0)
                    .sum();
                
                efficiency.put("totalOrders", totalOrders);
                efficiency.put("completedOrders", completedOrders);
                efficiency.put("completionRate", totalOrders > 0 ? (double) completedOrders / totalOrders : 0);
            } else {
                efficiency.put("totalOrders", 0);
                efficiency.put("completedOrders", 0);
                efficiency.put("completionRate", 0);
            }
            
            return Result.success(efficiency);
            
        } catch (Exception e) {
            return Result.error("获取订单效率统计失败: " + e.getMessage());
        }
    }

    /**
     * 系统健康检查
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> healthCheck() {
        try {
            Map<String, Object> health = new HashMap<>();
            
            // 检查Redis连接
            try {
                long driverCount = driverRedisService.getOnlineDriverCount();
                health.put("redis", "HEALTHY");
                health.put("redisDriverCount", driverCount);
            } catch (Exception e) {
                health.put("redis", "UNHEALTHY");
                health.put("redisError", e.getMessage());
            }
            
            // 检查数据库连接
            try {
                List<Order> orders = orderMapper.selectByStatus("PENDING");
                health.put("database", "HEALTHY");
                health.put("pendingOrdersCount", orders != null ? orders.size() : 0);
            } catch (Exception e) {
                health.put("database", "UNHEALTHY");
                health.put("databaseError", e.getMessage());
            }
            
            // 整体健康状态
            boolean isHealthy = "HEALTHY".equals(health.get("redis")) && "HEALTHY".equals(health.get("database"));
            health.put("overall", isHealthy ? "HEALTHY" : "UNHEALTHY");
            health.put("timestamp", System.currentTimeMillis());
            
            return Result.success(health);
            
        } catch (Exception e) {
            return Result.error("健康检查失败: " + e.getMessage());
        }
    }
}