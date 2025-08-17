package com.taxi.dto;

/**
 * 处理投诉请求DTO
 */
public class ProcessComplaintRequest {
    private String status;
    private String resolution;
    private Double refundAmount; // 退款金额

    // 构造函数
    public ProcessComplaintRequest() {}

    // Getter和Setter方法
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public Double getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(Double refundAmount) {
        this.refundAmount = refundAmount;
    }

    @Override
    public String toString() {
        return "ProcessComplaintRequest{" +
                "status='" + status + '\'' +
                ", resolution='" + resolution + '\'' +
                ", refundAmount=" + refundAmount +
                '}';
    }
}