# 司机注册和上线测试指南

## 问题修复说明

已修复司机注册时数据没有存入driver表的问题：

1. **修复了UserServiceImpl.java**：在司机注册时正确创建Driver记录
2. **增强了RegisterRequest.java**：添加了司机专用字段（驾驶证号、驾龄、从业资格证号）
3. **完善了数据初始化**：提供了完整的测试数据

## 测试文件说明

### 1. 数据库测试数据
- `database/driver_test_data.sql` - 包含多个测试司机的完整数据

### 2. API测试数据
- `driver_online_test_data.json` - 司机上线测试数据
- `apifox_driver_complete_test.json` - 完整的Apifox测试集合

### 3. 自动化测试脚本
- `test_driver_registration.js` - 批量司机注册和上线测试脚本

## 使用步骤

### 1. 导入数据库测试数据
```sql
-- 在MySQL中执行
source database/driver_test_data.sql;
```

### 2. 在Apifox中测试

#### 方法一：导入测试集合
1. 打开Apifox
2. 导入 `apifox_driver_complete_test.json`
3. 设置环境变量 `baseUrl = http://localhost:8080`
4. 按顺序执行测试用例

#### 方法二：手动创建请求

**司机注册请求：**
```
POST http://localhost:8080/api/user/register
Content-Type: application/json

{
  "username": "driver_test_001",
  "password": "123456",
  "phone": "13800138888",
  "email": "driver001@test.com",
  "realName": "测试司机001",
  "idCard": "110101199001011234",
  "userType": "DRIVER",
  "driverLicense": "DL2024001234",
  "drivingYears": 5,
  "professionalLicense": "PL2024001234"
}
```

**司机上线请求：**
```
POST http://localhost:8080/api/drivers/1/online?latitude=39.9042&longitude=116.4074
```

### 3. 使用自动化脚本测试

```bash
# 安装依赖
npm install axios

# 运行测试脚本
node test_driver_registration.js
```

## 常见问题解决

### 1. 司机上线参数错误
**错误：** `Required request parameter 'latitude' for method parameter type BigDecimal is not present`

**解决：** 确保在Apifox中正确设置Query参数：
- latitude: 39.9042
- longitude: 116.4074

### 2. 司机注册后driver表无数据
**已修复：** UserServiceImpl现在会在司机注册时自动创建Driver记录

### 3. 经纬度坐标顺序问题 ⚠️
**问题：** 输入的经纬度与获取的结果相反

**原因：** Redis GEO使用 `(longitude, latitude)` 顺序，但很多地方使用 `(latitude, longitude)` 顺序

**诊断步骤：**
1. 运行坐标测试工具：`node coordinate_test_tool.js`
2. 检查数据库坐标：执行 `database/update_coordinates.sql` 中的查询语句
3. 验证北京地区坐标范围：
   - 纬度 (latitude): 39.4 - 41.0
   - 经度 (longitude): 115.4 - 117.5

**修复方法：**
- 如果坐标颠倒，执行 `database/update_coordinates.sql` 中的修复语句
- 检查 `DriverRedisService.java` 中的坐标顺序
- 确保前端传参顺序正确

### 4. 数据库连接问题
确保：
- MySQL服务正在运行
- 数据库连接配置正确
- 已创建taxi_operation_system数据库

## 测试验证

### 1. 验证司机注册
```sql
-- 查看用户表
SELECT * FROM users WHERE user_type = 'DRIVER';

-- 查看司机表
SELECT * FROM drivers;

-- 关联查询
SELECT u.real_name, u.phone, d.driver_license, d.driving_years 
FROM users u 
JOIN drivers d ON u.id = d.user_id 
WHERE u.user_type = 'DRIVER';
```

### 2. 验证司机上线
```sql
-- 查看在线司机
SELECT u.real_name, d.is_online, d.current_latitude, d.current_longitude 
FROM users u 
JOIN drivers d ON u.id = d.user_id 
WHERE d.is_online = TRUE;
```

### 3. API验证
```bash
# 获取在线司机列表
curl http://localhost:8080/api/drivers/online

# 获取附近司机
curl "http://localhost:8080/api/drivers/nearby?latitude=39.9042&longitude=116.4074&radiusKm=5.0"
```

## 注意事项

1. **参数格式**：latitude和longitude必须是有效的数字格式
2. **数据库约束**：驾驶证号必须唯一
3. **用户类型**：注册时userType必须是"DRIVER"
4. **手机号格式**：必须符合中国手机号格式
5. **身份证格式**：必须符合18位身份证号格式