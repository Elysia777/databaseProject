package com.taxi.dto;

import com.taxi.entity.Complaint;

import java.math.BigDecimal;

/**
 * 包含订单信息的投诉DTO
 */
public class ComplaintWithOrderInfo extends Complaint {
    private String orderNumber;
    private String pickupAddress;
    private String destinationAddress;
    private String pickupTime;
    private String completionTime;
    private BigDecimal actualFare;
    private String paymentStatus;

    public ComplaintWithOrderInfo() {
        super();
    }

    public ComplaintWithOrderInfo(Complaint complaint) {
        // 复制Complaint的所有字段
        this.setId(complaint.getId());
        this.setOrderId(complaint.getOrderId());
        this.setComplainantId(complaint.getComplainantId());
        this.setDefendantId(complaint.getDefendantId());
        this.setComplaintType(complaint.getComplaintType());
        this.setTitle(complaint.getTitle());
        this.setDescription(complaint.getDescription());
        this.setEvidenceFiles(complaint.getEvidenceFiles());
        this.setStatus(complaint.getStatus());
        this.setAdminId(complaint.getAdminId());
        this.setResolution(complaint.getResolution());
        this.setRefundAmount(complaint.getRefundAmount());
        this.setResolutionTime(complaint.getResolutionTime());
        this.setCreatedAt(complaint.getCreatedAt());
        this.setUpdatedAt(complaint.getUpdatedAt());
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public void setDestinationAddress(String destinationAddress) {
        this.destinationAddress = destinationAddress;
    }

    public String getPickupTime() {
        return pickupTime;
    }

    public void setPickupTime(String pickupTime) {
        this.pickupTime = pickupTime;
    }

    public String getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(String completionTime) {
        this.completionTime = completionTime;
    }

    public BigDecimal getActualFare() {
        return actualFare;
    }

    public void setActualFare(BigDecimal actualFare) {
        this.actualFare = actualFare;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
}