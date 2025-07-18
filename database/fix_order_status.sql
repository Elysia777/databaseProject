-- 修复订单状态字段的SQL脚本
USE taxi_operation_system;

-- 查看当前orders表的status字段定义
DESCRIBE orders;

-- 修改status字段，添加DISPATCHING状态
ALTER TABLE orders 
MODIFY COLUMN status ENUM(
    'PENDING',      -- 待分配
    'DISPATCHING',  -- 分配中
    'ASSIGNED',     -- 已分配司机
    'PICKUP',       -- 司机到达上车点
    'IN_PROGRESS',  -- 行程中
    'COMPLETED',    -- 已完成
    'CANCELLED'     -- 已取消
) DEFAULT 'PENDING' COMMENT '订单状态';

-- 验证修改结果
DESCRIBE orders;

-- 查看现有订单的状态
SELECT id, order_number, status, created_at 
FROM orders 
ORDER BY created_at DESC 
LIMIT 10;