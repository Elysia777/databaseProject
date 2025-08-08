-- 优化司机收入统计查询性能的SQL脚本

-- 1. 为订单表的关键字段添加索引
-- 为司机ID和完成时间创建复合索引，优化按月份查询
CREATE INDEX IF NOT EXISTS idx_orders_driver_completion 
ON orders(driver_id, completion_time, status);

-- 为完成时间单独创建索引，优化日期范围查询
CREATE INDEX IF NOT EXISTS idx_orders_completion_time 
ON orders(completion_time);

-- 为司机ID和状态创建复合索引，优化司机订单查询
CREATE INDEX IF NOT EXISTS idx_orders_driver_status 
ON orders(driver_id, status);

-- 2. 检查现有索引
SHOW INDEX FROM orders WHERE Key_name LIKE '%driver%' OR Key_name LIKE '%completion%';

-- 3. 分析查询性能（可选）
-- EXPLAIN SELECT 
--     COALESCE(SUM(total_fare), 0) as totalEarnings,
--     COUNT(*) as totalOrders,
--     COALESCE(AVG(total_fare), 0) as averageEarnings,
--     COALESCE(SUM(actual_distance), 0) as totalDistance
-- FROM orders
-- WHERE driver_id = 1
-- AND status = 'COMPLETED'
-- AND DATE_FORMAT(completion_time, '%Y-%m') = '2024-12';

-- 4. 优化建议
-- 如果数据量很大，可以考虑：
-- - 创建按月分区的表
-- - 使用物化视图存储预计算的统计数据
-- - 定期清理历史数据

-- 5. 验证索引效果
-- 查看索引使用情况
-- SHOW STATUS LIKE 'Handler_read%';

-- 6. 为收入统计创建视图（可选）
CREATE OR REPLACE VIEW driver_monthly_earnings AS
SELECT 
    driver_id,
    DATE_FORMAT(completion_time, '%Y-%m') as month,
    COUNT(*) as total_orders,
    COALESCE(SUM(total_fare), 0) as total_earnings,
    COALESCE(AVG(total_fare), 0) as average_earnings,
    COALESCE(SUM(actual_distance), 0) as total_distance,
    MIN(completion_time) as first_order_time,
    MAX(completion_time) as last_order_time
FROM orders
WHERE status = 'COMPLETED'
AND completion_time IS NOT NULL
GROUP BY driver_id, DATE_FORMAT(completion_time, '%Y-%m');

-- 使用视图查询示例：
-- SELECT * FROM driver_monthly_earnings 
-- WHERE driver_id = 1 AND month = '2024-12';

-- 7. 创建每日收入统计视图
CREATE OR REPLACE VIEW driver_daily_earnings AS
SELECT 
    driver_id,
    DATE(completion_time) as date,
    DATE_FORMAT(completion_time, '%Y-%m') as month,
    COUNT(*) as order_count,
    COALESCE(SUM(total_fare), 0) as total_earnings,
    COALESCE(AVG(total_fare), 0) as average_earnings,
    COALESCE(SUM(actual_distance), 0) as total_distance
FROM orders
WHERE status = 'COMPLETED'
AND completion_time IS NOT NULL
GROUP BY driver_id, DATE(completion_time);

-- 使用每日统计视图：
-- SELECT * FROM driver_daily_earnings 
-- WHERE driver_id = 1 AND month = '2024-12'
-- ORDER BY date DESC;

-- 8. 检查数据完整性
-- 确保关键字段不为空
SELECT COUNT(*) as total_orders,
       COUNT(completion_time) as has_completion_time,
       COUNT(total_fare) as has_total_fare,
       COUNT(actual_distance) as has_actual_distance
FROM orders 
WHERE status = 'COMPLETED';

-- 9. 清理无效数据（谨慎执行）
-- UPDATE orders 
-- SET total_fare = estimated_fare 
-- WHERE status = 'COMPLETED' 
-- AND total_fare IS NULL 
-- AND estimated_fare IS NOT NULL;

-- 10. 性能监控查询
-- 查看最慢的查询
-- SELECT * FROM information_schema.PROCESSLIST 
-- WHERE COMMAND = 'Query' AND TIME > 1;