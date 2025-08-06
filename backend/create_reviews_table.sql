-- 创建评价表
CREATE TABLE IF NOT EXISTS reviews (
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
    INDEX idx_created_at (created_at),
    
    -- 外键约束
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (passenger_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (driver_id) REFERENCES drivers(id) ON DELETE CASCADE,
    
    -- 唯一约束：每个订单只能评价一次
    UNIQUE KEY uk_order_review (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价表';

-- 插入测试数据
INSERT INTO reviews (order_id, passenger_id, driver_id, rating, comment, tags, is_anonymous) VALUES
(1, 1, 1, 5.0, '司机师傅很好，服务态度很棒！', '["服务好", "准时", "车辆干净"]', FALSE),
(2, 1, 1, 4.5, '总体不错，就是路上有点堵车', '["服务好", "驾驶平稳"]', FALSE);

-- 更新司机评分（基于评价计算平均分）
UPDATE drivers d 
SET rating = (
    SELECT COALESCE(AVG(r.rating), 5.0) 
    FROM reviews r 
    WHERE r.driver_id = d.id
)
WHERE EXISTS (
    SELECT 1 FROM reviews r WHERE r.driver_id = d.id
);