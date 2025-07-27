package com.taxi.controller;

import com.taxi.common.Result;
import com.taxi.entity.Driver;
import com.taxi.entity.Order;
import com.taxi.mapper.DriverMapper;
import com.taxi.mapper.OrderMapper;
import com.taxi.service.DriverRedisService;
import com.taxi.service.OrderDispatchService;
import com.taxi.service.DriverLocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/drivers")
@CrossOrigin(origins = "*")
public class DriverController {

    @Autowired
    private DriverMapper driverMapper;
    
    @Autowired
    private OrderMapper orderMapper;
    
    @Autowired
    private DriverRedisService driverRedisService;
    
    @Autowired
    private OrderDispatchService orderDispatchService;
    
    @Autowired
    private DriverLocationService driverLocationService;

    /**
     * 司机上线
     */
    @PostMapping("/{driverId}/online")
    public Result<String> goOnline(@PathVariable Long driverId, 
                                   @RequestParam BigDecimal latitude, 
                                   @RequestParam BigDecimal longitude) {
        try {
            Driver driver = driverMapper.selectById(driverId);
            if (driver == null) {
                return Result.error("司机不存在");
            }
            
            // 更新司机状态为在线，并更新位置
            driver.setIsOnline(true);
            driver.setCurrentLatitude(latitude);
            driver.setCurrentLongitude(longitude);
            driver.setUpdatedAt(LocalDateTime.now());
            
            driverMapper.updateById(driver);
            
            System.out.println("司机上线成功 - ID: " + driverId + 
                             ", 输入坐标: 纬度=" + latitude + ", 经度=" + longitude);
            System.out.println("存储到数据库: 纬度字段=" + driver.getCurrentLatitude() + 
                             ", 经度字段=" + driver.getCurrentLongitude());
            
            // 司机上线后，建立TCP长连接并加入Redis缓存
            try {
                // 1. 建立TCP长连接（模拟）
                driverLocationService.establishConnection(driverId);
                
                // 2. 加入Redis GEO索引
                driverRedisService.driverGoOnline(driver);
                System.out.println("司机 " + driverId + " 已建立TCP连接并加入Redis GEO索引");
                
                // 注意：不在这里立即检查待分配订单，等WebSocket连接建立后再处理
                System.out.println("司机 " + driverId + " 上线完成，等待WebSocket连接后推送待分配订单");
                
            } catch (Exception e) {
                System.err.println("司机上线处理失败: " + e.getMessage());
                // 不影响司机上线的结果
            }
            
            return Result.success("上线成功");
        } catch (Exception e) {
            return Result.error("上线失败: " + e.getMessage());
        }
    }

    /**
     * 司机下线
     */
    @PostMapping("/{driverId}/offline")
    public Result<String> goOffline(@PathVariable Long driverId) {
        try {
            Driver driver = driverMapper.selectById(driverId);
            if (driver == null) {
                return Result.error("司机不存在");
            }
            
            // 更新司机状态为离线
            driver.setIsOnline(false);
            driver.setUpdatedAt(LocalDateTime.now());
            
            driverMapper.updateById(driver);
            
            // 司机下线时断开TCP连接并从Redis缓存移除
            try {
                // 1. 断开TCP长连接（模拟）
                driverLocationService.closeConnection(driverId);
                
                // 2. 从Redis缓存移除
                driverRedisService.driverGoOffline(driverId);
                System.out.println("司机 " + driverId + " 已断开TCP连接并从在线缓存移除");
            } catch (Exception e) {
                System.err.println("司机下线处理失败: " + e.getMessage());
            }
            
            System.out.println("司机下线成功 - ID: " + driverId);
            
            return Result.success("下线成功");
        } catch (Exception e) {
            return Result.error("下线失败: " + e.getMessage());
        }
    }

