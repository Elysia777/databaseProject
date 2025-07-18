-- 司机上线测试数据
USE taxi_operation_system;

-- 插入更多测试司机用户
INSERT INTO users (username, password, phone, email, real_name, user_type, status, created_at, updated_at) VALUES
('driver2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '13800138011', 'driver2@example.com', '司机李明', 'DRIVER', 'ACTIVE', NOW(), NOW()),
('driver3', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '13800138012', 'driver3@example.com', '司机王强', 'DRIVER', 'ACTIVE', NOW(), NOW()),
('driver4', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '13800138013', 'driver4@example.com', '司机赵敏', 'DRIVER', 'ACTIVE', NOW(), NOW()),
('driver5', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '13800138014', 'driver5@example.com', '司机刘洋', 'DRIVER', 'ACTIVE', NOW(), NOW()),
('driver6', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '13800138015', 'driver6@example.com', '司机陈华', 'DRIVER', 'ACTIVE', NOW(), NOW()),
('driver7', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '13800138016', 'driver7@example.com', '司机周杰', 'DRIVER', 'ACTIVE', NOW(), NOW()),
('driver8', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '13800138017', 'driver8@example.com', '司机吴磊', 'DRIVER', 'ACTIVE', NOW(), NOW());

-- 插入司机详细信息（假设user_id从2开始，因为1是admin）
INSERT INTO drivers (user_id, driver_license, driver_license_expiry, professional_license, professional_license_expiry, driving_years, total_mileage, rating, total_orders, completed_orders, cancelled_orders, is_online, current_latitude, current_longitude, created_at, updated_at) VALUES
-- 司机1 (张三) - 已存在，更新数据
-- 司机2 (李明)
(3, 'DL223456789', '2026-12-31', 'PL223456789', '2025-12-31', 8, 125000.50, 4.9, 280, 275, 5, FALSE, NULL, NULL, NOW(), NOW()),
-- 司机3 (王强)
(4, 'DL323456789', '2027-06-30', 'PL323456789', '2026-06-30', 6, 98000.25, 4.7, 220, 210, 10, FALSE, NULL, NULL, NOW(), NOW()),
-- 司机4 (赵敏)
(5, 'DL423456789', '2026-09-15', 'PL423456789', '2025-09-15', 4, 75000.75, 4.8, 180, 175, 5, FALSE, NULL, NULL, NOW(), NOW()),
-- 司机5 (刘洋)
(6, 'DL523456789', '2027-03-20', 'PL523456789', '2026-03-20', 7, 110000.00, 4.6, 320, 300, 20, FALSE, NULL, NULL, NOW(), NOW()),
-- 司机6 (陈华)
(7, 'DL623456789', '2026-11-10', 'PL623456789', '2025-11-10', 5, 88000.30, 4.9, 200, 195, 5, FALSE, NULL, NULL, NOW(), NOW()),
-- 司机7 (周杰)
(8, 'DL723456789', '2027-01-25', 'PL723456789', '2026-01-25', 9, 145000.80, 4.8, 380, 370, 10, FALSE, NULL, NULL, NOW(), NOW()),
-- 司机8 (吴磊)
(9, 'DL823456789', '2026-08-05', 'PL823456789', '2025-08-05', 3, 45000.60, 4.5, 120, 115, 5, FALSE, NULL, NULL, NOW(), NOW());

-- 插入车辆信息
INSERT INTO vehicles (driver_id, plate_number, brand, model, color, year, seats, vehicle_type, fuel_type, insurance_number, insurance_expiry, inspection_expiry, is_active, created_at, updated_at) VALUES
-- 司机1的车辆 (假设driver_id=1已存在)
-- 司机2的车辆
(2, '京A12345', '大众', '朗逸', '白色', 2020, 5, 'ECONOMY', 'GASOLINE', 'INS2023001', '2024-12-31', '2024-10-31', TRUE, NOW(), NOW()),
-- 司机3的车辆
(3, '京B23456', '丰田', '卡罗拉', '银色', 2019, 5, 'ECONOMY', 'GASOLINE', 'INS2023002', '2024-11-30', '2024-09-30', TRUE, NOW(), NOW()),
-- 司机4的车辆
(4, '京C34567', '本田', '雅阁', '黑色', 2021, 5, 'COMFORT', 'GASOLINE', 'INS2023003', '2025-01-31', '2024-12-31', TRUE, NOW(), NOW()),
-- 司机5的车辆
(5, '京D45678', '日产', '轩逸', '蓝色', 2020, 5, 'ECONOMY', 'GASOLINE', 'INS2023004', '2024-10-31', '2024-08-31', TRUE, NOW(), NOW()),
-- 司机6的车辆
(6, '京E56789', '别克', '君威', '红色', 2022, 5, 'COMFORT', 'GASOLINE', 'INS2023005', '2025-02-28', '2025-01-31', TRUE, NOW(), NOW()),
-- 司机7的车辆
(7, '京F67890', '奥迪', 'A4L', '白色', 2021, 5, 'PREMIUM', 'GASOLINE', 'INS2023006', '2024-12-15', '2024-11-15', TRUE, NOW(), NOW()),
-- 司机8的车辆
(8, '京G78901', '比亚迪', '秦PLUS', '绿色', 2023, 5, 'ECONOMY', 'ELECTRIC', 'INS2023007', '2025-03-31', '2025-02-28', TRUE, NOW(), NOW());

-- 更新司机1的信息（如果需要）
UPDATE drivers SET 
    driver_license_expiry = '2026-10-15',
    professional_license = 'PL123456789',
    professional_license_expiry = '2025-10-15',
    total_mileage = 85000.25,
    total_orders = 150,
    completed_orders = 145,
    cancelled_orders = 5
WHERE id = 1;

-- 为司机1添加车辆信息（如果不存在）
INSERT INTO vehicles (driver_id, plate_number, brand, model, color, year, seats, vehicle_type, fuel_type, insurance_number, insurance_expiry, inspection_expiry, is_active, created_at, updated_at) VALUES
(1, '京A11111', '现代', '伊兰特', '白色', 2019, 5, 'ECONOMY', 'GASOLINE', 'INS2023000', '2024-11-30', '2024-10-30', TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE plate_number = plate_number;