package com.taxi.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单详情响应DTO
 */
@Data
public class OrderDetailResponse {
    
    /**
     * 订单ID
     */
    private Long orderId;
    
    /**
     * 乘客ID
     */
    private Long passengerId;
    
    /**
     * 乘客姓名
     */
    private String passengerName;
    
    /**
     * 乘客手机号
     */
    private String passengerPhone;
    
    /**
     * 司机ID
     */
    private Long driverId;
    
    /**
     * 司机姓名
     */
    private String driverName;
    
    /**
     * 司机手机号
     */
    private String driverPhone;
    
    /**
     * 车辆ID
     */
    private Long vehicleId;
    
    /**
     * 车牌号
     */
    private String plateNumber;
    
    /**
     * 车辆型号
     */
    private String vehicleModel;
    
    /**
     * 订单类型（1-实时单，2-预约单）
     */
    private Integer orderType;
    
    /**
     * 订单状态（1-待派单，2-已派单，3-已接单，4-已上车，5-已完成，6-已取消）
     */
    private Integer orderStatus;
    
    /**
     * 上车地点
     */
    private String pickupLocation;
    
    /**
     * 上车地点经度
     */
    private Double pickupLongitude;
    
    /**
     * 上车地点纬度
     */
    private Double pickupLatitude;
    
    /**
     * 下车地点
     */
    private String dropoffLocation;
    
    /**
     * 下车地点经度
     */
    private Double dropoffLongitude;
    
    /**
     * 下车地点纬度
     */
    private Double dropoffLatitude;
    
    /**
     * 预约时间
     */
    private LocalDateTime scheduledTime;
    
    /**
     * 实际上车时间
     */
    private LocalDateTime actualPickupTime;
    
    /**
     * 实际下车时间
     */
    private LocalDateTime actualDropoffTime;
    
    /**
     * 行驶距离（公里）
     */
    private BigDecimal distance;
    
    /**
     * 订单金额
     */
    private BigDecimal amount;
    
    /**
     * 支付状态（1-未支付，2-已支付）
     */
    private Integer paymentStatus;
    
    /**
     * 支付方式（1-现金，2-微信，3-支付宝）
     */
    private Integer paymentMethod;
    
    /**
     * 订单备注
     */
    private String remark;
    
    /**
     * 取消原因
     */
    private String cancelReason;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
} 