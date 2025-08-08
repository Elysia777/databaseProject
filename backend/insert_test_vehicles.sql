-- 插入测试车辆数据

-- 首先确保vehicles表有正确的字段
-- 如果没有status和vehicle_image字段，请先执行add_vehicle_fields.sql

-- 插入测试车辆数据
INSERT INTO vehicles (
    driver_id, plate_number, brand, model, color, year, seats,
    vehicle_type, fuel_type, insurance_number, insurance_expiry,
    inspection_expiry, status, vehicle_image, is_active, created_at, updated_at
) VALUES 
(1, '京A12345', '丰田', '凯美瑞', '白色', 2022, 5, 'SEDAN', 'GASOLINE', 'INS001', '2025-12-31', '2025-06-30', 'ACTIVE', NULL, TRUE, NOW(), NOW()),
(2, '京B67890', '本田', 'CR-V', '黑色', 2021, 5, 'SUV', 'GASOLINE', 'INS002', '2025-11-30', '2025-05-31', 'ACTIVE', NULL, TRUE, NOW(), NOW()),
(3, '京C11111', '大众', '帕萨特', '银色', 2023, 5, 'SEDAN', 'GASOLINE', 'INS003', '2026-01-31', '2025-07-31', 'PENDING', NULL, FALSE, NOW(), NOW()),
(1, '京D22222', '奔驰', 'GLC', '蓝色', 2022, 5, 'SUV', 'GASOLINE', 'INS004', '2025-10-31', '2025-04-30', 'REJECTED', NULL, FALSE, NOW(), NOW()),
(4, '京E33333', '宝马', 'X3', '红色', 2021, 5, 'SUV', 'GASOLINE', 'INS005', '2025-09-30', '2025-03-31', 'INACTIVE', NULL, FALSE, NOW(), NOW());

-- 查看插入的数据
SELECT 
    v.id,
    v.driver_id,
    v.plate_number,
    v.brand,
    v.model,
    v.color,
    v.vehicle_type,
    v.status,
    v.is_active,
    d.id as driver_table_id,
    u.real_name as driver_name
FROM vehicles v
LEFT JOIN drivers d ON v.driver_id = d.id
LEFT JOIN users u ON d.user_id = u.id
ORDER BY v.created_at DESC;

-- 统计各状态的车辆数量
SELECT 
    status,
    COUNT(*) as count
FROM vehicles 
GROUP BY status;

-- 统计各类型的车辆数量
SELECT 
    vehicle_type,
    COUNT(*) as count
FROM vehicles 
GROUP BY vehicle_type;