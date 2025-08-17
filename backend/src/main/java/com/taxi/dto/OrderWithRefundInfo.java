package com.taxi.dto;

import com.taxi.entity.Order;

/**
 * 包含退款信息的订单DTO
 */
public class OrderWithRefundInfo extends Order {
    private Double refundAmount;
    private Boolean hasRefund;

    public OrderWithRefundInfo() {
        super();
    }

    public OrderWithRefundInfo(Order order) {
        // 复制Order的所有字段
        this.setId(order.getId());
        this.setOrderNumber(order.getOrderNumber());
        this.setPassengerId(order.getPassengerId());
        this.setDriverId(order.getDriverId());
        this.setVehicleId(order.getVehicleId());
        this.setOrderType(order.getOrderType());
        this.setStatus(order.getStatus());
        this.setPickupAddress(order.getPickupAddress());
        this.setPickupLatitude(order.getPickupLatitude());
        this.setPickupLongitude(order.getPickupLongitude());
        this.setDestinationAddress(order.getDestinationAddress());
        this.setDestinationLatitude(order.getDestinationLatitude());
        this.setDestinationLongitude(order.getDestinationLongitude());
        this.setEstimatedDistance(order.getEstimatedDistance());
        this.setEstimatedDuration(order.getEstimatedDuration());
        this.setEstimatedFare(order.getEstimatedFare());
        this.setActualDistance(order.getActualDistance());
        this.setActualDuration(order.getActualDuration());
        this.setActualFare(order.getActualFare());
        this.setServiceFee(order.getServiceFee());
        this.setTotalFare(order.getTotalFare());
        this.setPaymentMethod(order.getPaymentMethod());
        this.setPaymentStatus(order.getPaymentStatus());
        this.setScheduledTime(order.getScheduledTime());
        this.setPickupTime(order.getPickupTime());
        this.setCompletionTime(order.getCompletionTime());
        this.setCancelReason(order.getCancelReason());
        this.setCreatedAt(order.getCreatedAt());
        this.setUpdatedAt(order.getUpdatedAt());
        
        // 设置默认值
        this.refundAmount = 0.0;
        this.hasRefund = false;
    }

    public Double getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(Double refundAmount) {
        this.refundAmount = refundAmount;
        this.hasRefund = refundAmount != null && refundAmount > 0;
    }

    public Boolean getHasRefund() {
        return hasRefund;
    }

    public void setHasRefund(Boolean hasRefund) {
        this.hasRefund = hasRefund;
    }
}