-- 创建vehicles表
CREATE TABLE IF NOT EXISTS vehicles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    driver_id BIGINT NOT NULL,
    plate_number VARCHAR(20) NOT NULL UNIQUE,
    brand VARCHAR(50) NOT NULL,
    model VARCHAR(50) NOT NULL,
    color VARCHAR(30) NOT NULL,
    year INT,
    seats INT DEFAULT 5,
    vehicle_type VARCHAR(20) DEFAULT 'SEDAN',
    fuel_type VARCHAR(20) DEFAULT 'GASOLINE',
    insurance_number VARCHAR(50),
    insurance_expiry DATE,
    inspection_expiry DATE,
    status VARCHAR(20) DEFAULT 'PENDING',
    vehicle_image VARCHAR(255),
    is_active BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_driver_id (driver_id),
    INDEX idx_plate_number (plate_number),
    INDEX idx_status (status),
    INDEX idx_is_active (is_active),
    
    FOREIGN KEY (driver_id) REFERENCES drivers(id) ON DELETE CASCADE
);

-- 插入一些测试数据
INSERT INTO vehicles (driver_id, plate_number, brand, model, color, vehicle_type, status, is_active) VALUES
(1, '京A12345', '丰田', '凯美瑞', '白色', 'SEDAN', 'ACTIVE', TRUE),
(2, '京B67890', '本田', 'CR-V', '黑色', 'SUV', 'ACTIVE', TRUE),
(3, '京C11111', '大众', '帕萨特', '银色', 'SEDAN', 'PENDING', FALSE),
(1, '京D22222', '奔驰', 'E级', '蓝色', 'SEDAN', 'REJECTED', FALSE),
(2, '京E33333', '宝马', 'X3', '红色', 'SUV', 'INACTIVE', FALSE)
ON DUPLICATE KEY UPDATE 
    brand = VALUES(brand),
    model = VALUES(model),
    color = VALUES(color);

-- 验证数据
SELECT 'Vehicles table created and populated' as status;
SELECT COUNT(*) as vehicle_count FROM vehicles;
SELECT * FROM vehicles LIMIT 5;