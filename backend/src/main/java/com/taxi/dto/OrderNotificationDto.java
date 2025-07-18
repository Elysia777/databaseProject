package com.taxi.dto;

import lombok.Data;

/**
 * 订单通知DTO
 * 用于RabbitMQ消息传递
 */
@Data
public class OrderNotificationDto {
    
    /** 订单ID */
    private Long orderId;
    
    /** 订单号 */
    private String orderNumber;
    
    /** 司机ID */
    private Long driverId;
    
    /** 上车地址 */
    private String pickupAddress;
    
    /** 目的地地址 */
    private String destinationAddress;
    
    /** 距离（公里） */
    private Double distance;
    
    /** 预估费用 */
    private Double estimatedFare;
    
    /** 时间戳 */
    private Long timestamp;
    
    /** 通知类型 */
    private String notificationType;
    
    /** 额外信息 */
    private String message;
}