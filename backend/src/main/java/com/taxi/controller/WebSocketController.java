package com.taxi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * WebSocket控制器
 * 处理客户端WebSocket连接和消息
 */
@Controller
public class WebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 处理司机连接
     */
    @MessageMapping("/driver/connect")
    public void handleDriverConnect(@Payload Map<String, Object> message, SimpMessageHeaderAccessor headerAccessor) {
        try {
            System.out.println("=== 收到司机WebSocket连接请求 ===");
            System.out.println("原始消息: " + message);
            
            // 处理司机ID，支持字符串和数字类型
            Object driverIdObj = message.get("driverId");
            String driverId = driverIdObj != null ? driverIdObj.toString() : null;
            
            if (driverId == null || driverId.isEmpty()) {
                System.err.println("司机ID为空，连接失败");
                return;
            }
            
            System.out.println("司机 " + driverId + " 已连接WebSocket");
            
            // 将司机ID存储到session中
            headerAccessor.getSessionAttributes().put("driverId", driverId);
            headerAccessor.getSessionAttributes().put("userType", "DRIVER");
            
            // 发送连接成功消息
            Map<String, Object> response = Map.of(
                "status", "connected", 
                "message", "WebSocket连接成功，司机ID: " + driverId,
                "timestamp", System.currentTimeMillis()
            );
            
            System.out.println("准备发送连接确认消息到: /user/" + driverId + "/queue/connection");
            System.out.println("消息内容: " + response);
            
            messagingTemplate.convertAndSendToUser(
                driverId, 
                "/queue/connection", 
                response
            );
            
            System.out.println("已向司机 " + driverId + " 发送连接成功消息");
            
            // 同时发送广播消息测试
            Map<String, Object> broadcastMsg = Map.of(
                "type", "BROADCAST_TEST",
                "message", "这是一条广播测试消息",
                "driverId", driverId,
                "timestamp", System.currentTimeMillis()
            );
            
            messagingTemplate.convertAndSend("/topic/test", broadcastMsg);
            System.out.println("已发送广播测试消息");
            
            // 发送一条测试消息到订单队列
            Map<String, Object> testOrder = Map.of(
                "type", "TEST_MESSAGE",
                "message", "这是一条测试消息，确认WebSocket连接正常",
                "timestamp", System.currentTimeMillis()
            );
            
            messagingTemplate.convertAndSendToUser(
                driverId, 
                "/queue/orders", 
                testOrder
            );
            
            System.out.println("已向司机 " + driverId + " 发送测试消息");
            
        } catch (Exception e) {
            System.err.println("处理司机WebSocket连接失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 处理乘客连接
     */
    @MessageMapping("/passenger/connect")
    public void handlePassengerConnect(@Payload Map<String, Object> message, SimpMessageHeaderAccessor headerAccessor) {
        try {
            String passengerId = (String) message.get("passengerId");
            System.out.println("乘客 " + passengerId + " 已连接WebSocket");
            
            // 将乘客ID存储到session中
            headerAccessor.getSessionAttributes().put("passengerId", passengerId);
            headerAccessor.getSessionAttributes().put("userType", "PASSENGER");
            
            // 发送连接成功消息
            messagingTemplate.convertAndSendToUser(
                passengerId, 
                "/queue/connection", 
                Map.of("status", "connected", "message", "WebSocket连接成功")
            );
            
        } catch (Exception e) {
            System.err.println("处理乘客WebSocket连接失败: " + e.getMessage());
        }
    }

    /**
     * 处理司机位置更新
     */
    @MessageMapping("/driver/location")
    public void handleDriverLocationUpdate(@Payload Map<String, Object> message, SimpMessageHeaderAccessor headerAccessor) {
        try {
            String driverId = (String) headerAccessor.getSessionAttributes().get("driverId");
            if (driverId != null) {
                Double latitude = (Double) message.get("latitude");
                Double longitude = (Double) message.get("longitude");
                
                System.out.println("收到司机 " + driverId + " 位置更新: " + latitude + "," + longitude);
                
                // 这里可以调用DriverRedisService更新位置
                // driverRedisService.updateDriverLocation(Long.valueOf(driverId), 
                //     BigDecimal.valueOf(latitude), BigDecimal.valueOf(longitude));
            }
            
        } catch (Exception e) {
            System.err.println("处理司机位置更新失败: " + e.getMessage());
        }
    }

    /**
     * 处理测试消息
     */
    @MessageMapping("/test")
    public void handleTestMessage(@Payload Map<String, Object> message) {
        System.out.println("=== 收到测试消息 ===");
        System.out.println("消息内容: " + message);
        
        // 获取司机ID
        Object driverIdObj = message.get("driverId");
        String driverId = driverIdObj != null ? driverIdObj.toString() : "1";
        
        // 广播测试消息
        messagingTemplate.convertAndSend("/topic/test", Map.of(
            "type", "TEST_RESPONSE",
            "message", "服务器收到测试消息: " + message,
            "timestamp", System.currentTimeMillis()
        ));
        
        // 同时发送用户专用测试消息
        Map<String, Object> userTestMsg = Map.of(
            "type", "USER_TEST_MESSAGE",
            "message", "这是发送给用户专用队列的测试消息",
            "driverId", driverId,
            "timestamp", System.currentTimeMillis()
        );
        
        System.out.println("发送用户专用测试消息到: /user/" + driverId + "/queue/orders");
        System.out.println("消息内容: " + userTestMsg);
        
        messagingTemplate.convertAndSendToUser(
            driverId,
            "/queue/orders",
            userTestMsg
        );
        
        System.out.println("用户专用测试消息发送完成");
    }

    /**
     * 处理司机接单响应
     */
    @MessageMapping("/driver/accept-order")
    public void handleDriverAcceptOrder(@Payload Map<String, Object> message, SimpMessageHeaderAccessor headerAccessor) {
        try {
            String driverId = (String) headerAccessor.getSessionAttributes().get("driverId");
            Long orderId = Long.valueOf(message.get("orderId").toString());
            
            System.out.println("司机 " + driverId + " 通过WebSocket接单: " + orderId);
            
            // 这里可以调用OrderDispatchService处理接单
            // boolean success = orderDispatchService.acceptOrder(orderId, Long.valueOf(driverId));
            
            // 发送接单结果
            messagingTemplate.convertAndSendToUser(
                driverId, 
                "/queue/order-response", 
                Map.of("orderId", orderId, "status", "processing", "message", "正在处理接单请求...")
            );
            
        } catch (Exception e) {
            System.err.println("处理司机接单失败: " + e.getMessage());
        }
    }
}