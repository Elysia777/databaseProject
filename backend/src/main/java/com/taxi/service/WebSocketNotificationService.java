package com.taxi.service;

import com.taxi.entity.Order;
import com.taxi.entity.Driver;
import com.taxi.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
    
    @Autowired
    private DriverRedisService driverRedisService;

    /**
     * 通知司机有新订单
     */
    public void notifyDriverNewOrder(Long driverId, Order order, double distance) {
        try {
            System.out.println("=== 准备通知司机新订单 ===");
            System.out.println("司机ID: " + driverId);
            
            // 首先检查司机是否真的在线
            if (!driverRedisService.isDriverOnlineAndFree(driverId)) {
                System.out.println("❌ 司机 " + driverId + " 不在线或不空闲，跳过通知");
                return;
            }
            
            System.out.println("✅ 司机 " + driverId + " 在线且空闲，继续发送通知");
            System.out.println("订单详情:");
            System.out.println("  订单ID: " + order.getId());
            System.out.println("  订单号: " + order.getOrderNumber());
            System.out.println("  上车地址: " + order.getPickupAddress());
            System.out.println("  目的地地址: " + order.getDestinationAddress());
            System.out.println("  距离: " + distance + " 米");
            System.out.println("  预估费用: " + order.getEstimatedFare());
            
            // 数据验证和处理
            String orderIdStr = order.getId() != null ? order.getId().toString() : "unknown";
            String orderNumber = order.getOrderNumber() != null ? order.getOrderNumber() : "未知订单号";
            String pickupAddress = order.getPickupAddress() != null ? order.getPickupAddress() : "未知上车地址";
            String destinationAddress = order.getDestinationAddress() != null ? order.getDestinationAddress() : "未知目的地";
            String distanceStr = String.format("%.2f", distance / 1000); // 转换为公里
            String estimatedFare = order.getEstimatedFare() != null ? order.getEstimatedFare().toString() : "待计算";
            
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "NEW_ORDER");
            notification.put("orderId", orderIdStr);
            notification.put("orderNumber", orderNumber);
            notification.put("pickupAddress", pickupAddress);
            notification.put("destinationAddress", destinationAddress);
            notification.put("distance", distanceStr);
            notification.put("estimatedFare", estimatedFare);
            notification.put("timestamp", System.currentTimeMillis());
            
            String driverIdStr = driverId.toString();
            String destination = "/queue/orders";
            
            System.out.println("发送的通知数据:");
            System.out.println("  " + notification);
            System.out.println("  目标: /user/" + driverIdStr + destination);
            
            // 发送给特定司机
            messagingTemplate.convertAndSendToUser(
                driverIdStr, 
                destination, 
                notification
            );
            
            System.out.println("✅ 已通过WebSocket通知司机 " + driverId + " 新订单 " + order.getId());
            
        } catch (Exception e) {
            System.err.println("❌ WebSocket通知司机失败: " + e.getMessage());
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
     * 通知乘客司机已接单（包含司机详细信息）
     */
    public void notifyPassengerOrderAssigned(Long passengerId, Order order, Driver driver, User driverUser) {
        try {
            System.out.println("=== 通知乘客司机已接单 ===");
            System.out.println("乘客ID: " + passengerId);
            System.out.println("司机ID: " + driver.getId());
            System.out.println("司机姓名: " + (driverUser != null ? driverUser.getRealName() : "未知"));
            
            // 获取司机当前位置
            Map<String, Object> driverLocation = driverRedisService.getDriverLocation(driver.getId());
            
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "ORDER_ASSIGNED");
            
            // 订单信息
            Map<String, Object> orderInfo = new HashMap<>();
            orderInfo.put("id", order.getId());
            orderInfo.put("orderNumber", order.getOrderNumber());
            orderInfo.put("status", "ASSIGNED");
            orderInfo.put("pickupAddress", order.getPickupAddress());
            orderInfo.put("destinationAddress", order.getDestinationAddress());
            orderInfo.put("pickupLatitude", order.getPickupLatitude());
            orderInfo.put("pickupLongitude", order.getPickupLongitude());
            orderInfo.put("destinationLatitude", order.getDestinationLatitude());
            orderInfo.put("destinationLongitude", order.getDestinationLongitude());
            orderInfo.put("estimatedFare", order.getEstimatedFare());
            notification.put("order", orderInfo);
            
            // 司机信息 - 使用Driver和User的信息
            Map<String, Object> driverInfo = new HashMap<>();
            driverInfo.put("id", driver.getId());
            driverInfo.put("driverId", driver.getId());
            
            // 从User表获取基本信息
            if (driverUser != null) {
                driverInfo.put("name", driverUser.getRealName());
                driverInfo.put("driverName", driverUser.getRealName());
                driverInfo.put("phone", driverUser.getPhone());
                driverInfo.put("phoneNumber", driverUser.getPhone());
                driverInfo.put("avatar", driverUser.getAvatar());
            } else {
                driverInfo.put("name", "司机");
                driverInfo.put("driverName", "司机");
                driverInfo.put("phone", "");
                driverInfo.put("phoneNumber", "");
                driverInfo.put("avatar", null);
            }
            
            // 从Driver表获取专业信息
            driverInfo.put("rating", driver.getRating() != null ? driver.getRating() : BigDecimal.valueOf(5.0));
            driverInfo.put("vehicleInfo", "车牌号"); // 暂时使用默认值
            driverInfo.put("carModel", "车辆信息"); // 暂时使用默认值
            
            // 添加司机位置信息
            if (driverLocation != null) {
                driverInfo.put("latitude", driverLocation.get("latitude"));
                driverInfo.put("longitude", driverLocation.get("longitude"));
                System.out.println("司机位置: " + driverLocation.get("longitude") + "," + driverLocation.get("latitude"));
            } else {
                System.out.println("⚠️ 未找到司机位置信息，使用司机表中的位置");
                // 使用司机表中的位置作为备用
                if (driver.getCurrentLatitude() != null && driver.getCurrentLongitude() != null) {
                    driverInfo.put("latitude", driver.getCurrentLatitude());
                    driverInfo.put("longitude", driver.getCurrentLongitude());
                } else {
                    driverInfo.put("latitude", null);
                    driverInfo.put("longitude", null);
                }
            }
            
            notification.put("driver", driverInfo);
            notification.put("timestamp", System.currentTimeMillis());
            
            // 发送给特定乘客的订单队列
            messagingTemplate.convertAndSendToUser(
                passengerId.toString(), 
                "/queue/orders", 
                notification
            );
            
            System.out.println("✅ 已通过WebSocket通知乘客司机接单信息");
            
        } catch (Exception e) {
            System.err.println("❌ WebSocket通知乘客司机接单失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 推送司机位置更新给乘客
     */
    public void pushDriverLocationToPassenger(Long passengerId, Long driverId, Double latitude, Double longitude) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "DRIVER_LOCATION");
            notification.put("driverId", driverId);
            notification.put("latitude", latitude);
            notification.put("longitude", longitude);
            notification.put("timestamp", System.currentTimeMillis());
            
            // 发送给特定乘客的订单队列
            messagingTemplate.convertAndSendToUser(
                passengerId.toString(), 
                "/queue/orders", 
                notification
            );
            
            System.out.println("✅ 已推送司机 " + driverId + " 位置更新给乘客 " + passengerId);
            
        } catch (Exception e) {
            System.err.println("❌ 推送司机位置失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 通知乘客订单状态变化
     */
    public void notifyPassengerOrderStatusChange(Long passengerId, Long orderId, String status, String message) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "ORDER_STATUS_CHANGE");
            notification.put("orderId", orderId);
            notification.put("status", status);
            notification.put("message", message);
            notification.put("timestamp", System.currentTimeMillis());
            
            // 发送给特定乘客的订单队列
            messagingTemplate.convertAndSendToUser(
                passengerId.toString(), 
                "/queue/orders", 
                notification
            );
            
            System.out.println("✅ 已通知乘客 " + passengerId + " 订单状态变化: " + status);
            
        } catch (Exception e) {
            System.err.println("❌ 通知乘客订单状态变化失败: " + e.getMessage());
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