-- 修复经纬度坐标顺序的SQL脚本
USE taxi_operation_system;

-- 检查当前司机表中的坐标数据
SELECT 
    id,
    user_id,
    current_latitude as 纬度,
    current_longitude as 经度,
    is_online as 在线状态
FROM drivers 
WHERE current_latitude IS NOT NULL AND current_longitude IS NOT NULL;

-- 如果发现坐标顺序颠倒，使用以下SQL修复
-- 注意：只有在确认坐标顺序错误时才执行以下语句

-- 备份原始数据
CREATE TABLE IF NOT EXISTS drivers_coordinate_backup AS
SELECT id, current_latitude, current_longitude, updated_at
FROM drivers 
WHERE current_latitude IS NOT NULL AND current_longitude IS NOT NULL;

-- 交换经纬度（仅在确认需要时执行）
-- UPDATE drivers 
-- SET 
--     current_latitude = current_longitude,
--     current_longitude = current_latitude,
--     updated_at = NOW()
-- WHERE current_latitude IS NOT NULL AND current_longitude IS NOT NULL;

-- 验证修复后的数据
-- SELECT 
--     id,
--     current_latitude as 修复后纬度,
--     current_longitude as 修复后经度
-- FROM drivers 
-- WHERE current_latitude IS NOT NULL AND current_longitude IS NOT NULL;

-- 常用的测试坐标（北京地区）
-- 天安门广场: 纬度 39.9042, 经度 116.4074
-- 西单商业区: 纬度 39.9163, 经度 116.3972
-- 鸟巢体育场: 纬度 39.9928, 经度 116.3906

-- 插入测试坐标数据
INSERT INTO drivers (user_id, driver_license, driving_years, rating, total_orders, completed_orders, cancelled_orders, is_online, current_latitude, current_longitude, created_at, updated_at)
VALUES 
-- 测试司机1 - 天安门广场
(10, 'TEST001', 5, 5.00, 0, 0, 0, FALSE, 39.9042, 116.4074, NOW(), NOW()),
-- 测试司机2 - 西单商业区  
(11, 'TEST002', 3, 5.00, 0, 0, 0, FALSE, 39.9163, 116.3972, NOW(), NOW()),
-- 测试司机3 - 鸟巢体育场
(12, 'TEST003', 7, 5.00, 0, 0, 0, FALSE, 39.9928, 116.3906, NOW(), NOW())
ON DUPLICATE KEY UPDATE 
    current_latitude = VALUES(current_latitude),
    current_longitude = VALUES(current_longitude),
    updated_at = NOW();

-- 验证坐标范围（北京地区合理范围）
-- 纬度范围: 39.4 - 41.0 (北京地区)
-- 经度范围: 115.4 - 117.5 (北京地区)
SELECT 
    id,
    current_latitude,
    current_longitude,
    CASE 
        WHEN current_latitude BETWEEN 39.4 AND 41.0 AND current_longitude BETWEEN 115.4 AND 117.5 THEN '坐标正常'
        WHEN current_latitude BETWEEN 115.4 AND 117.5 AND current_longitude BETWEEN 39.4 AND 41.0 THEN '坐标可能颠倒'
        ELSE '坐标异常'
    END as 坐标状态
FROM drivers 
WHERE current_latitude IS NOT NULL AND current_longitude IS NOT NULL;