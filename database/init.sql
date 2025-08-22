-- 网约车运营系统数据库初始化脚本
-- 创建数据库
CREATE DATABASE IF NOT EXISTS taxi_operation_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE taxi_operation_system;

-- 1. 用户表
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    phone VARCHAR(20) NOT NULL UNIQUE COMMENT '手机号',
    email VARCHAR(100) UNIQUE COMMENT '邮箱',
    real_name VARCHAR(50) COMMENT '真实姓名',
    id_card VARCHAR(18) COMMENT '身份证号',
    avatar VARCHAR(255) COMMENT '头像URL',
    user_type ENUM('ADMIN', 'DRIVER', 'PASSENGER') NOT NULL COMMENT '用户类型：管理员、司机、乘客',
    status ENUM('ACTIVE', 'INACTIVE', 'BANNED') DEFAULT 'ACTIVE' COMMENT '用户状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_phone (phone),
    INDEX idx_email (email),
    INDEX idx_user_type (user_type),
    INDEX idx_status (status)
) COMMENT '用户表';

-- 2. 司机信息表
CREATE TABLE drivers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '司机ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    driver_license VARCHAR(50) NOT NULL UNIQUE COMMENT '驾驶证号',
    driver_license_expiry DATE COMMENT '驾驶证到期日期',
    professional_license VARCHAR(50) COMMENT '从业资格证号',
    professional_license_expiry DATE COMMENT '从业资格证到期日期',
    driving_years INT DEFAULT 0 COMMENT '驾龄(年)',
    total_mileage DECIMAL(10,2) DEFAULT 0.00 COMMENT '总里程(公里)',
    rating DECIMAL(3,2) DEFAULT 5.00 COMMENT '评分',
    total_orders INT DEFAULT 0 COMMENT '总订单数',
    completed_orders INT DEFAULT 0 COMMENT '完成订单数',
    cancelled_orders INT DEFAULT 0 COMMENT '取消订单数',
    is_online BOOLEAN DEFAULT FALSE COMMENT '是否在线',
    current_latitude DECIMAL(12,8) COMMENT '当前纬度',
    current_longitude DECIMAL(12,8) COMMENT '当前经度',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_driver_license (driver_license),
    INDEX idx_is_online (is_online),
    INDEX idx_rating (rating)
) COMMENT '司机信息表';

-- 3. 车辆信息表
CREATE TABLE vehicles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '车辆ID',
    driver_id BIGINT NOT NULL COMMENT '司机ID',
    plate_number VARCHAR(20) NOT NULL UNIQUE COMMENT '车牌号',
    brand VARCHAR(50) COMMENT '品牌',
    model VARCHAR(50) COMMENT '型号',
    color VARCHAR(20) COMMENT '颜色',
    year INT COMMENT '年份',
    seats INT DEFAULT 4 COMMENT '座位数',
    vehicle_type ENUM('ECONOMY', 'COMFORT', 'PREMIUM', 'LUXURY') DEFAULT 'ECONOMY' COMMENT '车辆类型',
    fuel_type ENUM('GASOLINE', 'DIESEL', 'ELECTRIC', 'HYBRID') COMMENT '燃料类型',
    insurance_number VARCHAR(50) COMMENT '保险单号',
    insurance_expiry DATE COMMENT '保险到期日期',
    inspection_expiry DATE COMMENT '年检到期日期',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否激活',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (driver_id) REFERENCES drivers(id) ON DELETE CASCADE,
    INDEX idx_driver_id (driver_id),
    INDEX idx_plate_number (plate_number),
    INDEX idx_vehicle_type (vehicle_type),
    INDEX idx_is_active (is_active)
) COMMENT '车辆信息表';

