package com.taxi.config;

import com.taxi.service.DriverLocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 定时任务配置
 * 用于管理TCP连接心跳检查等定时任务
 */
@Configuration
@EnableScheduling
public class ScheduleConfig {

    @Autowired
    private DriverLocationService driverLocationService;

    /**
     * 每30秒检查一次TCP连接心跳
     * 清理超时的连接
     */
    @Scheduled(fixedRate = 30000)
    public void checkDriverConnections() {
        try {
            driverLocationService.checkHeartbeat();
        } catch (Exception e) {
            System.err.println("检查司机连接心跳失败: " + e.getMessage());
        }
    }

    /**
     * 每5分钟输出一次系统统计信息
     */
    @Scheduled(fixedRate = 300000)
    public void printSystemStats() {
        try {
            int activeConnections = driverLocationService.getActiveConnectionCount();
            System.out.println("=== 系统统计 ===");
            System.out.println("活跃TCP连接数: " + activeConnections);
            System.out.println("时间: " + java.time.LocalDateTime.now());
            System.out.println("===============");
        } catch (Exception e) {
            System.err.println("输出系统统计失败: " + e.getMessage());
        }
    }
}