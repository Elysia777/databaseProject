 package com.taxi.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 司机实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class Driver {

    private Long id;

    /** 用户ID */
    private Long userId;
    
    /** 司机姓名 (来自users表的real_name) */
    private String name;
    
    /** 手机号 (来自users表) */
    private String phone;
    
    /** 用户状态 (来自users表) */
    private String status;
    
    /** 驾驶证号 */
    private String licenseNumber;  // 重命名以匹配常用命名

    /** 驾驶证号 */
    private String driverLicense;

    /** 驾驶证到期日期 */
    private LocalDateTime driverLicenseExpiry;

    /** 从业资格证号 */
    private String professionalLicense;

    /** 从业资格证到期日期 */
    private LocalDateTime professionalLicenseExpiry;
    
    /** 车辆信息 */
    private String vehicleInfo;
    
    /** 车牌号 */
    private String licensePlate;

    /** 驾龄(年) */
    private Integer drivingYears;

    /** 总里程(公里) */
    private BigDecimal totalMileage;

    /** 评分 */
    private BigDecimal rating;

    /** 总订单数 */
    private Integer totalOrders;

    /** 完成订单数 */
    private Integer completedOrders;

    /** 取消订单数 */
    private Integer cancelledOrders;

    /** 是否在线 */
    private Boolean isOnline;

    /** 当前纬度 */
    private BigDecimal currentLatitude;

    /** 当前经度 */
    private BigDecimal currentLongitude;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}