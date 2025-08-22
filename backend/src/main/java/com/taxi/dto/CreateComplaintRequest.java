package com.taxi.dto;

import java.util.List;

/**
 * 创建投诉请求DTO
 */
public class CreateComplaintRequest {
    private Long orderId;
    private Long defendantId;
    private String complaintType;
    private String title;
    private String description;
    private List<String> evidenceFiles;

    // 构造函数
    public CreateComplaintRequest() {}

    // Getter和Setter方法
    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getDefendantId() {
        return defendantId;
    }

    public void setDefendantId(Long defendantId) {
        this.defendantId = defendantId;
    }

    public String getComplaintType() {
        return complaintType;
    }

    public void setComplaintType(String complaintType) {
        this.complaintType = complaintType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getEvidenceFiles() {
        return evidenceFiles;
    }

    public void setEvidenceFiles(List<String> evidenceFiles) {
        this.evidenceFiles = evidenceFiles;
    }

    @Override
    public String toString() {
        return "CreateComplaintRequest{" +
                "orderId=" + orderId +
                ", defendantId=" + defendantId +
                ", complaintType='" + complaintType + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}