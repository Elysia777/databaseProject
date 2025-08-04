-- 修复orders表的status字段，添加SCHEDULED状态
-- 当前status字段是ENUM类型，需要添加SCHEDULED状态

-- 根据日志显示，数据库名称可能是taxi_system
USE taxi_system;

-- 查看当前表结构
DESCRIBE orders;

-- 修改status字段，添加SCHEDULED状态到ENUM中
-- 原有状态：PENDING, ASSIGNED, PICKUP, IN_PROGRESS, COMPLETED, CANCELLED
-- 新增状态：SCHEDULED
ALTER TABLE orders MODIFY COLUMN status 
ENUM('PENDING', 'ASSIGNED', 'PICKUP', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'SCHEDULED') 
DEFAULT 'PENDING' 
COMMENT '订单状态：待分配、已分配、接客中、进行中、已完成、已取消、已预约';

-- 验证修改结果
DESCRIBE orders;

-- 查看当前所有订单的状态
SELECT id, order_number, status, order_type, scheduled_time, created_at 
FROM orders 
ORDER BY created_at DESC 
LIMIT 10;

-- 如果上面的数据库名称不对，尝试其他可能的数据库名称
-- USE taxi_operation_system;
-- 然后重复上面的ALTER TABLE语句