package com.taxi.service;

import com.taxi.entity.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 司机Redis缓存服务
 * 管理在线司机的缓存信息
 */
@Service
public class DriverRedisService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // Redis key前缀
    private static final String DRIVER_GEO_KEY = "driver_geo"; // GEO地理位置
    private static final String DRIVER_STATUS_KEY = "driver_status:"; // 司机状态 Hash
    private static final String ORDER_LOCK_KEY = "order_lock:"; // 订单分布式锁
    private static final String DRIVER_INFO_KEY = "driver_info:"; // 司机详细信息

    /**
     * 司机上线，加入Redis缓存
     */
    public void driverGoOnline(Driver driver) {
        try {
            Long driverId = driver.getId();
            
            // 1. 使用Redis GEO添加司机位置
            if (driver.getCurrentLatitude() != null && driver.getCurrentLongitude() != null) {
                double storedLatitude = driver.getCurrentLatitude().doubleValue();
                double storedLongitude = driver.getCurrentLongitude().doubleValue();
                
                System.out.println("Redis GEO 存储坐标:");
                System.out.println("  司机对象中的纬度字段: " + driver.getCurrentLatitude());
                System.out.println("  司机对象中的经度字段: " + driver.getCurrentLongitude());
                
                // 检查是否需要交换坐标（临时修复）
                double longitude, latitude;
                if (storedLatitude > 100) {
                    // 如果纬度字段的值大于100，说明存储时搞反了，需要交换
                    longitude = storedLatitude;  // 纬度字段实际存储的是经度
                    latitude = storedLongitude;   // 经度字段实际存储的是纬度
                    System.out.println("⚠️  检测到坐标存储错误，自动交换:");
                    System.out.println("  修正后经度: " + longitude);
                    System.out.println("  修正后纬度: " + latitude);
                } else {
                    longitude = storedLongitude;
                    latitude = storedLatitude;
                    System.out.println("✅ 坐标存储正确，无需交换");
                }
                
                System.out.println("  传给Redis的Point(经度, 纬度): (" + longitude + ", " + latitude + ")");
                
                // 验证坐标范围
                if (longitude < -180 || longitude > 180) {
                    System.err.println("❌ 经度超出范围: " + longitude + " (应该在-180到180之间)");
                    return;
                }
                if (latitude < -85.05112878 || latitude > 85.05112878) {
                    System.err.println("❌ 纬度超出范围: " + latitude + " (应该在-85到85之间)");
                    return;
                }
                
                redisTemplate.opsForGeo().add(
                    DRIVER_GEO_KEY,
                    new org.springframework.data.geo.Point(longitude, latitude),
                    driverId.toString()
                );
                
                System.out.println("✅ Redis GEO 存储成功");
            }
            
            // 2. 存储司机详细信息
            redisTemplate.opsForValue().set(
                DRIVER_INFO_KEY + driverId, 
                driver, 
                24, TimeUnit.HOURS
            );
            
            // 3. 设置司机状态为在线且空闲
            Map<String, Object> status = new HashMap<>();
            status.put("online", true);
            status.put("busy", false);
            status.put("lastUpdate", System.currentTimeMillis());
            
            redisTemplate.opsForHash().putAll(DRIVER_STATUS_KEY + driverId, status);
            redisTemplate.expire(DRIVER_STATUS_KEY + driverId, 24, TimeUnit.HOURS);
            
            System.out.println("司机 " + driverId + " 已上线并加入GEO索引");
            
        } catch (Exception e) {
            System.err.println("司机上线缓存失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 司机下线，从Redis缓存移除
     */
    public void driverGoOffline(Long driverId) {
        try {
            // 1. 从GEO索引中移除司机位置
            redisTemplate.opsForGeo().remove(DRIVER_GEO_KEY, driverId.toString());
            
            // 2. 删除司机详细信息
            redisTemplate.delete(DRIVER_INFO_KEY + driverId);
            
            // 3. 删除司机状态信息
            redisTemplate.delete(DRIVER_STATUS_KEY + driverId);
            
            System.out.println("司机 " + driverId + " 已从在线缓存移除");
            
        } catch (Exception e) {
            System.err.println("司机下线缓存失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 更新司机位置
     */
    public void updateDriverLocation(Long driverId, BigDecimal latitude, BigDecimal longitude) {
        try {
            System.out.println("更新司机位置 - ID: " + driverId + ", 纬度: " + latitude + ", 经度: " + longitude);
            
            // 1. 更新GEO位置 (Redis GEO使用 longitude, latitude 顺序)
            redisTemplate.opsForGeo().add(
                DRIVER_GEO_KEY,
                new org.springframework.data.geo.Point(
                    longitude.doubleValue(),  // 经度在前
                    latitude.doubleValue()    // 纬度在后
                ),
                driverId.toString()
            );
            
            // 2. 更新司机详细信息中的位置
            Driver driver = (Driver) redisTemplate.opsForValue().get(DRIVER_INFO_KEY + driverId);
            if (driver != null) {
                driver.setCurrentLatitude(latitude);
                driver.setCurrentLongitude(longitude);
                redisTemplate.opsForValue().set(DRIVER_INFO_KEY + driverId, driver, 24, TimeUnit.HOURS);
            }
            
            // 3. 更新状态时间戳
            redisTemplate.opsForHash().put(DRIVER_STATUS_KEY + driverId, "lastUpdate", System.currentTimeMillis());
            
        } catch (Exception e) {
            System.err.println("更新司机位置缓存失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 使用Redis GEO获取指定范围内的在线司机
     */
    public List<Driver> getNearbyOnlineDrivers(BigDecimal centerLat, BigDecimal centerLng, double radiusKm) {
        List<Driver> nearbyDrivers = new ArrayList<>();
        
        try {
            // 使用Redis GEO RADIUS命令查找附近司机
            // 注意：Redis GEO使用 (longitude, latitude) 顺序
            org.springframework.data.geo.Circle circle = new org.springframework.data.geo.Circle(
                new org.springframework.data.geo.Point(centerLng.doubleValue(), centerLat.doubleValue()),
                new org.springframework.data.geo.Distance(radiusKm, org.springframework.data.geo.Metrics.KILOMETERS)
            );
            
            org.springframework.data.redis.connection.RedisGeoCommands.GeoRadiusCommandArgs args = 
                org.springframework.data.redis.connection.RedisGeoCommands.GeoRadiusCommandArgs
                    .newGeoRadiusArgs()
                    .includeDistance()
                    .includeCoordinates()
                    .sortAscending()
                    .limit(50); // 限制最多返回50个司机
            
            org.springframework.data.geo.GeoResults<org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation<Object>> results = 
                redisTemplate.opsForGeo().radius(DRIVER_GEO_KEY, circle, args);
            
            if (results != null) {
                for (org.springframework.data.geo.GeoResult<org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation<Object>> result : results) {
                    String driverIdStr = result.getContent().getName().toString();
                    Long driverId = Long.valueOf(driverIdStr);
                    
                    // 检查司机是否在线且空闲
                    if (isDriverOnlineAndFree(driverId)) {
                        // 获取司机详细信息
                        Driver driver = getDriverInfo(driverId);
                        if (driver != null) {
                            nearbyDrivers.add(driver);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Redis连接失败，回退到数据库查询: " + e.getMessage());
            e.printStackTrace(); // 打印完整的异常堆栈
            // Redis不可用时，返回空列表
            return new ArrayList<>();
        }
        
        return nearbyDrivers;
    }

    /**
     * 标记司机为忙碌状态
     */
    public void markDriverBusy(Long driverId) {
        try {
            redisTemplate.opsForHash().put(DRIVER_STATUS_KEY + driverId, "busy", true);
            redisTemplate.opsForHash().put(DRIVER_STATUS_KEY + driverId, "lastUpdate", System.currentTimeMillis());
            System.out.println("司机 " + driverId + " 已标记为忙碌");
        } catch (Exception e) {
            System.err.println("标记司机忙碌失败: " + e.getMessage());
        }
    }

    /**
     * 标记司机为空闲状态
     */
    public void markDriverFree(Long driverId) {
        try {
            redisTemplate.opsForHash().put(DRIVER_STATUS_KEY + driverId, "busy", false);
            redisTemplate.opsForHash().put(DRIVER_STATUS_KEY + driverId, "lastUpdate", System.currentTimeMillis());
            System.out.println("司机 " + driverId + " 已标记为空闲");
        } catch (Exception e) {
            System.err.println("标记司机空闲失败: " + e.getMessage());
        }
    }

    /**
     * 检查司机是否忙碌
     */
    public boolean isDriverBusy(Long driverId) {
        try {
            Object busy = redisTemplate.opsForHash().get(DRIVER_STATUS_KEY + driverId, "busy");
            return busy != null && (Boolean) busy;
        } catch (Exception e) {
            System.err.println("检查司机忙碌状态失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 检查司机是否在线且空闲
     */
    public boolean isDriverOnlineAndFree(Long driverId) {
        try {
            Map<Object, Object> status = redisTemplate.opsForHash().entries(DRIVER_STATUS_KEY + driverId);
            if (status.isEmpty()) {
                return false;
            }
            
            Boolean online = (Boolean) status.get("online");
            Boolean busy = (Boolean) status.get("busy");
            
            return online != null && online && (busy == null || !busy);
        } catch (Exception e) {
            System.err.println("检查司机在线状态失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取司机详细信息
     */
    public Driver getDriverInfo(Long driverId) {
        try {
            return (Driver) redisTemplate.opsForValue().get(DRIVER_INFO_KEY + driverId);
        } catch (Exception e) {
            System.err.println("获取司机信息失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 获取所有在线司机数量
     */
    public long getOnlineDriverCount() {
        try {
            // 使用GEO命令获取所有司机，然后计算数量
            List<String> allDriverIds = redisTemplate.opsForGeo().hash(DRIVER_GEO_KEY, new Object[0]);
            return allDriverIds != null ? allDriverIds.size() : 0;
        } catch (Exception e) {
            System.err.println("获取在线司机数量失败: " + e.getMessage());
            return 0;
        }
    }

    /**
     * 获取所有在线司机列表
     */
    public List<Driver> getAllOnlineDrivers() {
        List<Driver> drivers = new ArrayList<>();
        try {
            // 由于Redis GEO没有直接获取所有成员的方法，我们使用一个大范围的搜索
            // 以北京为中心，搜索半径1000公里（覆盖全国）
            org.springframework.data.geo.Circle circle = new org.springframework.data.geo.Circle(
                new org.springframework.data.geo.Point(116.4074, 39.9042), // 北京坐标
                new org.springframework.data.geo.Distance(1000, org.springframework.data.geo.Metrics.KILOMETERS)
            );
            
            org.springframework.data.redis.connection.RedisGeoCommands.GeoRadiusCommandArgs args = 
                org.springframework.data.redis.connection.RedisGeoCommands.GeoRadiusCommandArgs
                    .newGeoRadiusArgs()
                    .limit(1000); // 限制最多返回1000个司机
            
            org.springframework.data.geo.GeoResults<org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation<Object>> results = 
                redisTemplate.opsForGeo().radius(DRIVER_GEO_KEY, circle, args);
            
            if (results != null) {
                for (org.springframework.data.geo.GeoResult<org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation<Object>> result : results) {
                    String driverIdStr = result.getContent().getName().toString();
                    Long driverId = Long.valueOf(driverIdStr);
                    
                    if (isDriverOnlineAndFree(driverId)) {
                        Driver driver = getDriverInfo(driverId);
                        if (driver != null) {
                            drivers.add(driver);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("获取所有在线司机失败: " + e.getMessage());
        }
        return drivers;
    }

    /**
     * 尝试获取订单分布式锁
     */
    public boolean tryLockOrder(Long orderId, Long driverId, int expireSeconds) {
        try {
            String lockKey = ORDER_LOCK_KEY + orderId;
            String lockValue = driverId.toString();
            
            // 使用SETNX实现分布式锁
            Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, expireSeconds, TimeUnit.SECONDS);
            
            if (success != null && success) {
                System.out.println("司机 " + driverId + " 成功获取订单 " + orderId + " 的锁");
                return true;
            } else {
                System.out.println("司机 " + driverId + " 获取订单 " + orderId + " 的锁失败");
                return false;
            }
        } catch (Exception e) {
            System.err.println("获取订单锁失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 释放订单分布式锁
     */
    public void releaseLockOrder(Long orderId, Long driverId) {
        try {
            String lockKey = ORDER_LOCK_KEY + orderId;
            String lockValue = driverId.toString();
            
            // 只有锁的持有者才能释放锁
            String currentValue = (String) redisTemplate.opsForValue().get(lockKey);
            if (lockValue.equals(currentValue)) {
                redisTemplate.delete(lockKey);
                System.out.println("司机 " + driverId + " 释放订单 " + orderId + " 的锁");
            }
        } catch (Exception e) {
            System.err.println("释放订单锁失败: " + e.getMessage());
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
        return s * 6378137.0; // 地球半径，返回米
    }

    /**
     * 获取司机当前执行的订单ID
     */
    public Long getDriverCurrentOrder(Long driverId) {
        try {
            String currentOrderKey = "driver_current_order:" + driverId;
            Object orderId = redisTemplate.opsForValue().get(currentOrderKey);
            return orderId != null ? Long.valueOf(orderId.toString()) : null;
        } catch (Exception e) {
            System.err.println("获取司机当前订单失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 设置司机当前执行的订单ID
     */
    public void setDriverCurrentOrder(Long driverId, Long orderId) {
        try {
            String currentOrderKey = "driver_current_order:" + driverId;
            if (orderId != null) {
                redisTemplate.opsForValue().set(currentOrderKey, orderId.toString(), 24, TimeUnit.HOURS);
                System.out.println("已设置司机 " + driverId + " 当前订单: " + orderId);
            } else {
                redisTemplate.delete(currentOrderKey);
                System.out.println("已清除司机 " + driverId + " 当前订单");
            }
        } catch (Exception e) {
            System.err.println("设置司机当前订单失败: " + e.getMessage());
        }
    }

    /**
     * 获取司机位置信息
     */
    public Map<String, Object> getDriverLocation(Long driverId) {
        try {
            // 从GEO获取位置
            List<org.springframework.data.geo.Point> positions = redisTemplate.opsForGeo()
                .position(DRIVER_GEO_KEY, driverId.toString());
            
            if (positions != null && !positions.isEmpty() && positions.get(0) != null) {
                org.springframework.data.geo.Point point = positions.get(0);
                Map<String, Object> location = new HashMap<>();
                location.put("longitude", point.getX());
                location.put("latitude", point.getY());
                location.put("timestamp", System.currentTimeMillis());
                return location;
            }
            
            return null;
        } catch (Exception e) {
            System.err.println("获取司机位置失败: " + e.getMessage());
            return null;
        }
    }
}