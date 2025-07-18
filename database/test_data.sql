-- 测试数据
USE taxi_operation_system;

-- 插入测试用户（密码：123456，使用BCrypt加密）
INSERT INTO users (username, password, phone, email, real_name, user_type, status, created_at, updated_at) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '13800138000', 'admin@example.com', '管理员', 'ADMIN', 'ACTIVE', NOW(), NOW()),
('driver1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '13800138001', 'driver1@example.com', '司机张三', 'DRIVER', 'ACTIVE', NOW(), NOW()),
('passenger1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '13800138002', 'passenger1@example.com', '乘客李四', 'PASSENGER', 'ACTIVE', NOW(), NOW());

-- 插入司机信息
INSERT INTO drivers (user_id, driver_license, driving_years, rating, total_orders, completed_orders, is_online, created_at, updated_at) VALUES
(2, 'DL123456789', 5, 4.8, 100, 95, FALSE, NOW(), NOW());

-- 插入乘客信息
INSERT INTO passengers (user_id, emergency_contact, emergency_phone, default_payment_method, rating, total_orders, total_spent, created_at, updated_at) VALUES
(3, '王五', '13900139000', 'WECHAT', 4.9, 50, 1500.00, NOW(), NOW()); 