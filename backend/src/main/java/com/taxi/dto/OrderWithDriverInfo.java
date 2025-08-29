package com.taxi.dto;

import com.taxi.entity.Order;

/**
 * 包含司机信息的订单DTO
 */
public class OrderWithDriverInfo extends Order {
    private String driverName; // 司机姓名
    private String driverPhone; // 司机手机号

    public OrderWithDriverInfo() {
        super();
    }

    public OrderWithDriverInfo(Order order) {
        // 复制Order的所有字段
        this.setId(order.getId());
        this.setOrderNumber(order.getOrderNumber());
        this.setPassengerId(order.getPassengerId());
        this.setDriverId(order.getDriverId());
        this.setVehicleId(order.getVehicleId());
        this.setOrderType(order.getOrderType());
        this.setStatus(order.getStatus());
        this.setPaymentStatus(order.getPaymentStatus());
        this.setPaymentMethod(order.getPaymentMethod());
        this.setPickupAddress(order.getPickupAddress());
        this.setPickupLatitude(order.getPickupLatitude());
        this.setPickupLongitude(order.getPickupLongitude());
        this.setDestinationAddress(order.getDestinationAddress());
        this.setDestinationLatitude(order.getDestinationLatitude());
        this.setDestinationLongitude(order.getDestinationLongitude());
        this.setEstimatedDistance(order.getEstimatedDistance());
        this.setEstimatedDuration(order.getEstimatedDuration());
        this.setEstimatedFare(order.getEstimatedFare());
        this.setActualFare(order.getActualFare());
        this.setPickupTime(order.getPickupTime());
        this.setCompletionTime(order.getCompletionTime());
        this.setCancelReason(order.getCancelReason());
        this.setCreatedAt(order.getCreatedAt());
        this.setUpdatedAt(order.getUpdatedAt());
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getDriverPhone() {
        return driverPhone;
    }

    public void setDriverPhone(String driverPhone) {
        this.driverPhone = driverPhone;
    }
}
