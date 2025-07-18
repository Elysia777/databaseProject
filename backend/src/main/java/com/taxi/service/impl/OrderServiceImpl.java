package com.taxi.service.impl;

import com.taxi.entity.Order;
import com.taxi.mapper.OrderMapper;
import com.taxi.service.OrderService;
import com.taxi.service.OrderDispatchService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    private OrderDispatchService orderDispatchService;

    @Override
    public void createOrder(Order order) {
        System.out.println("=== 开始创建订单 ===");
        
        // 保存订单到数据库，状态为PENDING
        order.setStatus("PENDING");
        orderMapper.insert(order);
        System.out.println("订单已保存到数据库，订单ID: " + order.getId());
        
        // 使用Redis + RabbitMQ混合架构：订单发送到等待队列
        try {
            // 1. 立即尝试分配（如果有在线司机）
            orderDispatchService.dispatchOrder(order.getId());
            
            // 2. 同时发送到等待队列（确保后续上线的司机也能看到）
            rabbitTemplate.convertAndSend("order_waiting_queue", order.getId());
            
            System.out.println("订单已创建并发送到分配系统，订单ID: " + order.getId());
        } catch (Exception e) {
            System.err.println("订单分配失败: " + e.getMessage());
            e.printStackTrace();
            
            // 即使立即分配失败，也要确保订单进入等待队列
            try {
                rabbitTemplate.convertAndSend("order_waiting_queue", order.getId());
                System.out.println("订单已发送到等待队列作为备用");
            } catch (Exception queueException) {
                System.err.println("发送到等待队列也失败: " + queueException.getMessage());
                throw queueException;
            }
        }
        
        System.out.println("=== 订单创建完成 ===");
    }
} 