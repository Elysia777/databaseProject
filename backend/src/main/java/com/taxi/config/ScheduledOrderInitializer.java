package com.taxi.config;

import com.taxi.service.ScheduledOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 预约单系统初始化器
 * 系统启动时恢复所有待激活的预约单任务
 */
@Slf4j
@Component
public class ScheduledOrderInitializer implements ApplicationRunner {

    @Autowired
    private ScheduledOrderService scheduledOrderService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("系统启动，开始恢复预约单定时任务...");
        
        try {
            scheduledOrderService.recoverScheduledOrders();
            log.info("预约单定时任务恢复完成");
        } catch (Exception e) {
            log.error("恢复预约单定时任务失败", e);
        }
    }
}