 package com.taxi.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建订单请求DTO
 */
@Data
public class CreateOrderRequest {

    /** 乘客ID */
    @NotNull(message = "乘客ID不能为空")
    private Long passengerId;

    /** 订单类型：REAL_TIME-实时单，RESERVATION-预约单 */
    @NotBlank(message = "订单类型不能为空")
    private String orderType;

    /** 上车地址 */
    @NotBlank(message = "上车地址不能为空")
    private String pickupAddress;

    /** 上车地点纬度 */
    @NotNull(message = "上车地点纬度不能为空")
    private BigDecimal pickupLatitude;

    /** 上车地点经度 */
    @NotNull(message = "上车地点经度不能为空")
    private BigDecimal pickupLongitude;

    /** 目的地地址 */
    @NotBlank(message = "目的地地址不能为空")
    private String destinationAddress;

    /** 目的地纬度 */
    @NotNull(message = "目的地纬度不能为空")
    private BigDecimal destinationLatitude;

    /** 目的地经度 */
    @NotNull(message = "目的地经度不能为空")
    private BigDecimal destinationLongitude;

    /** 预约时间（预约单必填） */
    private LocalDateTime reservationTime;
}