    /**
     * 司机接单
     */
    @PostMapping("/{driverId}/accept-order/{orderId}")
    public Result<Order> acceptOrder(@PathVariable Long driverId, @PathVariable Long orderId) {
        try {
            System.out.println("=== 接单请求开始 ===");
            System.out.println("司机ID: " + driverId);
            System.out.println("订单ID: " + orderId);
            
            // 先检查司机是否存在
            Driver driver = driverMapper.selectById(driverId);
            if (driver == null) {
                System.out.println("❌ 司机不存在: " + driverId);
                return Result.error("司机不存在");
            }
            
            // 检查司机是否在线
            if (!driver.getIsOnline()) {
                System.out.println("❌ 司机不在线: " + driverId);
                return Result.error("司机不在线");
            }
            
            System.out.println("✅ 司机状态检查通过");
            
            boolean success = orderDispatchService.acceptOrder(orderId, driverId);
            
            if (success) {
                // 接单成功，返回完整的订单信息
                Order order = orderMapper.selectById(orderId);
                if (order != null) {
                    System.out.println("✅ 司机 " + driverId + " 接单 " + orderId + " 成功，返回订单信息");
                    System.out.println("📍 订单坐标信息: pickup(" + order.getPickupLongitude() + "," + order.getPickupLatitude() + 
                                     "), destination(" + order.getDestinationLongitude() + "," + order.getDestinationLatitude() + ")");
                    return Result.success(order);
                } else {
                    System.out.println("❌ 接单成功但无法获取订单信息");
                    return Result.error("接单成功但无法获取订单信息");
                }
            } else {
                System.out.println("❌ 司机 " + driverId + " 接单 " + orderId + " 失败");
                return Result.error("接单失败，订单可能已被其他司机接单或司机状态不符合要求");
            }
        } catch (Exception e) {
            System.err.println("❌ 接单异常: " + e.getMessage());
            e.printStackTrace();
            return Result.error("接单失败: " + e.getMessage());
        }
    }

    /**
     * 司机拒单
     */
    @PostMapping("/{driverId}/reject-order/{orderId}")
    public Result<String> rejectOrder(@PathVariable Long driverId, @PathVariable Long orderId, 
                                     @RequestParam(defaultValue = "司机拒单") String reason) {
        try {
            System.out.println("司机 " + driverId + " 拒绝订单 " + orderId + ", 原因: " + reason);
            
            // 调用订单分发服务处理拒单
            orderDispatchService.handleDriverRejectOrder(orderId, driverId, reason);
            
            System.out.println("司机 " + driverId + " 拒单 " + orderId + " 处理完成");
            return Result.success("拒单成功");
        } catch (Exception e) {
            System.err.println("拒单异常: " + e.getMessage());
            return Result.error("拒单失败: " + e.getMessage());
        }
    }

    /**
     * 司机位置上报（模拟TCP长连接每3-5秒上报一次）
     */
    @PostMapping("/{driverId}/location")
    public Result<String> reportLocation(@PathVariable Long driverId,
                                        @RequestParam BigDecimal latitude,
                                        @RequestParam BigDecimal longitude,
                                        @RequestParam(required = false) String vehicleHeading) {
        try {
            // 通过DriverLocationService处理位置上报
            driverLocationService.handleLocationUpdate(driverId, latitude, longitude, vehicleHeading);
            
            return Result.success("位置上报成功");
        } catch (Exception e) {
            return Result.error("位置上报失败: " + e.getMessage());
        }
    }

    /**
     * 手动更新司机位置（兼容旧接口）
     */
    @PostMapping("/{driverId}/update-location")
    public Result<String> updateLocation(@PathVariable Long driverId,
                                        @RequestParam BigDecimal latitude,
                                        @RequestParam BigDecimal longitude) {
        try {
            Driver driver = driverMapper.selectById(driverId);
            if (driver == null) {
                return Result.error("司机不存在");
            }
            
            // 更新数据库中的司机位置
            driver.setCurrentLatitude(latitude);
            driver.setCurrentLongitude(longitude);
            driver.setUpdatedAt(LocalDateTime.now());
            driverMapper.updateById(driver);
            
            // 更新Redis缓存中的位置信息
            try {
                driverRedisService.updateDriverLocation(driverId, latitude, longitude);
                System.out.println("司机 " + driverId + " 位置已更新到缓存");
            } catch (Exception e) {
                System.err.println("更新缓存位置失败: " + e.getMessage());
            }
            
            return Result.success("位置更新成功");
        } catch (Exception e) {
            return Result.error("位置更新失败: " + e.getMessage());
        }
    }

