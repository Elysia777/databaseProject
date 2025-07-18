package com.taxi.service;

import com.taxi.entity.Order;
import com.taxi.entity.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket实时通知服务
 * 通过WebSocket向司机和乘客发送实时通知
 */
@Service
public class WebSocketNotificationService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 通知司机有新订单
     */
    public void notifyDriverNewOrder(Long driverId, Order order, double distance) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "NEW_ORDER");
            notification.put("orderId", order.getId());
            notification.put("orderNumber", order.getOrderNumber());
            notification.put("pickupAddress", order.getPickupAddress());
            notification.put("destinationAddress", order.getDestinationAddress());
            notification.put("distance", String.format("%.2f", distance / 1000)); // 转换为公里
            notification.put("estimatedFare", order.getEstimatedFare());
            notification.put("timestamp", System.currentTimeMillis());
            
            // 发送给特定司机
            messagingTemplate.convertAndSendToUser(
                driverId.toString(), 
                "/queue/orders", 
                notification
            );
            
            System.out.println("已通过WebSocket通知司机 " + driverId + " 新订单 " + order.getId());
            
        } catch (Exception e) {
            System.err.println("WebSocket通知司机失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 通知乘客订单状态更新
     */
    public void notifyPassengerOrderUpdate(Long passengerId, Order order, String message) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "ORDER_UPDATE");
            notification.put("orderId", order.getId());
            notification.put("orderNumber", order.getOrderNumber());
            notification.put("status", order.getStatus());
            notification.put("message", message);
            notification.put("driverId", order.getDriverId());
            notification.put("timestamp", System.currentTimeMillis());
            
            // 发送给特定乘客
            messagingTemplate.convertAndSendToUser(
                passengerId.toString(), 
                "/queue/notifications", 
                notification
            );
            
            System.out.println("已通过WebSocket通知乘客 " + passengerId + " 订单更新");
            
        } catch (Exception e) {
            System.err.println("WebSocket通知乘客失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 通知司机订单被取消
     */
    public void notifyDriverOrderCancelled(Long driverId, Order order) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "ORDER_CANCELLED");
            notification.put("orderId", order.getId());
            notification.put("orderNumber", order.getOrderNumber());
            notification.put("message", "订单已被取消");
            notification.put("timestamp", System.currentTimeMillis());
            
            messagingTemplate.convertAndSendToUser(
                driverId.toString(), 
                "/queue/notifications", 
                notification
            );
            
            System.out.println("已通过WebSocket通知司机 " + driverId + " 订单取消");
            
        } catch (Exception e) {
            System.err.println("WebSocket通知司机订单取消失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 广播系统消息
     */
    public void broadcastSystemMessage(String message) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "SYSTEM_MESSAGE");
            notification.put("message", message);
            notification.put("timestamp", System.currentTimeMillis());
            
            // 广播给所有连接的用户
            messagingTemplate.convertAndSend("/topic/system", notification);
            
            System.out.println("已广播系统消息: " + message);
            
        } catch (Exception e) {
            System.err.println("广播系统消息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 通知司机位置更新请求
     */
    public void requestDriverLocationUpdate(Long driverId) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "LOCATION_UPDATE_REQUEST");
            notification.put("message", "请更新您的位置信息");
            notification.put("timestamp", System.currentTimeMillis());
            
            messagingTemplate.convertAndSendToUser(
                driverId.toString(), 
                "/queue/location", 
                notification
            );
            
        } catch (Exception e) {
            System.err.println("请求司机位置更新失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}