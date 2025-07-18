package com.taxi.service;

import com.taxi.entity.Driver;
import com.taxi.mapper.DriverMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 司机位置服务
 * 处理司机位置上报和管理
 * 模拟TCP长连接接收司机位置信息
 */
@Service
public class DriverLocationService {

    @Autowired
    private DriverMapper driverMapper;
    
    @Autowired
    private DriverRedisService driverRedisService;

    // 模拟TCP连接状态管理
    private final Map<Long, DriverConnectionInfo> activeConnections = new ConcurrentHashMap<>();
    
    /**
     * 司机连接状态信息
     */
    public static class DriverConnectionInfo {
        private Long driverId;
        private LocalDateTime lastHeartbeat;
        private boolean isOnline;
        private BigDecimal lastLatitude;
        private BigDecimal lastLongitude;
        private String vehicleHeading; // 车头朝向
        
        // 构造函数和getter/setter
        public DriverConnectionInfo(Long driverId) {
            this.driverId = driverId;
            this.lastHeartbeat = LocalDateTime.now();
            this.isOnline = true;
        }
        
        // Getters and Setters
        public Long getDriverId() { return driverId; }
        public LocalDateTime getLastHeartbeat() { return lastHeartbeat; }
        public void setLastHeartbeat(LocalDateTime lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }
        public boolean isOnline() { return isOnline; }
        public void setOnline(boolean online) { isOnline = online; }
        public BigDecimal getLastLatitude() { return lastLatitude; }
        public void setLastLatitude(BigDecimal lastLatitude) { this.lastLatitude = lastLatitude; }
        public BigDecimal getLastLongitude() { return lastLongitude; }
        public void setLastLongitude(BigDecimal lastLongitude) { this.lastLongitude = lastLongitude; }
        public String getVehicleHeading() { return vehicleHeading; }
        public void setVehicleHeading(String vehicleHeading) { this.vehicleHeading = vehicleHeading; }
    }

    /**
     * 司机建立TCP连接（模拟）
     */
    public void establishConnection(Long driverId) {
        System.out.println("司机 " + driverId + " 建立TCP长连接");
        
        DriverConnectionInfo connectionInfo = new DriverConnectionInfo(driverId);
        activeConnections.put(driverId, connectionInfo);
        
        // 更新司机在线状态
        try {
            Driver driver = driverMapper.selectById(driverId);
            if (driver != null) {
                driver.setIsOnline(true);
                driver.setUpdatedAt(LocalDateTime.now());
                driverMapper.updateById(driver);
                
                // 加入Redis缓存
                driverRedisService.driverGoOnline(driver);
                
                // 注意：检查待分配订单的逻辑移到DriverController中处理，避免循环依赖
            }
        } catch (Exception e) {
            System.err.println("司机上线处理失败: " + e.getMessage());
        }
    }

    /**
     * 司机断开TCP连接（模拟）
     */
    public void closeConnection(Long driverId) {
        System.out.println("司机 " + driverId + " 断开TCP长连接");
        
        DriverConnectionInfo connectionInfo = activeConnections.remove(driverId);
        if (connectionInfo != null) {
            connectionInfo.setOnline(false);
        }
        
        // 更新司机离线状态
        try {
            Driver driver = driverMapper.selectById(driverId);
            if (driver != null) {
                driver.setIsOnline(false);
                driver.setUpdatedAt(LocalDateTime.now());
                driverMapper.updateById(driver);
                
                // 从Redis缓存移除
                driverRedisService.driverGoOffline(driverId);
            }
        } catch (Exception e) {
            System.err.println("司机下线处理失败: " + e.getMessage());
        }
    }

    /**
     * 处理司机位置上报（每3-5秒一次）
     */
    @Async
    public void handleLocationUpdate(Long driverId, BigDecimal latitude, BigDecimal longitude, String vehicleHeading) {
        System.out.println("收到司机 " + driverId + " 位置上报: " + latitude + "," + longitude + " 朝向:" + vehicleHeading);
        
        DriverConnectionInfo connectionInfo = activeConnections.get(driverId);
        if (connectionInfo == null) {
            System.out.println("司机 " + driverId + " 未建立连接，忽略位置上报");
            return;
        }
        
        // 更新连接信息
        connectionInfo.setLastHeartbeat(LocalDateTime.now());
        connectionInfo.setLastLatitude(latitude);
        connectionInfo.setLastLongitude(longitude);
        connectionInfo.setVehicleHeading(vehicleHeading);
        
        try {
            // 更新数据库
            Driver driver = driverMapper.selectById(driverId);
            if (driver != null) {
                driver.setCurrentLatitude(latitude);
                driver.setCurrentLongitude(longitude);
                driver.setUpdatedAt(LocalDateTime.now());
                driverMapper.updateById(driver);
                
                // 更新Redis缓存
                driverRedisService.updateDriverLocation(driverId, latitude, longitude);
            }
            
        } catch (Exception e) {
            System.err.println("更新司机位置失败: " + e.getMessage());
        }
    }

    /**
     * 向司机推送订单消息（通过TCP长连接）
     */
    public void pushOrderToDriver(Long driverId, Map<String, Object> orderMessage) {
        DriverConnectionInfo connectionInfo = activeConnections.get(driverId);
        if (connectionInfo == null || !connectionInfo.isOnline()) {
            System.out.println("司机 " + driverId + " 不在线，无法推送订单");
            return;
        }
        
        try {
            // 模拟通过TCP长连接推送订单消息
            System.out.println("通过TCP长连接向司机 " + driverId + " 推送订单: " + orderMessage.get("orderId"));
            
            // 这里在真实环境中会通过TCP连接发送消息
            // 现在我们通过WebSocket模拟
            // webSocketNotificationService.notifyDriverNewOrder(driverId, order, distance);
            
        } catch (Exception e) {
            System.err.println("推送订单到司机失败: " + e.getMessage());
        }
    }

    /**
     * 检查连接心跳，清理超时连接
     */
    public void checkHeartbeat() {
        LocalDateTime now = LocalDateTime.now();
        
        activeConnections.entrySet().removeIf(entry -> {
            DriverConnectionInfo info = entry.getValue();
            // 如果超过30秒没有心跳，认为连接断开
            if (info.getLastHeartbeat().plusSeconds(30).isBefore(now)) {
                System.out.println("司机 " + entry.getKey() + " 连接超时，自动断开");
                closeConnection(entry.getKey());
                return true;
            }
            return false;
        });
    }

    /**
     * 获取在线司机连接数
     */
    public int getActiveConnectionCount() {
        return activeConnections.size();
    }

    /**
     * 获取司机连接信息
     */
    public DriverConnectionInfo getDriverConnection(Long driverId) {
        return activeConnections.get(driverId);
    }

    /**
     * 获取所有在线司机连接
     */
    public Map<Long, DriverConnectionInfo> getAllActiveConnections() {
        return new ConcurrentHashMap<>(activeConnections);
    }
}