-- 修复车辆表中的司机ID问题
-- 问题：车辆表中的driver_id存储的是用户ID，应该是司机ID

-- 1. 首先查看当前数据状态
SELECT 
    v.id as vehicle_id,
    v.driver_id as current_driver_id,
    v.plate_number,
    u.id as user_id,
    u.real_name as user_name,
    d.id as correct_driver_id,
    d.user_id as driver_user_id
FROM vehicles v
LEFT JOIN users u ON v.driver_id = u.id
LEFT JOIN drivers d ON u.id = d.user_id
WHERE u.user_type = 'DRIVER';

-- 2. 修复车辆表中的司机ID
-- 将用户ID替换为对应的司机ID
UPDATE vehicles v
JOIN users u ON v.driver_id = u.id
JOIN drivers d ON u.id = d.user_id
SET v.driver_id = d.id
WHERE u.user_type = 'DRIVER';

-- 3. 验证修复结果
SELECT 
    v.id as vehicle_id,
    v.driver_id as fixed_driver_id,
    v.plate_number,
    v.is_active,
    d.id as driver_id,
    u.real_name as driver_name
FROM vehicles v
JOIN drivers d ON v.driver_id = d.id
JOIN users u ON d.user_id = u.id
ORDER BY v.driver_id, v.id;

-- 4. 检查是否有孤立的车辆记录（driver_id不存在于drivers表中）
SELECT 
    v.id as vehicle_id,
    v.driver_id,
    v.plate_number
FROM vehicles v
LEFT JOIN drivers d ON v.driver_id = d.id
WHERE d.id IS NULL;

-- 5. 如果有孤立记录，可以删除或者手动修复
-- DELETE FROM vehicles WHERE driver_id NOT IN (SELECT id FROM drivers);