-- 4. 乘客信息表
CREATE TABLE passengers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '乘客ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    emergency_contact VARCHAR(50) COMMENT '紧急联系人',
    emergency_phone VARCHAR(20) COMMENT '紧急联系人电话',
    default_payment_method ENUM('CASH', 'WECHAT', 'ALIPAY', 'CREDIT_CARD') DEFAULT 'WECHAT' COMMENT '默认支付方式',
    rating DECIMAL(3,2) DEFAULT 5.00 COMMENT '评分',
    total_orders INT DEFAULT 0 COMMENT '总订单数',
    total_spent DECIMAL(10,2) DEFAULT 0.00 COMMENT '总消费金额',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_rating (rating)
) COMMENT '乘客信息表';

-- 5. 订单表
CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
    order_number VARCHAR(50) NOT NULL UNIQUE COMMENT '订单号',
    passenger_id BIGINT NOT NULL COMMENT '乘客ID',
    driver_id BIGINT COMMENT '司机ID',
    vehicle_id BIGINT COMMENT '车辆ID',
    order_type ENUM('REAL_TIME', 'RESERVATION') NOT NULL COMMENT '订单类型：实时单、预约单',
    status ENUM('PENDING', 'ASSIGNED', 'PICKUP', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') DEFAULT 'PENDING' COMMENT '订单状态',
    pickup_address VARCHAR(255) NOT NULL COMMENT '上车地址',
    pickup_latitude DECIMAL(12,8) COMMENT '上车地点纬度',
    pickup_longitude DECIMAL(12,8) COMMENT '上车地点经度',
    destination_address VARCHAR(255) NOT NULL COMMENT '目的地地址',
    destination_latitude DECIMAL(12,8) COMMENT '目的地纬度',
    destination_longitude DECIMAL(12,8) COMMENT '目的地经度',
    estimated_distance DECIMAL(8,2) COMMENT '预估距离(公里)',
    estimated_duration INT COMMENT '预估时间(分钟)',
    estimated_fare DECIMAL(8,2) COMMENT '预估费用',
    actual_distance DECIMAL(8,2) COMMENT '实际距离(公里)',
    actual_duration INT COMMENT '实际时间(分钟)',
    actual_fare DECIMAL(8,2) COMMENT '实际费用',
    service_fee DECIMAL(8,2) DEFAULT 0.00 COMMENT '服务费',
    total_fare DECIMAL(8,2) COMMENT '总费用',
    payment_method ENUM('CASH', 'WECHAT', 'ALIPAY', 'CREDIT_CARD') COMMENT '支付方式',
    payment_status ENUM('UNPAID', 'PAID', 'REFUNDED') DEFAULT 'UNPAID' COMMENT '支付状态',
    scheduled_time DATETIME COMMENT '预约时间',
    pickup_time DATETIME COMMENT '上车时间',
    completion_time DATETIME COMMENT '完成时间',
    cancel_reason VARCHAR(255) COMMENT '取消原因',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (passenger_id) REFERENCES passengers(id),
    FOREIGN KEY (driver_id) REFERENCES drivers(id),
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
    INDEX idx_order_number (order_number),
    INDEX idx_passenger_id (passenger_id),
    INDEX idx_driver_id (driver_id),
    INDEX idx_status (status),
    INDEX idx_order_type (order_type),
    INDEX idx_created_at (created_at),
    INDEX idx_scheduled_time (scheduled_time)
) COMMENT '订单表';

-- 6. 订单详情表
CREATE TABLE order_details (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '详情ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    sequence INT NOT NULL COMMENT '序号',
    address VARCHAR(255) NOT NULL COMMENT '地址',
    latitude DECIMAL(12,8) COMMENT '纬度',
    longitude DECIMAL(12,8) COMMENT '经度',
    action ENUM('PICKUP', 'DROPOFF', 'WAYPOINT') NOT NULL COMMENT '动作类型',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    INDEX idx_order_id (order_id),
    INDEX idx_sequence (sequence)
) COMMENT '订单详情表';

