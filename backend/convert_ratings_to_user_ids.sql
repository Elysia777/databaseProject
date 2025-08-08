-- 将ratings表中的passenger_id和driver_id转换为user_id
USE taxi_operation_system;

-- 1. 检查当前数据状态
SELECT '=== 当前数据状态检查 ===' as info;

-- 检查ratings表当前数据
SELECT 'Current ratings data:' as info;
SELECT id, order_id, rater_id, rated_id, rating_type, rating, comment FROM ratings ORDER BY id LIMIT 10;

-- 检查passengers表结构
SELECT 'Passengers table sample:' as info;
SELECT id as passenger_id, user_id FROM passengers ORDER BY id LIMIT 5;

-- 检查drivers表结构  
SELECT 'Drivers table sample:' as info;
SELECT id as driver_id, user_id FROM drivers ORDER BY id LIMIT 5;

-- 检查users表
SELECT 'Users table sample:' as info;
SELECT id, username, real_name, user_type FROM users ORDER BY id LIMIT 10;

-- 2. 分析当前ratings数据的ID类型
SELECT '=== 分析当前ratings数据 ===' as info;

-- 检查rater_id是否在passengers表中（作为passenger_id）
SELECT 'Rater IDs that exist in passengers table:' as info;
SELECT DISTINCT r.rater_id, p.user_id as passenger_user_id
FROM ratings r
INNER JOIN passengers p ON r.rater_id = p.id
ORDER BY r.rater_id;

-- 检查rated_id是否在drivers表中（作为driver_id）
SELECT 'Rated IDs that exist in drivers table:' as info;
SELECT DISTINCT r.rated_id, d.user_id as driver_user_id
FROM ratings r
INNER JOIN drivers d ON r.rated_id = d.id
ORDER BY r.rated_id;

-- 检查rater_id是否直接在users表中（作为user_id）
SELECT 'Rater IDs that exist directly in users table:' as info;
SELECT DISTINCT r.rater_id, u.username, u.real_name
FROM ratings r
INNER JOIN users u ON r.rater_id = u.id
ORDER BY r.rater_id;

-- 检查rated_id是否直接在users表中（作为user_id）
SELECT 'Rated IDs that exist directly in users table:' as info;
SELECT DISTINCT r.rated_id, u.username, u.real_name
FROM ratings r
INNER JOIN users u ON r.rated_id = u.id
ORDER BY r.rated_id;

-- 3. 创建备份表
SELECT '=== 创建备份表 ===' as info;
CREATE TABLE IF NOT EXISTS ratings_backup AS SELECT * FROM ratings;
SELECT 'Backup created with', COUNT(*), 'records' FROM ratings_backup;

-- 4. 更新ratings表，将passenger_id和driver_id转换为user_id
SELECT '=== 开始转换ID ===' as info;

-- 更新rater_id：如果它是passenger_id，转换为对应的user_id
UPDATE ratings r
INNER JOIN passengers p ON r.rater_id = p.id
SET r.rater_id = p.user_id
WHERE r.rating_type = 'PASSENGER_TO_DRIVER';

-- 更新rated_id：如果它是driver_id，转换为对应的user_id  
UPDATE ratings r
INNER JOIN drivers d ON r.rated_id = d.id
SET r.rated_id = d.user_id
WHERE r.rating_type = 'PASSENGER_TO_DRIVER';

-- 5. 验证转换结果
SELECT '=== 验证转换结果 ===' as info;

-- 检查转换后的数据
SELECT 'Ratings after conversion:' as info;
SELECT id, order_id, rater_id, rated_id, rating_type, rating, comment FROM ratings ORDER BY id LIMIT 10;

-- 测试关联查询
SELECT 'Testing join query after conversion:' as info;
SELECT 
    r.id,
    r.order_id as orderId,
    r.rater_id as reviewerId,
    r.rated_id as revieweeId,
    r.rating_type as reviewType,
    r.rating,
    r.comment,
    r.created_at as createdAt,
    COALESCE(rater.real_name, rater.username, CONCAT('用户', r.rater_id)) as reviewerName,
    rater.avatar as reviewerAvatar,
    COALESCE(rated.real_name, rated.username, CONCAT('用户', r.rated_id)) as revieweeName,
    rated.avatar as revieweeAvatar,
    CASE 
        WHEN rater.id IS NULL THEN 'Missing rater user'
        WHEN rated.id IS NULL THEN 'Missing rated user'
        WHEN COALESCE(rater.real_name, rater.username) IS NULL THEN 'Rater has no name'
        WHEN COALESCE(rated.real_name, rated.username) IS NULL THEN 'Rated has no name'
        ELSE 'OK'
    END as status
FROM ratings r
LEFT JOIN users rater ON r.rater_id = rater.id
LEFT JOIN users rated ON r.rated_id = rated.id
ORDER BY r.id
LIMIT 10;

-- 统计转换结果
SELECT 'Conversion summary:' as info;
SELECT 
    COUNT(*) as total_ratings,
    COUNT(CASE WHEN rater.id IS NOT NULL THEN 1 END) as ratings_with_rater_user,
    COUNT(CASE WHEN rated.id IS NOT NULL THEN 1 END) as ratings_with_rated_user,
    COUNT(CASE WHEN rater.id IS NOT NULL AND rated.id IS NOT NULL THEN 1 END) as ratings_with_both_users,
    COUNT(CASE WHEN COALESCE(rater.real_name, rater.username) IS NOT NULL THEN 1 END) as ratings_with_rater_name,
    COUNT(CASE WHEN COALESCE(rated.real_name, rated.username) IS NOT NULL THEN 1 END) as ratings_with_rated_name,
    ROUND(COUNT(CASE WHEN rater.id IS NOT NULL AND rated.id IS NOT NULL THEN 1 END) * 100.0 / COUNT(*), 2) as success_rate_percent
FROM ratings r
LEFT JOIN users rater ON r.rater_id = rater.id
LEFT JOIN users rated ON r.rated_id = rated.id;

-- 6. 如果转换成功，可以选择删除备份表（手动执行）
-- DROP TABLE ratings_backup;

SELECT 'Conversion completed successfully!' as info;
SELECT 'Note: ratings_backup table contains the original data for safety.' as info;