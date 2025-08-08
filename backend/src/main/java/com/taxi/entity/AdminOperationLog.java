package com.taxi.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 管理员操作日志实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class AdminOperationLog {

    private Long id;

    /** 管理员ID */
    private Long adminId;

    /** 操作类型 */
    private String operationType;

    /** 操作描述 */
    private String operationDesc;

    /** 操作对象类型 */
    private String targetType;

    /** 操作对象ID */
    private Long targetId;

    /** 请求方法 */
    private String requestMethod;

    /** 请求URL */
    private String requestUrl;

    /** 请求参数 */
    private Map<String, Object> requestParams;

    /** 响应状态码 */
    private Integer responseStatus;

    /** IP地址 */
    private String ipAddress;

    /** 用户代理 */
    private String userAgent;

    /** 执行时间(毫秒) */
    private Integer executionTime;

    /** 创建时间 */
    private LocalDateTime createdAt;

    // 关联的管理员信息
    private AdminUser adminUser;
}