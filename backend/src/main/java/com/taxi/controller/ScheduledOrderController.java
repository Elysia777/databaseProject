package com.taxi.controller;

import com.taxi.common.Result;
import com.taxi.entity.Order;
import com.taxi.service.ScheduledOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 预约单控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class ScheduledOrderController {

    @Autowired
    private ScheduledOrderService scheduledOrderService;

    /**
     * 创建预约单
     */
    @PostMapping("/scheduled")
    public Result<Order> createScheduledOrder(@RequestBody CreateScheduledOrderRequest request) {
        try {
            log.info("=== 创建预约单请求 ===");
            log.info("接收到的请求数据: {}", request);
            log.info("预估距离: {}", request.getEstimatedDistance());
            log.info("预估时长: {}", request.getEstimatedDuration());
            log.info("预估费用: {}", request.getEstimatedFare());
            
            // 构建订单对象
            Order order = new Order();
            order.setOrderNumber("SCH" + System.currentTimeMillis());
            order.setPassengerId(request.getPassengerId());
            order.setOrderType("RESERVATION");
            order.setStatus("PENDING");
            order.setPickupAddress(request.getPickupAddress());
            order.setPickupLatitude(request.getPickupLatitude());
            order.setPickupLongitude(request.getPickupLongitude());
            order.setDestinationAddress(request.getDestinationAddress());
            order.setDestinationLatitude(request.getDestinationLatitude());
            order.setDestinationLongitude(request.getDestinationLongitude());
            order.setScheduledTime(request.getScheduledTime());
            // 设置距离、时长和费用信息
            order.setEstimatedDistance(request.getEstimatedDistance());
            order.setEstimatedDuration(request.getEstimatedDuration());
            order.setEstimatedFare(request.getEstimatedFare());
            
            log.info("设置到Order对象的数据:");
            log.info("  预估距离: {}", order.getEstimatedDistance());
            log.info("  预估时长: {}", order.getEstimatedDuration());
            log.info("  预估费用: {}", order.getEstimatedFare());
            order.setPaymentMethod("WECHAT");
            order.setPaymentStatus("UNPAID");
            
            Order createdOrder = scheduledOrderService.createScheduledOrder(order);
            
            log.info("预约单创建成功: orderId={}", createdOrder.getId());
            return Result.success(createdOrder);
            
        } catch (Exception e) {
            log.error("创建预约单失败", e);
            return Result.error("创建预约单失败: " + e.getMessage());
        }
    }

    /**
     * 获取乘客的预约单列表
     */
    @GetMapping("/passenger/{passengerId}/scheduled")
    public Result<List<Order>> getPassengerScheduledOrders(@PathVariable Long passengerId) {
        try {
            List<Order> orders = scheduledOrderService.getPassengerScheduledOrders(passengerId);
            return Result.success(orders);
        } catch (Exception e) {
            log.error("获取乘客预约单列表失败: passengerId={}", passengerId, e);
            return Result.error("获取预约单列表失败: " + e.getMessage());
        }
    }

    /**
     * 取消预约单
     */
    @PostMapping("/{orderId}/cancel-scheduled")
    public Result<String> cancelScheduledOrder(@PathVariable Long orderId) {
        try {
            scheduledOrderService.cancelScheduledOrder(orderId, "乘客取消");
            return Result.success("预约单已取消");
        } catch (Exception e) {
            log.error("取消预约单失败: orderId={}", orderId, e);
            return Result.error("取消预约单失败: " + e.getMessage());
        }
    }

    /**
     * 检查乘客是否有进行中的订单
     */
    @GetMapping("/passenger/{passengerId}/has-active")
    public Result<Boolean> hasActiveOrder(@PathVariable Long passengerId) {
        try {
            boolean hasActive = scheduledOrderService.hasActiveOrder(passengerId);
            return Result.success(hasActive);
        } catch (Exception e) {
            log.error("检查乘客活跃订单失败: passengerId={}", passengerId, e);
            return Result.error("检查失败: " + e.getMessage());
        }
    }

    /**
     * 创建预约单请求对象
     */
    public static class CreateScheduledOrderRequest {
        private Long passengerId;
        private String pickupAddress;
        private java.math.BigDecimal pickupLatitude;
        private java.math.BigDecimal pickupLongitude;
        private String destinationAddress;
        private java.math.BigDecimal destinationLatitude;
        private java.math.BigDecimal destinationLongitude;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime scheduledTime;
        private java.math.BigDecimal estimatedDistance;
        private Integer estimatedDuration;
        private java.math.BigDecimal estimatedFare;

        // Getters and Setters
        public Long getPassengerId() { return passengerId; }
        public void setPassengerId(Long passengerId) { this.passengerId = passengerId; }

        public String getPickupAddress() { return pickupAddress; }
        public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }

        public java.math.BigDecimal getPickupLatitude() { return pickupLatitude; }
        public void setPickupLatitude(java.math.BigDecimal pickupLatitude) { this.pickupLatitude = pickupLatitude; }

        public java.math.BigDecimal getPickupLongitude() { return pickupLongitude; }
        public void setPickupLongitude(java.math.BigDecimal pickupLongitude) { this.pickupLongitude = pickupLongitude; }

        public String getDestinationAddress() { return destinationAddress; }
        public void setDestinationAddress(String destinationAddress) { this.destinationAddress = destinationAddress; }

        public java.math.BigDecimal getDestinationLatitude() { return destinationLatitude; }
        public void setDestinationLatitude(java.math.BigDecimal destinationLatitude) { this.destinationLatitude = destinationLatitude; }

        public java.math.BigDecimal getDestinationLongitude() { return destinationLongitude; }
        public void setDestinationLongitude(java.math.BigDecimal destinationLongitude) { this.destinationLongitude = destinationLongitude; }

        public LocalDateTime getScheduledTime() { return scheduledTime; }
        public void setScheduledTime(LocalDateTime scheduledTime) { this.scheduledTime = scheduledTime; }

        public java.math.BigDecimal getEstimatedDistance() { return estimatedDistance; }
        public void setEstimatedDistance(java.math.BigDecimal estimatedDistance) { this.estimatedDistance = estimatedDistance; }

        public Integer getEstimatedDuration() { return estimatedDuration; }
        public void setEstimatedDuration(Integer estimatedDuration) { this.estimatedDuration = estimatedDuration; }

        public java.math.BigDecimal getEstimatedFare() { return estimatedFare; }
        public void setEstimatedFare(java.math.BigDecimal estimatedFare) { this.estimatedFare = estimatedFare; }

        @Override
        public String toString() {
            return "CreateScheduledOrderRequest{" +
                    "passengerId=" + passengerId +
                    ", pickupAddress='" + pickupAddress + '\'' +
                    ", destinationAddress='" + destinationAddress + '\'' +
                    ", scheduledTime=" + scheduledTime +
                    ", estimatedFare=" + estimatedFare +
                    '}';
        }
    }
}