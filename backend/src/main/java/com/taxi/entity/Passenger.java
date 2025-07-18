package com.taxi.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 乘客实体类
 */
@Data
public class Passenger {

    private Long id;

    /** 用户ID */
    private Long userId;

    /** 紧急联系人 */
    private String emergencyContact;

    /** 紧急联系人电话 */
    private String emergencyPhone;

    /** 默认支付方式：CASH-现金，WECHAT-微信，ALIPAY-支付宝，CREDIT_CARD-信用卡 */
    private String defaultPaymentMethod;

    /** 评分 */
    private BigDecimal rating;

    /** 总订单数 */
    private Integer totalOrders;

    /** 总消费金额 */
    private BigDecimal totalSpent;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
} 