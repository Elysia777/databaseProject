package com.taxi.dto;

import lombok.Data;

/**
 * 订单列表请求DTO
 */
@Data
public class OrderListRequest {
    
    /**
     * 当前页码
     */
    private Integer page = 1;
    
    /**
     * 每页大小
     */
    private Integer size = 10;
    
    /**
     * 乘客ID
     */
    private Long passengerId;
    
    /**
     * 司机ID
     */
    private Long driverId;
    
    /**
     * 订单类型（1-实时单，2-预约单）
     */
    private Integer orderType;
    
    /**
     * 订单状态（1-待派单，2-已派单，3-已接单，4-已上车，5-已完成，6-已取消）
     */
    private Integer orderStatus;
    
    /**
     * 支付状态（1-未支付，2-已支付）
     */
    private Integer paymentStatus;
    
    /**
     * 开始时间
     */
    private String startTime;
    
    /**
     * 结束时间
     */
    private String endTime;
    
    /**
     * 关键词搜索（订单号、乘客姓名、司机姓名、车牌号）
     */
    private String keyword;
} 