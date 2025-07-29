// 修复Redis中司机状态的Java代码片段
// 这个代码可以添加到DriverController中作为一个临时修复接口

/**
 * 修复Redis中的司机状态 - 临时修复接口
 */
@PostMapping("/fix-redis-status")
public Result<String> fixRedisDriverStatus() {
    try {
        // 1. 从数据库获取所有可用司机
        List<Driver> availableDrivers = driverMapper.selectList(
            new QueryWrapper<Driver>().eq("status", "AVAILABLE")
        );
        
        if (availableDrivers.isEmpty()) {
            return Result.error("数据库中没有可用司机");
        }
        
        int fixedCount = 0;
        StringBuilder result = new StringBuilder("修复结果:\n");
        
        for (Driver driver : availableDrivers) {
            try {
                // 2. 确保司机有位置信息
                if (driver.getCurrentLatitude() == null || driver.getCurrentLongitude() == null) {
                    // 设置默认位置（大连市中心）
                    driver.setCurrentLatitude(new BigDecimal("39.044237"));
                    driver.setCurrentLongitude(new BigDecimal("121.749849"));
                    driverMapper.updateById(driver);
                    result.append("- 司机").append(driver.getId()).append("设置默认位置\n");
                }
                
                // 3. 重新加入Redis
                driverRedisService.driverGoOnline(driver);
                
                // 4. 验证Redis状态
                boolean isOnline = driverRedisService.isDriverOnlineAndFree(driver.getId());
                Driver redisDriver = driverRedisService.getDriverInfo(driver.getId());
                
                if (isOnline && redisDriver != null) {
                    fixedCount++;
                    result.append("✅ 司机").append(driver.getId()).append("(").append(driver.getName()).append(") 修复成功\n");
                } else {
                    result.append("❌ 司机").append(driver.getId()).append("(").append(driver.getName()).append(") 修复失败\n");
                }
                
            } catch (Exception e) {
                result.append("❌ 司机").append(driver.getId()).append(" 修复异常: ").append(e.getMessage()).append("\n");
            }
        }
        
        result.append("\n总计修复 ").append(fixedCount).append(" 个司机");
        
        return Result.success(result.toString());
        
    } catch (Exception e) {
        return Result.error("修复失败: " + e.getMessage());
    }
}

/**
 * 检查司机状态详情 - 诊断接口
 */
@GetMapping("/{driverId}/status-detail")
public Result<Map<String, Object>> getDriverStatusDetail(@PathVariable Long driverId) {
    Map<String, Object> detail = new HashMap<>();
    
    try {
        // 1. 数据库中的司机信息
        Driver dbDriver = driverMapper.selectById(driverId);
        detail.put("database", dbDriver);
        
        // 2. Redis中的司机信息
        Driver redisDriver = driverRedisService.getDriverInfo(driverId);
        detail.put("redis", redisDriver);
        
        // 3. Redis中的状态信息
        boolean isOnline = driverRedisService.isDriverOnlineAndFree(driverId);
        detail.put("isOnlineAndFree", isOnline);
        
        // 4. 检查GEO位置
        try {
            List<org.springframework.data.geo.Point> positions = redisTemplate.opsForGeo()
                .position("driver:geo", driverId.toString());
            detail.put("geoPosition", positions != null && !positions.isEmpty() ? positions.get(0) : null);
        } catch (Exception e) {
            detail.put("geoPosition", "获取失败: " + e.getMessage());
        }
        
        // 5. 状态一致性检查
        Map<String, Object> consistency = new HashMap<>();
        consistency.put("dbExists", dbDriver != null);
        consistency.put("redisExists", redisDriver != null);
        consistency.put("statusMatch", dbDriver != null && redisDriver != null && 
                       dbDriver.getStatus().equals(redisDriver.getStatus()));
        detail.put("consistency", consistency);
        
        return Result.success(detail);
        
    } catch (Exception e) {
        return Result.error("检查失败: " + e.getMessage());
    }
}