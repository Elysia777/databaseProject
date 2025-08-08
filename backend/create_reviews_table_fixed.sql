-- 创建评价表（修复版本）
USE taxi_operation_system;

-- 删除旧的reviews表（如果存在）
DROP TABLE IF EXISTS reviews;

-- 创建新的reviews表
CREATE TABLE reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '评价ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    passenger_id BIGINT NOT NULL COMMENT '乘客ID',
    driver_id BIGINT NOT NULL COMMENT '司机ID',
    rating DECIMAL(2,1) NOT NULL COMMENT '评分(1-5分)',
    comment TEXT COMMENT '评价内容',
    tags VARCHAR(500) COMMENT '评价标签(JSON格式)',
    is_anonymous BOOLEAN DEFAULT FALSE COMMENT '是否匿名评价',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 索引
    INDEX idx_order_id (order_id),
    INDEX idx_passenger_id (passenger_id),
    INDEX idx_driver_id (driver_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价表';

-- 插入测试数据
-- 注意：这里使用的ID需要根据实际的用户和订单数据调整

-- 首先检查是否有用户数据
SELECT 'Checking users...' as status;
SELECT id, username, real_name, user_type FROM users LIMIT 5;

-- 检查是否有订单数据
SELECT 'Checking orders...' as status;
SELECT id, order_number, passenger_id, driver_id FROM orders LIMIT 5;

-- 插入一些基础的测试评价数据
-- 注意：这里需要根据实际的数据库数据调整
-- passenger_id 应该是 users 表中 user_type='PASSENGER' 的用户ID
-- driver_id 应该是 drivers 表的ID（不是users表的ID）

-- 先查看现有的用户和司机数据
SELECT 'Current users:' as info;
SELECT id, username, real_name, user_type FROM users WHERE user_type IN ('PASSENGER', 'DRIVER') LIMIT 10;

SELECT 'Current drivers:' as info;
SELECT d.id as driver_id, d.user_id, u.username, u.real_name 
FROM drivers d 
LEFT JOIN users u ON d.user_id = u.id 
LIMIT 10;

-- 动态插入测试数据（基于现有数据）
-- 如果有乘客和司机数据，则插入评价
INSERT INTO reviews (order_id, passenger_id, driver_id, rating, comment, tags, is_anonymous)
SELECT 
    1 as order_id,
    (SELECT id FROM users WHERE user_type = 'PASSENGER' LIMIT 1) as passenger_id,
    (SELECT id FROM drivers LIMIT 1) as driver_id,
    5.0 as rating,
    '司机师傅很好，服务态度很棒！车辆干净，驾驶平稳。' as comment,
    '["服务好", "准时", "车辆干净", "驾驶平稳"]' as tags,
    FALSE as is_anonymous
WHERE EXISTS (SELECT 1 FROM users WHERE user_type = 'PASSENGER')
  AND EXISTS (SELECT 1 FROM drivers);

INSERT INTO reviews (order_id, passenger_id, driver_id, rating, comment, tags, is_anonymous)
SELECT 
    2 as order_id,
    (SELECT id FROM users WHERE user_type = 'PASSENGER' LIMIT 1) as passenger_id,
    (SELECT id FROM drivers LIMIT 1) as driver_id,
    4.5 as rating,
    '总体不错，就是路上有点堵车，但司机很耐心。' as comment,
    '["服务好", "驾驶平稳", "耐心"]' as tags,
    FALSE as is_anonymous
WHERE EXISTS (SELECT 1 FROM users WHERE user_type = 'PASSENGER')
  AND EXISTS (SELECT 1 FROM drivers);

INSERT INTO reviews (order_id, passenger_id, driver_id, rating, comment, tags, is_anonymous)
SELECT 
    3 as order_id,
    (SELECT id FROM users WHERE user_type = 'PASSENGER' LIMIT 1 OFFSET 1) as passenger_id,
    (SELECT id FROM drivers LIMIT 1) as driver_id,
    4.0 as rating,
    '司机很准时，车辆干净，就是空调有点冷。' as comment,
    '["准时", "车辆干净"]' as tags,
    FALSE as is_anonymous
WHERE EXISTS (SELECT 1 FROM users WHERE user_type = 'PASSENGER')
  AND EXISTS (SELECT 1 FROM drivers);

INSERT INTO reviews (order_id, passenger_id, driver_id, rating, comment, tags, is_anonymous)
SELECT 
    4 as order_id,
    (SELECT id FROM users WHERE user_type = 'PASSENGER' LIMIT 1) as passenger_id,
    (SELECT id FROM drivers LIMIT 1 OFFSET 1) as driver_id,
    4.8 as rating,
    '非常满意的一次出行，司机专业，路线合理。' as comment,
    '["专业", "路线好", "服务好"]' as tags,
    FALSE as is_anonymous
WHERE EXISTS (SELECT 1 FROM users WHERE user_type = 'PASSENGER')
  AND EXISTS (SELECT 1 FROM drivers LIMIT 1 OFFSET 1);

-- 检查插入的数据
SELECT 'Inserted review data:' as status;
SELECT 
    r.id,
    r.order_id,
    r.passenger_id,
    r.driver_id,
    r.rating,
    r.comment,
    r.created_at
FROM reviews r
ORDER BY r.created_at DESC;

-- 检查关联查询是否正常工作
SELECT 'Testing join query:' as status;
SELECT 
    r.id,
    r.order_id as orderId,
    r.passenger_id as reviewerId,
    r.driver_id as revieweeId,
    'passenger_to_driver' as reviewType,
    r.rating,
    r.comment,
    r.tags,
    r.created_at as createdAt,
    COALESCE(passenger.real_name, passenger.username) as reviewerName,
    passenger.avatar as reviewerAvatar,
    COALESCE(driver_user.real_name, driver_user.username) as revieweeName,
    driver_user.avatar as revieweeAvatar
FROM reviews r
LEFT JOIN users passenger ON r.passenger_id = passenger.id
LEFT JOIN drivers d ON r.driver_id = d.id
LEFT JOIN users driver_user ON d.user_id = driver_user.id
ORDER BY r.created_at DESC
LIMIT 5;

SELECT 'Reviews table setup completed!' as status;