-- 7. 支付记录表
CREATE TABLE payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '支付ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    payment_number VARCHAR(50) NOT NULL UNIQUE COMMENT '支付单号',
    amount DECIMAL(8,2) NOT NULL COMMENT '支付金额',
    payment_method ENUM('CASH', 'WECHAT', 'ALIPAY', 'CREDIT_CARD') NOT NULL COMMENT '支付方式',
    payment_status ENUM('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED') DEFAULT 'PENDING' COMMENT '支付状态',
    transaction_id VARCHAR(100) COMMENT '第三方交易号',
    payment_time DATETIME COMMENT '支付时间',
    refund_time DATETIME COMMENT '退款时间',
    refund_reason VARCHAR(255) COMMENT '退款原因',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (order_id) REFERENCES orders(id),
    INDEX idx_order_id (order_id),
    INDEX idx_payment_number (payment_number),
    INDEX idx_payment_status (payment_status),
    INDEX idx_payment_time (payment_time)
) COMMENT '支付记录表';

-- 8. 评价表
CREATE TABLE ratings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评价ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    rater_id BIGINT NOT NULL COMMENT '评价人ID',
    rated_id BIGINT NOT NULL COMMENT '被评价人ID',
    rating_type ENUM('PASSENGER_TO_DRIVER', 'DRIVER_TO_PASSENGER') NOT NULL COMMENT '评价类型',
    rating DECIMAL(3,2) NOT NULL COMMENT '评分(1-5)',
    comment TEXT COMMENT '评价内容',
    tags JSON COMMENT '评价标签',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (order_id) REFERENCES orders(id),
    INDEX idx_order_id (order_id),
    INDEX idx_rater_id (rater_id),
    INDEX idx_rated_id (rated_id),
    INDEX idx_rating_type (rating_type),
    INDEX idx_rating (rating)
) COMMENT '评价表';

-- 9. 投诉表
CREATE TABLE complaints (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '投诉ID',
    order_id BIGINT COMMENT '订单ID',
    complainant_id BIGINT NOT NULL COMMENT '投诉人ID',
    defendant_id BIGINT NOT NULL COMMENT '被投诉人ID',
    complaint_type ENUM('SERVICE', 'SAFETY', 'PAYMENT', 'BEHAVIOR', 'OTHER') NOT NULL COMMENT '投诉类型',
    title VARCHAR(100) NOT NULL COMMENT '投诉标题',
    description TEXT NOT NULL COMMENT '投诉描述',
    evidence_files JSON COMMENT '证据文件',
    status ENUM('PENDING', 'INVESTIGATING', 'RESOLVED', 'CLOSED') DEFAULT 'PENDING' COMMENT '处理状态',
    admin_id BIGINT COMMENT '处理管理员ID',
    resolution TEXT COMMENT '处理结果',
    refund_amount DECIMAL(10,2) DEFAULT 0.00 COMMENT '退款金额',
    resolution_time DATETIME COMMENT '处理时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (order_id) REFERENCES orders(id),
    INDEX idx_order_id (order_id),
    INDEX idx_complainant_id (complainant_id),
    INDEX idx_defendant_id (defendant_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) COMMENT '投诉表';

-- 10. 纠纷处理表
CREATE TABLE disputes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '纠纷ID',
    complaint_id BIGINT COMMENT '投诉ID',
    order_id BIGINT COMMENT '订单ID',
    initiator_id BIGINT NOT NULL COMMENT '发起人ID',
    respondent_id BIGINT NOT NULL COMMENT '被申请人ID',
    dispute_type ENUM('FARE_DISPUTE', 'SERVICE_DISPUTE', 'SAFETY_DISPUTE', 'OTHER') NOT NULL COMMENT '纠纷类型',
    description TEXT NOT NULL COMMENT '纠纷描述',
    amount DECIMAL(8,2) COMMENT '纠纷金额',
    status ENUM('PENDING', 'MEDIATION', 'ARBITRATION', 'RESOLVED', 'CLOSED') DEFAULT 'PENDING' COMMENT '处理状态',
    admin_id BIGINT COMMENT '处理管理员ID',
    resolution TEXT COMMENT '处理结果',
    resolution_time DATETIME COMMENT '处理时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (complaint_id) REFERENCES complaints(id),
    FOREIGN KEY (order_id) REFERENCES orders(id),
    INDEX idx_complaint_id (complaint_id),
    INDEX idx_order_id (order_id),
    INDEX idx_initiator_id (initiator_id),
    INDEX idx_respondent_id (respondent_id),
    INDEX idx_status (status)
) COMMENT '纠纷处理表';

