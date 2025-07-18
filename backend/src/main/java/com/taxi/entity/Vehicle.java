 package com.taxi.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 车辆实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class Vehicle {

    private Long id;

    /** 司机ID */
    private Long driverId;

    /** 车牌号 */
    private String plateNumber;

    /** 车辆品牌 */
    private String brand;

    /** 车辆型号 */
    private String model;

    /** 车辆颜色 */
    private String color;

    /** 车辆类型：SEDAN-轿车，SUV-越野车，VAN-面包车等 */
    private String vehicleType;

    /** 座位数 */
    private Integer seats;

    /** 车辆年份 */
    private Integer year;

    /** 行驶证号 */
    private String registrationNumber;

    /** 行驶证有效期 */
    private LocalDateTime registrationExpiryDate;

    /** 保险到期时间 */
    private LocalDateTime insuranceExpiryDate;

    /** 车辆状态：ACTIVE-正常，MAINTENANCE-维修中，INACTIVE-停用 */
    private String status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}