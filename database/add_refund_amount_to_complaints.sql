-- 为投诉表添加退款金额字段
-- 如果表已存在但没有退款金额字段，执行此脚本

USE taxi_operation_system;

-- 检查是否已存在 refund_amount 字段
SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'taxi_operation_system'
    AND TABLE_NAME = 'complaints'
    AND COLUMN_NAME = 'refund_amount'
);

-- 如果字段不存在，则添加
SET @sql = IF(@column_exists = 0,
    'ALTER TABLE complaints ADD COLUMN refund_amount DECIMAL(10,2) DEFAULT 0.00 COMMENT ''退款金额'' AFTER resolution',
    'SELECT ''Column refund_amount already exists'' as message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 显示表结构确认
DESCRIBE complaints;