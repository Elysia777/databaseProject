-- 专门修复用户ID为3的乘客记录
USE taxi_operation_system;

-- 检查用户3的当前状态
SELECT 'User 3 info:' as info;
SELECT id, username, user_type FROM users WHERE id = 3;

SELECT 'Passenger record for user 3:' as info;
SELECT * FROM passengers WHERE user_id = 3;

-- 如果没有passengers记录，创建一个
INSERT IGNORE INTO passengers (user_id, default_payment_method, rating, total_orders, total_spent, created_at, updated_at)
VALUES (3, 'WECHAT', 5.00, 0, 0.00, NOW(), NOW());

-- 验证修复结果
SELECT 'After fix:' as info;
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
WHERE u.id = 3;