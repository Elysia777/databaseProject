-- 修复司机数据问题的SQL脚本

-- 1. 检查users表是否存在司机用户数据
SELECT COUNT(*) as user_count FROM users WHERE user_type = 'DRIVER';

-- 2. 检查drivers表是否存在司机数据
SELECT COUNT(*) as driver_count FROM drivers;

-- 3. 查看现有司机数据（关联查询）
SELECT d.id, u.real_name as name, u.phone, d.driver_license, u.status, d.current_latitude, d.current_longitude 
FROM drivers d
LEFT JOIN users u ON d.user_id = u.id
ORDER BY d.id;

-- 4. 如果没有司机用户数据，插入测试用户
INSERT IGNORE INTO users (id, username, password, phone, real_name, user_type, status, created_at, updated_at) 
VALUES 
(1, 'driver1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKyZFzUNjKjvKHDvpHMS6Y1.k4W2', '13800138001', '测试司机1', 'DRIVER', 'ACTIVE', NOW(), NOW()),
(2, 'driver2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKyZFzUNjKjvKHDvpHMS6Y1.k4W2', '13800138002', '测试司机2', 'DRIVER', 'ACTIVE', NOW(), NOW()),
(3, 'driver3', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKyZFzUNjKjvKHDvpHMS6Y1.k4W2', '13800138003', '测试司机3', 'DRIVER', 'ACTIVE', NOW(), NOW());

-- 5. 如果没有司机数据，插入测试司机
INSERT IGNORE INTO drivers (id, user_id, driver_license, current_latitude, current_longitude, is_online, created_at, updated_at) 
VALUES 
(1, 1, 'TEST001', 39.044237, 121.749849, true, NOW(), NOW()),
(2, 2, 'TEST002', 39.045237, 121.750849, true, NOW(), NOW()),
(3, 3, 'TEST003', 39.046237, 121.751849, true, NOW(), NOW());

-- 6. 插入测试车辆信息
INSERT IGNORE INTO vehicles (id, driver_id, plate_number, brand, model, color, created_at, updated_at)
VALUES 
(1, 1, '辽B12345', '大众', '朗逸', '白色', NOW(), NOW()),
(2, 2, '辽B23456', '丰田', '卡罗拉', '银色', NOW(), NOW()),
(3, 3, '辽B34567', '本田', '雅阁', '黑色', NOW(), NOW());

-- 7. 更新司机状态为在线
UPDATE drivers SET is_online = true WHERE is_online = false;

-- 8. 确保司机有位置信息
UPDATE drivers SET 
    current_latitude = 39.044237,
    current_longitude = 121.749849
WHERE current_latitude IS NULL OR current_longitude IS NULL;

-- 9. 查看更新后的司机数据（完整关联查询）
SELECT 
    d.id, 
    u.real_name as name, 
    u.phone, 
    d.driver_license, 
    u.status, 
    d.is_online,
    d.current_latitude, 
    d.current_longitude,
    CONCAT(COALESCE(v.brand, ''), ' ', COALESCE(v.model, ''), ' ', COALESCE(v.color, '')) as vehicle_info
FROM drivers d
LEFT JOIN users u ON d.user_id = u.id
LEFT JOIN vehicles v ON d.id = v.driver_id
ORDER BY d.id;