-- 11. 统计报表表
CREATE TABLE statistics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '统计ID',
    date DATE NOT NULL COMMENT '统计日期',
    stat_type ENUM('DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY') NOT NULL COMMENT '统计类型',
    category VARCHAR(50) NOT NULL COMMENT '统计类别',
    metric_name VARCHAR(100) NOT NULL COMMENT '指标名称',
    metric_value DECIMAL(15,2) NOT NULL COMMENT '指标值',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_date (date),
    INDEX idx_stat_type (stat_type),
    INDEX idx_category (category),
    INDEX idx_metric_name (metric_name)
) COMMENT '统计报表表';

-- 12. 系统配置表
CREATE TABLE system_configs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    config_type ENUM('STRING', 'NUMBER', 'BOOLEAN', 'JSON') DEFAULT 'STRING' COMMENT '配置类型',
    description VARCHAR(255) COMMENT '配置描述',
    is_editable BOOLEAN DEFAULT TRUE COMMENT '是否可编辑',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_config_key (config_key)
) COMMENT '系统配置表';

-- 13. 管理员详细信息表
CREATE TABLE admin_users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '管理员ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role ENUM('SUPER_ADMIN', 'ADMIN', 'OPERATOR') NOT NULL DEFAULT 'ADMIN' COMMENT '管理员角色',
    permissions JSON COMMENT '权限配置',
    department VARCHAR(50) COMMENT '部门',
    position VARCHAR(50) COMMENT '职位',
    last_login_at TIMESTAMP COMMENT '最后登录时间',
    last_login_ip VARCHAR(45) COMMENT '最后登录IP',
    login_count INT DEFAULT 0 COMMENT '登录次数',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否激活',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_role (role),
    INDEX idx_is_active (is_active)
) COMMENT '管理员详细信息表';

-- 14. 管理员操作日志表
CREATE TABLE admin_operation_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    admin_id BIGINT NOT NULL COMMENT '管理员ID',
    operation_type VARCHAR(50) NOT NULL COMMENT '操作类型',
    operation_desc TEXT COMMENT '操作描述',
    target_type VARCHAR(50) COMMENT '操作对象类型',
    target_id BIGINT COMMENT '操作对象ID',
    request_method VARCHAR(10) COMMENT '请求方法',
    request_url VARCHAR(255) COMMENT '请求URL',
    request_params JSON COMMENT '请求参数',
    response_status INT COMMENT '响应状态码',
    ip_address VARCHAR(45) COMMENT 'IP地址',
    user_agent TEXT COMMENT '用户代理',
    execution_time INT COMMENT '执行时间(毫秒)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_admin_id (admin_id),
    INDEX idx_operation_type (operation_type),
    INDEX idx_target_type (target_type),
    INDEX idx_created_at (created_at)
) COMMENT '管理员操作日志表';

-- 15. 系统通知表
CREATE TABLE system_notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '通知ID',
    title VARCHAR(100) NOT NULL COMMENT '通知标题',
    content TEXT NOT NULL COMMENT '通知内容',
    notification_type ENUM('SYSTEM', 'ANNOUNCEMENT', 'ALERT', 'MAINTENANCE') NOT NULL COMMENT '通知类型',
    target_type ENUM('ALL', 'DRIVER', 'PASSENGER', 'SPECIFIC') NOT NULL COMMENT '目标类型',
    target_users JSON COMMENT '目标用户ID列表(当target_type为SPECIFIC时)',
    priority ENUM('LOW', 'NORMAL', 'HIGH', 'URGENT') DEFAULT 'NORMAL' COMMENT '优先级',
    send_method ENUM('APP', 'SMS', 'EMAIL', 'ALL') DEFAULT 'APP' COMMENT '发送方式',
    status ENUM('DRAFT', 'SCHEDULED', 'SENT', 'CANCELLED') DEFAULT 'DRAFT' COMMENT '状态',
    scheduled_time DATETIME COMMENT '定时发送时间',
    sent_time DATETIME COMMENT '实际发送时间',
    sender_id BIGINT NOT NULL COMMENT '发送者ID',
    read_count INT DEFAULT 0 COMMENT '已读数量',
    total_count INT DEFAULT 0 COMMENT '总发送数量',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_notification_type (notification_type),
    INDEX idx_target_type (target_type),
    INDEX idx_status (status),
    INDEX idx_scheduled_time (scheduled_time),
    INDEX idx_created_at (created_at)
) COMMENT '系统通知表';

