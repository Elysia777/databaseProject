 package com.taxi.entity;

 import lombok.Data;
 import lombok.EqualsAndHashCode;

 import java.io.Serializable;
 import java.math.BigDecimal;
 import java.time.LocalDateTime;

 /**
 * 订单实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class Order implements Serializable {

    private Long id;

    /** 订单号 */
    private String orderNumber;

    /** 乘客ID */
    private Long passengerId;

    /** 司机ID */
    private Long driverId;

    /** 车辆ID */
    private Long vehicleId;

    /** 订单类型：REAL_TIME-实时单，RESERVATION-预约单 */
    private String orderType;

    /** 订单状态：PENDING-待派单，ASSIGNED-已派单，PICKUP-已上车，COMPLETED-已完成，CANCELLED-已取消 */
    private String status;

    /** 上车地址 */
    private String pickupAddress;

    /** 上车地点纬度 */
    private BigDecimal pickupLatitude;

    /** 上车地点经度 */
    private BigDecimal pickupLongitude;

    /** 目的地地址 */
    private String destinationAddress;

    /** 目的地纬度 */
    private BigDecimal destinationLatitude;

    /** 目的地经度 */
    private BigDecimal destinationLongitude;

    /** 预估距离(公里) */
    private BigDecimal estimatedDistance;

    /** 预估时间(分钟) */
    private Integer estimatedDuration;

    /** 预估费用 */
    private BigDecimal estimatedFare;

    /** 实际距离(公里) */
    private BigDecimal actualDistance;

    /** 实际时间(分钟) */
    private Integer actualDuration;

    /** 实际费用 */
    private BigDecimal actualFare;

    /** 服务费 */
    private BigDecimal serviceFee;

    /** 总费用 */
    private BigDecimal totalFare;

    /** 支付方式：CASH-现金，WECHAT-微信，ALIPAY-支付宝，CREDIT_CARD-信用卡 */
    private String paymentMethod;

    /** 支付状态：UNPAID-未支付，PAID-已支付，REFUNDED-已退款 */
    private String paymentStatus;

    /** 预约时间 */
    private LocalDateTime scheduledTime;

    /** 上车时间 */
    private LocalDateTime pickupTime;

    /** 完成时间 */
    private LocalDateTime completionTime;

    /** 取消原因 */
    private String cancelReason;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}