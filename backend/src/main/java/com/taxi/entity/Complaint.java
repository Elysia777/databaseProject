package com.taxi.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 投诉实体类
 */
@Data
public class Complaint {
    private Long id;
    private Long orderId;
    private Long complainantId;
    private Long defendantId;
    private String complaintType;
    private String title;
    private String description;
    private List<String> evidenceFiles;
    private String status;
    private Long adminId;
    private String resolution;
    private Double refundAmount;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime resolutionTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // 关联信息（用于查询时显示）
    private String complainantName;
    private String defendantName;
    private String adminName;
    private String orderNumber;

    // 构造函数
    public Complaint() {}

}