    /**
     * 获取在线司机列表
     */
    @GetMapping("/online")
    public Result<List<Driver>> getOnlineDrivers() {
        try {
            List<Driver> drivers = driverMapper.selectOnlineAndFreeDrivers();
            return Result.success(drivers);
        } catch (Exception e) {
            return Result.error("获取在线司机失败: " + e.getMessage());
        }
    }

    /**
     * 获取司机信息
     */
    @GetMapping("/{driverId}")
    public Result<Driver> getDriverInfo(@PathVariable Long driverId) {
        try {
            Driver driver = driverMapper.selectById(driverId);
            if (driver == null) {
                return Result.error("司机不存在");
            }
            return Result.success(driver);
        } catch (Exception e) {
            return Result.error("获取司机信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取在线司机统计（从Redis）
     */
    @GetMapping("/online/stats")
    public Result<String> getOnlineStats() {
        try {
            long onlineCount = driverRedisService.getOnlineDriverCount();
            return Result.success("当前有 " + onlineCount + " 个司机在线");
        } catch (Exception e) {
            return Result.error("获取在线统计失败: " + e.getMessage());
        }
    }

    /**
     * 检查司机是否忙碌
     */
    @GetMapping("/{driverId}/busy")
    public Result<Boolean> isDriverBusy(@PathVariable Long driverId) {
        try {
            boolean isBusy = driverRedisService.isDriverBusy(driverId);
            return Result.success(isBusy);
        } catch (Exception e) {
            return Result.error("检查忙碌状态失败: " + e.getMessage());
        }
    }

    /**
     * 获取附近司机（测试用）
     */
    @GetMapping("/nearby")
    public Result<List<Driver>> getNearbyDrivers(@RequestParam BigDecimal latitude,
                                                @RequestParam BigDecimal longitude,
                                                @RequestParam(defaultValue = "5.0") double radiusKm) {
        try {
            List<Driver> nearbyDrivers = driverRedisService.getNearbyOnlineDrivers(latitude, longitude, radiusKm);
            return Result.success(nearbyDrivers);
        } catch (Exception e) {
            return Result.error("获取附近司机失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有在线司机（从Redis）
     */
    @GetMapping("/online/redis")
    public Result<List<Driver>> getOnlineDriversFromRedis() {
        try {
            List<Driver> drivers = driverRedisService.getAllOnlineDrivers();
            return Result.success(drivers);
        } catch (Exception e) {
            return Result.error("获取在线司机失败: " + e.getMessage());
        }
    }

    /**
     * 司机完成订单
     */
    @PostMapping("/{driverId}/complete-order/{orderId}")
    public Result<String> completeOrder(@PathVariable Long driverId, @PathVariable Long orderId) {
        try {
            orderDispatchService.completeOrder(orderId, driverId);
            return Result.success("订单完成");
        } catch (Exception e) {
            return Result.error("完成订单失败: " + e.getMessage());
        }
    }

    /**
     * 获取司机TCP连接状态
     */
    @GetMapping("/{driverId}/connection")
    public Result<Object> getConnectionStatus(@PathVariable Long driverId) {
        try {
            DriverLocationService.DriverConnectionInfo connectionInfo = 
                driverLocationService.getDriverConnection(driverId);
            
            if (connectionInfo == null) {
                return Result.success("司机未连接");
            }
            
            return Result.success(Map.of(
                "driverId", connectionInfo.getDriverId(),
                "isOnline", connectionInfo.isOnline(),
                "lastHeartbeat", connectionInfo.getLastHeartbeat(),
                "lastLatitude", connectionInfo.getLastLatitude(),
                "lastLongitude", connectionInfo.getLastLongitude(),
                "vehicleHeading", connectionInfo.getVehicleHeading()
            ));
        } catch (Exception e) {
            return Result.error("获取连接状态失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有活跃TCP连接统计
     */
    @GetMapping("/connections/stats")
    public Result<Object> getConnectionStats() {
        try {
            int activeConnections = driverLocationService.getActiveConnectionCount();
            return Result.success(Map.of(
                "activeConnections", activeConnections,
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            return Result.error("获取连接统计失败: " + e.getMessage());
        }
    }
}