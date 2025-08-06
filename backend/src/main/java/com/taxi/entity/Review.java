package com.taxi.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评价实体类 - 对应ratings表
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class Review {

    private Long id;

    /** 订单ID */
    private Long orderId;

    /** 评价人ID */
    private Long raterId;

    /** 被评价人ID */
    private Long ratedId;

    /** 评价类型：PASSENGER_TO_DRIVER-乘客评价司机，DRIVER_TO_PASSENGER-司机评价乘客 */
    private String ratingType;

    /** 评分 (1-5分) */
    private BigDecimal rating;

    /** 评价内容 */
    private String comment;

    /** 评价标签 (JSON格式存储多个标签) */
    private String tags;

    /** 创建时间 */
    private LocalDateTime createdAt;
}