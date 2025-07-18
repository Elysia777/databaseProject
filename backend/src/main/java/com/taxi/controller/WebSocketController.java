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
            String driverId = (String) message.get("driverId");
            System.out.println("司机 " + driverId + " 已连接WebSocket");
            
            // 将司机ID存储到session中
            headerAccessor.getSessionAttributes().put("driverId", driverId);
            headerAccessor.getSessionAttributes().put("userType", "DRIVER");
            
            // 发送连接成功消息
            messagingTemplate.convertAndSendToUser(
                driverId, 
                "/queue/connection", 
                Map.of("status", "connected", "message", "WebSocket连接成功")
            );
            
        } catch (Exception e) {
            System.err.println("处理司机WebSocket连接失败: " + e.getMessage());
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