-- 16. 用户通知记录表
CREATE TABLE user_notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    notification_id BIGINT NOT NULL COMMENT '通知ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    is_read BOOLEAN DEFAULT FALSE COMMENT '是否已读',
    read_time DATETIME COMMENT '阅读时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (notification_id) REFERENCES system_notifications(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_notification_id (notification_id),
    INDEX idx_user_id (user_id),
    INDEX idx_is_read (is_read)
) COMMENT '用户通知记录表';

-- 插入初始数据

-- 插入管理员用户
INSERT INTO users (username, password, phone, email, real_name, user_type) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '13800000000', 'admin@taxi.com', '系统管理员', 'ADMIN'),
('superadmin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '13800000001', 'superadmin@taxi.com', '超级管理员', 'ADMIN');

-- 插入管理员详细信息
INSERT INTO admin_users (user_id, role, department, position, permissions) VALUES
(1, 'SUPER_ADMIN', '技术部', '系统管理员', '["ALL"]'),
(2, 'ADMIN', '运营部', '运营管理员', '["USER_MANAGEMENT", "ORDER_MANAGEMENT", "REVIEW_MANAGEMENT", "STATISTICS"]');

-- 插入系统配置
INSERT INTO system_configs (config_key, config_value, config_type, description) VALUES
('base_fare', '10.00', 'NUMBER', '基础起步价'),
('per_km_fare', '2.50', 'NUMBER', '每公里费用'),
('service_fee_rate', '0.10', 'NUMBER', '服务费比例'),
('max_wait_time', '300', 'NUMBER', '最大等待时间(秒)'),
('auto_assign_radius', '5000', 'NUMBER', '自动派单半径(米)'),
('min_driver_rating', '4.0', 'NUMBER', '最低司机评分'),
('max_cancel_rate', '0.1', 'NUMBER', '最大取消率');

-- 创建视图
CREATE VIEW driver_stats AS
SELECT 
    d.id,
    d.user_id,
    u.real_name,
    u.phone,
    d.rating,
    d.total_orders,
    d.completed_orders,
    d.cancelled_orders,
    CASE 
        WHEN d.total_orders > 0 THEN ROUND(d.completed_orders / d.total_orders * 100, 2)
        ELSE 0 
    END as completion_rate,
    CASE 
        WHEN d.total_orders > 0 THEN ROUND(d.cancelled_orders / d.total_orders * 100, 2)
        ELSE 0 
    END as cancel_rate
FROM drivers d
JOIN users u ON d.user_id = u.id;

CREATE VIEW order_summary AS
SELECT 
    o.id,
    o.order_number,
    o.order_type,
    o.status,
    o.estimated_fare,
    o.actual_fare,
    o.total_fare,
    o.created_at,
    p.user_id as passenger_user_id,
    pu.real_name as passenger_name,
    pu.phone as passenger_phone,
    d.user_id as driver_user_id,
    du.real_name as driver_name,
    du.phone as driver_phone
FROM orders o
JOIN passengers p ON o.passenger_id = p.id
JOIN users pu ON p.user_id = pu.id
LEFT JOIN drivers d ON o.driver_id = d.id
LEFT JOIN users du ON d.user_id = du.id; 