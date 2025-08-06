package com.taxi.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
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

    /** 品牌 */
    private String brand;

    /** 型号 */
    private String model;

    /** 颜色 */
    private String color;

    /** 年份 */
    private Integer year;

    /** 座位数 */
    private Integer seats;

    /** 车辆类型 */
    private String vehicleType;

    /** 燃料类型 */
    private String fuelType;

    /** 保险单号 */
    private String insuranceNumber;

    /** 保险到期日期 */
    private LocalDate insuranceExpiry;

    /** 年检到期日期 */
    private LocalDate inspectionExpiry;

    /** 是否激活 */
    private Boolean isActive;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}