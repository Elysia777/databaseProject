-- 检查用户和乘客表的关联关系
USE taxi_operation_system;

-- 查看所有乘客用户的关联情况
SELECT 
    u.id as user_id, 
    u.username, 
    u.user_type, 
    p.id as passenger_id,
    CASE 
        WHEN p.id IS NULL THEN '❌ 缺少passengers记录'
        ELSE '✅ 关联正常'
    END as status
FROM users u 
LEFT JOIN passengers p ON u.id = p.user_id 
WHERE u.user_type = 'PASSENGER'
ORDER BY u.id;

-- 查看订单表中的passenger_id引用情况
SELECT 
    o.id as order_id,
    o.passenger_id,
    p.id as passenger_exists,
    u.username,
    CASE 
        WHEN p.id IS NULL THEN '❌ 外键约束违反'
        ELSE '✅ 引用正常'
    END as foreign_key_status
FROM orders o
LEFT JOIN passengers p ON o.passenger_id = p.id
LEFT JOIN users u ON p.user_id = u.id
ORDER BY o.id DESC
LIMIT 10;

-- 检查是否有孤立的用户（有users记录但没有passengers记录）
SELECT 
    u.id,
    u.username,
    u.user_type,
    u.created_at
FROM users u
LEFT JOIN passengers p ON u.id = p.user_id
WHERE u.user_type = 'PASSENGER' AND p.id IS NULL;