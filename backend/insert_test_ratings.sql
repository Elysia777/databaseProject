-- 插入测试评价数据到ratings表
USE taxi_operation_system;

-- 检查当前数据
SELECT 'Current ratings count:' as info, COUNT(*) as count FROM ratings;

-- 插入测试数据（如果表为空）
INSERT INTO ratings (order_id, rater_id, rated_id, rating_type, rating, comment, tags, created_at) 
SELECT * FROM (
    SELECT 1 as order_id, 1 as rater_id, 2 as rated_id, 'PASSENGER_TO_DRIVER' as rating_type, 5.0 as rating, '司机师傅很好，服务态度很棒！车辆干净，驾驶平稳。' as comment, '["服务好", "准时", "车辆干净"]' as tags, NOW() as created_at
    UNION ALL
    SELECT 2, 1, 2, 'PASSENGER_TO_DRIVER', 4.5, '总体不错，就是路上有点堵车，但司机很耐心。', '["服务好", "耐心"]', NOW()
    UNION ALL
    SELECT 3, 3, 2, 'PASSENGER_TO_DRIVER', 4.0, '司机很准时，车辆干净，空调温度刚好。', '["准时", "车辆干净"]', NOW()
    UNION ALL
    SELECT 4, 3, 4, 'PASSENGER_TO_DRIVER', 4.8, '非常满意的一次出行，司机专业，路线合理。', '["专业", "路线好"]', NOW()
    UNION ALL
    SELECT 5, 1, 4, 'PASSENGER_TO_DRIVER', 3.5, '司机态度一般，但是准时到达了。', '["准时"]', NOW()
    UNION ALL
    SELECT 6, 3, 2, 'PASSENGER_TO_DRIVER', 5.0, '完美的出行体验！司机非常友善，车辆很新。', '["友善", "车辆新", "服务好"]', NOW()
    UNION ALL
    SELECT 7, 1, 4, 'PASSENGER_TO_DRIVER', 4.2, '司机开车很稳，就是路上聊天有点多。', '["驾驶平稳"]', NOW()
    UNION ALL
    SELECT 8, 3, 2, 'PASSENGER_TO_DRIVER', 4.7, '司机很专业，帮忙搬行李，服务很好。', '["专业", "服务好", "热心"]', NOW()
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM ratings LIMIT 1);

-- 检查插入结果
SELECT 'After insert:' as info, COUNT(*) as count FROM ratings;

-- 显示插入的数据
SELECT 
    r.id,
    r.order_id,
    r.rater_id,
    r.rated_id,
    r.rating_type,
    r.rating,
    r.comment,
    r.created_at
FROM ratings r
ORDER BY r.created_at DESC
LIMIT 10;