-- 为vehicles表添加缺失的字段

-- 添加status字段（车辆状态）
ALTER TABLE vehicles ADD COLUMN status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '车辆状态: ACTIVE-活跃, PENDING-待审核, REJECTED-已拒绝, INACTIVE-已停用';

-- 添加vehicle_image字段（车辆图片）
ALTER TABLE vehicles ADD COLUMN vehicle_image VARCHAR(255) COMMENT '车辆图片路径';

-- 更新现有数据的status字段为ACTIVE
UPDATE vehicles SET status = 'ACTIVE' WHERE status IS NULL;

-- 查看更新后的表结构
DESCRIBE vehicles;

-- 查看更新后的数据
SELECT id, plate_number, brand, model, status, vehicle_image, is_active FROM vehicles LIMIT 5;