package com.taxi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxi.dto.OrderNotificationDto;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 司机通知消费者
 * 处理发送给司机的通知消息
 */
@Component
public class DriverNotificationConsumer {

    @Autowired
    private ObjectMapper objectMapper;

    @RabbitListener(queues = "driver_notification_queue")
    public void handleDriverNotification(java.util.Map<String, Object> notification) {
        System.out.println("=== 司机通知 ===");
        System.out.println("收到Map通知，内容: " + notification);
        
        try {
            // 直接处理Map类型的通知
            handleMapNotification(notification);
            
        } catch (Exception e) {
            System.err.println("处理司机通知失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 处理JSON内容
     */
    private void handleJsonContent(String jsonContent) {
        try {
            // 尝试解析为OrderNotificationDto
            OrderNotificationDto orderNotification = objectMapper.readValue(jsonContent, OrderNotificationDto.class);
            handleOrderNotification(orderNotification);
            
        } catch (Exception e) {
            System.err.println("解析JSON为OrderNotificationDto失败: " + e.getMessage());
            
            // 尝试解析为通用Map
            try {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> messageMap = objectMapper.readValue(jsonContent, java.util.Map.class);
                handleMapNotification(messageMap);
                
            } catch (Exception ex) {
                System.err.println("解析JSON为Map也失败: " + ex.getMessage());
                System.err.println("原始JSON内容: " + jsonContent);
                
                // 作为字符串处理
                handleStringNotification(jsonContent);
            }
        }
    }
    
    /**
     * 处理Map类型的通知
     */
    private void handleMapNotification(java.util.Map<String, Object> messageMap) {
        System.out.println("=== 处理Map通知 ===");
        System.out.println("消息内容: " + messageMap);
        
        // 尝试提取订单信息
        Object orderIdObj = messageMap.get("orderId");
        Object orderNumberObj = messageMap.get("orderNumber");
        Object driverIdObj = messageMap.get("driverId");
        
        if (orderIdObj != null && driverIdObj != null) {
            System.out.println("检测到订单通知:");
            System.out.println("  订单ID: " + orderIdObj);
            System.out.println("  订单号: " + orderNumberObj);
            System.out.println("  司机ID: " + driverIdObj);
            System.out.println("  上车地址: " + messageMap.get("pickupAddress"));
            System.out.println("  目的地: " + messageMap.get("destinationAddress"));
            System.out.println("  距离: " + messageMap.get("distance"));
            System.out.println("  预估费用: " + messageMap.get("estimatedFare"));
            
            // 构建通知消息
            String message = String.format(
                "新订单通知：从%s到%s",
                messageMap.get("pickupAddress"),
                messageMap.get("destinationAddress")
            );
            
            System.out.println("通知消息: " + message);
            
            // 发送通知
            Long driverId = Long.valueOf(driverIdObj.toString());
            sendNotificationToDriverSimple(driverId, message, messageMap);
        } else {
            handleGenericNotification(messageMap);
        }
    }
    
    /**
     * 发送简单通知给司机
     */
    private void sendNotificationToDriverSimple(Long driverId, String message, java.util.Map<String, Object> orderInfo) {
        System.out.println("=== 发送通知给司机 " + driverId + " ===");
        System.out.println("消息: " + message);
        System.out.println("订单信息: " + orderInfo);
        System.out.println("通知发送完成");
    }
    
    /**
     * 处理字符串类型的通知
     */
    private void handleStringNotification(String notification) {
        System.out.println("处理字符串通知: " + notification);
        
        // 这里可以集成：
        // 1. WebSocket推送给司机端
        // 2. 短信通知
        // 3. App推送通知
        // 4. 邮件通知等
    }
    
    /**
     * 处理对象类型的通知（如订单分配）
     */
    private void handleObjectNotification(Object notification) {
        System.out.println("处理对象通知: " + notification);
        
        try {
            // 尝试将对象转换为JSON字符串，然后解析为OrderNotificationDto
            String jsonString;
            if (notification instanceof String) {
                jsonString = (String) notification;
            } else {
                jsonString = objectMapper.writeValueAsString(notification);
            }
            
            System.out.println("JSON字符串: " + jsonString);
            
            // 尝试解析为订单通知DTO
            OrderNotificationDto orderNotification = objectMapper.readValue(jsonString, OrderNotificationDto.class);
            handleOrderNotification(orderNotification);
            
        } catch (Exception e) {
            System.err.println("解析订单通知失败: " + e.getMessage());
            System.err.println("原始通知内容: " + notification);
            
            // 如果解析失败，作为普通通知处理
            handleGenericNotification(notification);
        }
    }
    
    /**
     * 处理订单通知
     */
    private void handleOrderNotification(OrderNotificationDto orderNotification) {
        System.out.println("=== 处理订单通知 ===");
        System.out.println("订单ID: " + orderNotification.getOrderId());
        System.out.println("订单号: " + orderNotification.getOrderNumber());
        System.out.println("司机ID: " + orderNotification.getDriverId());
        System.out.println("上车地址: " + orderNotification.getPickupAddress());
        System.out.println("目的地: " + orderNotification.getDestinationAddress());
        System.out.println("距离: " + orderNotification.getDistance() + "公里");
        System.out.println("预估费用: " + orderNotification.getEstimatedFare());
        
        // 这里可以实现具体的通知逻辑：
        // 1. 通过WebSocket推送给司机端
        // 2. 发送短信通知
        // 3. App推送通知
        // 4. 更新司机端界面等
        
        // 示例：构建通知消息
        String message = String.format(
            "新订单通知：从%s到%s，距离%.1f公里，预估费用%.2f元",
            orderNotification.getPickupAddress(),
            orderNotification.getDestinationAddress(),
            orderNotification.getDistance(),
            orderNotification.getEstimatedFare()
        );
        
        System.out.println("通知消息: " + message);
        
        // TODO: 实现实际的通知发送逻辑
        sendNotificationToDriver(orderNotification.getDriverId(), message, orderNotification);
    }
    
    /**
     * 处理通用通知
     */
    private void handleGenericNotification(Object notification) {
        System.out.println("=== 处理通用通知 ===");
        System.out.println("通知内容: " + notification);
        
        // 对于无法解析的通知，进行基本处理
        // TODO: 实现通用通知处理逻辑
    }
    
    /**
     * 发送通知给司机
     */
    private void sendNotificationToDriver(Long driverId, String message, OrderNotificationDto orderNotification) {
        System.out.println("=== 发送通知给司机 " + driverId + " ===");
        System.out.println("消息: " + message);
        
        // 这里可以集成多种通知方式：
        
        // 1. WebSocket实时推送
        // webSocketService.sendToDriver(driverId, message, orderNotification);
        
        // 2. 短信通知（紧急情况）
        // smsService.sendOrderNotification(driverId, message);
        
        // 3. App推送通知
        // pushNotificationService.sendToDriver(driverId, "新订单", message);
        
        // 4. 邮件通知（备用）
        // emailService.sendOrderNotification(driverId, orderNotification);
        
        System.out.println("通知发送完成");
    }
}