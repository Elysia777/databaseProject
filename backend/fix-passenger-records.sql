-- 修复缺失的passengers记录
USE taxi_operation_system;

-- 为所有缺少passengers记录的PASSENGER用户创建记录
INSERT INTO passengers (user_id, default_payment_method, rating, total_orders, total_spent, created_at, updated_at)
SELECT 
    u.id,
    'WECHAT' as default_payment_method,
    5.00 as rating,
    0 as total_orders,
    0.00 as total_spent,
    NOW() as created_at,
    NOW() as updated_at
FROM users u
LEFT JOIN passengers p ON u.id = p.user_id
WHERE u.user_type = 'PASSENGER' AND p.id IS NULL;

-- 检查修复结果
SELECT 
    u.id as user_id, 
    u.username, 
    u.user_type, 
    p.id as passenger_id,
    CASE 
        WHEN p.id IS NULL THEN '❌ 仍然缺少passengers记录'
        ELSE '✅ 关联正常'
    END as status
FROM users u 
LEFT JOIN passengers p ON u.id = p.user_id 
WHERE u.user_type = 'PASSENGER'
ORDER BY u.id;