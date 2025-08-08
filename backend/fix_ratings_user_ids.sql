-- 修复ratings表中的用户ID问题
USE taxi_operation_system;

-- 1. 检查当前数据状态
SELECT '=== 当前数据检查 ===' as info;

-- 检查用户表
SELECT 'Users table:' as info;
SELECT id, username, real_name, user_type FROM users ORDER BY id LIMIT 10;

-- 检查当前ratings数据
SELECT 'Current ratings:' as info;
SELECT id, order_id, rater_id, rated_id, rating_type, rating, comment FROM ratings ORDER BY id LIMIT 10;

-- 检查关联问题
SELECT 'Checking rater_id in users:' as info;
SELECT 
    r.rater_id,
    u.id as user_id,
    u.username,
    u.real_name,
    CASE WHEN u.id IS NULL THEN 'NOT FOUND' ELSE 'FOUND' END as status
FROM ratings r
LEFT JOIN users u ON r.rater_id = u.id
GROUP BY r.rater_id, u.id, u.username, u.real_name
ORDER BY r.rater_id;

SELECT 'Checking rated_id in users:' as info;
SELECT 
    r.rated_id,
    u.id as user_id,
    u.username,
    u.real_name,
    CASE WHEN u.id IS NULL THEN 'NOT FOUND' ELSE 'FOUND' END as status
FROM ratings r
LEFT JOIN users u ON r.rated_id = u.id
GROUP BY r.rated_id, u.id, u.username, u.real_name
ORDER BY r.rated_id;

-- 2. 如果需要，创建缺失的用户记录
SELECT '=== 创建缺失的用户记录 ===' as info;

-- 为缺失的rater_id创建用户记录
INSERT IGNORE INTO users (id, username, real_name, user_type, password, phone, status, created_at)
SELECT 
    missing_rater_id,
    CONCAT('passenger_', missing_rater_id) as username,
    CONCAT('乘客', missing_rater_id) as real_name,
    'PASSENGER' as user_type,
    '$2a$10$defaultpasswordhash' as password,
    CONCAT('138', LPAD(missing_rater_id, 8, '0')) as phone,
    'ACTIVE' as status,
    NOW() as created_at
FROM (
    SELECT DISTINCT r.rater_id as missing_rater_id
    FROM ratings r
    LEFT JOIN users u ON r.rater_id = u.id
    WHERE u.id IS NULL
) AS missing_raters;

-- 为缺失的rated_id创建用户记录
INSERT IGNORE INTO users (id, username, real_name, user_type, password, phone, status, created_at)
SELECT 
    missing_rated_id,
    CONCAT('driver_', missing_rated_id) as username,
    CONCAT('司机', missing_rated_id) as real_name,
    'DRIVER' as user_type,
    '$2a$10$defaultpasswordhash' as password,
    CONCAT('139', LPAD(missing_rated_id, 8, '0')) as phone,
    'ACTIVE' as status,
    NOW() as created_at
FROM (
    SELECT DISTINCT r.rated_id as missing_rated_id
    FROM ratings r
    LEFT JOIN users u ON r.rated_id = u.id
    WHERE u.id IS NULL
) AS missing_rateds;

-- 3. 验证修复结果
SELECT '=== 修复结果验证 ===' as info;

-- 测试关联查询
SELECT 'Testing join query after fix:' as info;
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
        WHEN rater.id IS NULL THEN 'Missing rater'
        WHEN rated.id IS NULL THEN 'Missing rated'
        ELSE 'OK'
    END as join_status
FROM ratings r
LEFT JOIN users rater ON r.rater_id = rater.id
LEFT JOIN users rated ON r.rated_id = rated.id
ORDER BY r.id
LIMIT 10;

-- 统计修复结果
SELECT 'Fix summary:' as info;
SELECT 
    COUNT(*) as total_ratings,
    COUNT(CASE WHEN rater.id IS NOT NULL THEN 1 END) as ratings_with_rater,
    COUNT(CASE WHEN rated.id IS NOT NULL THEN 1 END) as ratings_with_rated,
    COUNT(CASE WHEN rater.id IS NOT NULL AND rated.id IS NOT NULL THEN 1 END) as ratings_with_both_users,
    COUNT(CASE WHEN COALESCE(rater.real_name, rater.username) IS NOT NULL THEN 1 END) as ratings_with_rater_name,
    COUNT(CASE WHEN COALESCE(rated.real_name, rated.username) IS NOT NULL THEN 1 END) as ratings_with_rated_name
FROM ratings r
LEFT JOIN users rater ON r.rater_id = rater.id
LEFT JOIN users rated ON r.rated_id = rated.id;

SELECT 'Fix completed successfully!' as info;