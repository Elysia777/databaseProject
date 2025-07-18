package com.taxi.service;

import com.taxi.entity.Order;
import com.taxi.mapper.OrderMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Redis + RabbitMQ 混合架构的订单队列消费者
 * 处理订单等待队列中的消息
 */
@Component
public class OrderQueueConsumer {

    @Autowired
    private OrderMapper orderMapper;
    
    @Autowired
    private OrderDispatchService orderDispatchService;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 处理订单等待队列中的订单
     * 从Redis GEO查找附近司机并通知
     */
    @RabbitListener(queues = "order_waiting_queue")
    public void handleOrderWaiting(Long orderId) {
        System.out.println("=== 处理等待队列中的订单: " + orderId + " ===");
        
        try {
            // 检查订单是否存在且状态为PENDING
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                System.out.println("订单不存在: " + orderId);
                return;
            }
            
            if (!"PENDING".equals(order.getStatus())) {
                System.out.println("订单状态已变更: " + order.getStatus() + "，跳过处理");
                return;
            }
            
            // 使用OrderDispatchService分配订单
            orderDispatchService.dispatchOrder(orderId);
            
        } catch (Exception e) {
            System.err.println("处理等待队列订单失败: " + e.getMessage());
            e.printStackTrace();
            
            // 处理失败，可以考虑重试或记录错误
            handleOrderDispatchFailure(orderId, e);
        }
    }

    /**
     * 处理订单状态更新消息
     */
    @RabbitListener(queues = "order_status_queue")
    public void handleOrderStatusUpdate(Object message) {
        System.out.println("=== 处理订单状态更新: " + message + " ===");
        
        try {
            // 这里可以处理订单状态变更的后续业务
            // 比如通知相关系统、更新统计数据等
            System.out.println("订单状态更新处理完成");
            
        } catch (Exception e) {
            System.err.println("处理订单状态更新失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 处理司机状态更新消息
     */
    @RabbitListener(queues = "driver_status_queue")
    public void handleDriverStatusUpdate(Object message) {
        System.out.println("=== 处理司机状态更新: " + message + " ===");
        
        try {
            // 这里可以处理司机状态变更的后续业务
            // 比如更新统计数据、触发其他业务逻辑等
            System.out.println("司机状态更新处理完成");
            
        } catch (Exception e) {
            System.err.println("处理司机状态更新失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 处理订单分配失败的情况
     */
    private void handleOrderDispatchFailure(Long orderId, Exception e) {
        try {
            System.out.println("订单 " + orderId + " 分配失败，准备重试");
            
            // 检查是否需要重试
            Order order = orderMapper.selectById(orderId);
            if (order != null && "PENDING".equals(order.getStatus())) {
                
                // 可以根据失败原因决定重试策略
                if (shouldRetry(e)) {
                    // 发送到延迟重试队列
                    rabbitTemplate.convertAndSend("order_retry_queue", orderId);
                    System.out.println("订单 " + orderId + " 已发送到重试队列");
                } else {
                    // 扩大搜索范围重试
                    orderDispatchService.retryDispatchWithLargerRadius(orderId);
                }
            }
            
        } catch (Exception ex) {
            System.err.println("处理订单分配失败时出错: " + ex.getMessage());
        }
    }

    /**
     * 判断是否应该重试
     */
    private boolean shouldRetry(Exception e) {
        // 这里可以根据异常类型判断是否需要重试
        // 比如网络异常可以重试，业务逻辑异常不重试
        return e instanceof RuntimeException;
    }
}