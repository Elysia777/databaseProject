package com.taxi.service;

import org.springframework.stereotype.Component;

/**
 * 旧的订单分配消费者 - 已废弃
 * 现在使用 DriverListenerService 实现异步监听模式
 * 
 * 功能已迁移到：
 * - DriverListenerService: 司机动态监听服务
 * - DriverNotificationConsumer: 司机通知消费者
 */
@Component
public class OrderDispatchConsumer {
    
    // 此类已废弃，功能已迁移到新的异步监听架构
    // 保留空类避免Spring启动时的依赖问题
    
} 