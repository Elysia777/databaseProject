-- 验证和修复ratings表中的用户ID问题
-- 确保rater_id和rated_id都是用户ID而不是乘客/司机ID

-- 1. 首先检查当前ratings表的数据结构和内容
SELECT 'Current ratings table structure:' as info;
DESCRIBE ratings;

SELECT 'Current ratings data sample:' as info;
SELECT * FROM ratings LIMIT 5;

-- 2. 检查是否存在用户ID不一致的问题
SELECT 'Checking for inconsistent user IDs:' as info;

-- 检查rater_id是否都是有效的用户ID
SELECT 
    'Invalid rater_id count:' as check_type,
    COUNT(*) as count
FROM ratings r
LEFT JOIN users u ON r.rater_id = u.id
WHERE u.id IS NULL;

-- 检查rated_id是否都是有效的用户ID  
SELECT 
    'Invalid rated_id count:' as check_type,
    COUNT(*) as count
FROM ratings r
LEFT JOIN users u ON r.rated_id = u.id
WHERE u.id IS NULL;

-- 3. 如果发现问题，显示具体的问题记录
SELECT 'Problematic ratings records:' as info;
SELECT 
    r.id,
    r.order_id,
    r.rater_id,
    r.rated_id,
    r.rating_type,
    'rater_id not found in users' as issue
FROM ratings r
LEFT JOIN users u ON r.rater_id = u.id
WHERE u.id IS NULL

UNION ALL

SELECT 
    r.id,
    r.order_id,
    r.rater_id,
    r.rated_id,
    r.rating_type,
    'rated_id not found in users' as issue
FROM ratings r
LEFT JOIN users u ON r.rated_id = u.id
WHERE u.id IS NULL;

-- 4. 检查订单表中的用户ID映射
SELECT 'Order user ID mapping check:' as info;
SELECT 
    o.id as order_id,
    o.passenger_id,
    p.user_id as passenger_user_id,
    o.driver_id,
    d.user_id as driver_user_id
FROM orders o
LEFT JOIN passengers p ON o.passenger_id = p.id
LEFT JOIN drivers d ON o.driver_id = d.id
LIMIT 10;

-- 5. 修复脚本：如果rater_id和rated_id是passenger_id/driver_id而不是user_id
-- 注意：运行前请备份数据！

-- 备份当前ratings表
CREATE TABLE ratings_backup_before_userid_fix AS SELECT * FROM ratings;

-- 修复rater_id：如果它实际上是passenger_id，转换为user_id
UPDATE ratings r
JOIN passengers p ON r.rater_id = p.id
SET r.rater_id = p.user_id
WHERE r.rating_type = 'PASSENGER_TO_DRIVER'
AND EXISTS (
    SELECT 1 FROM passengers p2 WHERE p2.id = r.rater_id
);

-- 修复rated_id：如果它实际上是driver_id，转换为user_id
UPDATE ratings r
JOIN drivers d ON r.rated_id = d.id
SET r.rated_id = d.user_id
WHERE r.rating_type = 'PASSENGER_TO_DRIVER'
AND EXISTS (
    SELECT 1 FROM drivers d2 WHERE d2.id = r.rated_id
);

-- 6. 验证修复结果
SELECT 'After fix - checking user ID validity:' as info;

-- 检查修复后的rater_id
SELECT 
    'Valid rater_id count after fix:' as check_type,
    COUNT(*) as count
FROM ratings r
JOIN users u ON r.rater_id = u.id;

-- 检查修复后的rated_id
SELECT 
    'Valid rated_id count after fix:' as check_type,
    COUNT(*) as count
FROM ratings r
JOIN users u ON r.rated_id = u.id;

-- 7. 显示修复后的数据样本
SELECT 'Fixed ratings data sample:' as info;
SELECT 
    r.id,
    r.order_id,
    r.rater_id,
    ru.real_name as rater_name,
    r.rated_id,
    rau.real_name as rated_name,
    r.rating,
    r.comment,
    r.created_at
FROM ratings r
LEFT JOIN users ru ON r.rater_id = ru.id
LEFT JOIN users rau ON r.rated_id = rau.id
ORDER BY r.created_at DESC
LIMIT 10;

-- 8. 验证与订单的关联是否正确
SELECT 'Verification with orders:' as info;
SELECT 
    r.id as rating_id,
    r.order_id,
    o.passenger_id,
    p.user_id as passenger_user_id,
    r.rater_id,
    o.driver_id,
    d.user_id as driver_user_id,
    r.rated_id,
    CASE 
        WHEN r.rater_id = p.user_id AND r.rated_id = d.user_id THEN 'CORRECT'
        ELSE 'INCORRECT'
    END as mapping_status
FROM ratings r
JOIN orders o ON r.order_id = o.id
LEFT JOIN passengers p ON o.passenger_id = p.id
LEFT JOIN drivers d ON o.driver_id = d.id
LIMIT 10;

-- 9. 创建索引以提高查询性能（如果不存在）
CREATE INDEX IF NOT EXISTS idx_ratings_rater_id ON ratings(rater_id);
CREATE INDEX IF NOT EXISTS idx_ratings_rated_id ON ratings(rated_id);
CREATE INDEX IF NOT EXISTS idx_ratings_order_id ON ratings(order_id);

-- 10. 最终统计
SELECT 'Final statistics:' as info;
SELECT 
    COUNT(*) as total_ratings,
    COUNT(CASE WHEN ru.id IS NOT NULL THEN 1 END) as valid_rater_ids,
    COUNT(CASE WHEN rau.id IS NOT NULL THEN 1 END) as valid_rated_ids,
    COUNT(CASE WHEN ru.id IS NOT NULL AND rau.id IS NOT NULL THEN 1 END) as fully_valid_ratings
FROM ratings r
LEFT JOIN users ru ON r.rater_id = ru.id
LEFT JOIN users rau ON r.rated_id = rau.id;

SELECT 'Ratings table user ID fix completed!' as status;