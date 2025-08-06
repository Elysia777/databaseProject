# 数据库表结构分析

## 📊 当前使用的表

根据项目代码分析，当前网约车系统使用了以下5个主要数据表：

### 1. **users** 表 - 用户基础信息表 ✅ **正在使用**
**用途**: 存储所有用户（管理员、司机、乘客）的基础信息

**字段**:
- `id` - 用户ID（主键）
- `username` - 用户名
- `password` - 密码
- `phone` - 手机号
- `email` - 邮箱
- `real_name` - 真实姓名
- `id_card` - 身份证号
- `avatar` - 头像URL
- `user_type` - 用户类型（ADMIN/DRIVER/PASSENGER）
- `status` - 用户状态（ACTIVE/INACTIVE/BANNED）
- `created_at` - 创建时间
- `updated_at` - 更新时间

**使用情况**: 
- ✅ 用户登录认证
- ✅ 用户信息管理
- ✅ 权限控制

### 2. **drivers** 表 - 司机专业信息表 ✅ **正在使用**
**用途**: 存储司机的专业信息和状态

**字段**:
- `id` - 司机ID（主键）
- `user_id` - 关联用户ID（外键）
- `driver_license` - 驾驶证号
- `driver_license_expiry` - 驾驶证到期日期
- `professional_license` - 从业资格证号
- `professional_license_expiry` - 从业资格证到期日期
- `driving_years` - 驾龄
- `total_mileage` - 总里程
- `rating` - 评分
- `total_orders` - 总订单数
- `completed_orders` - 完成订单数
- `cancelled_orders` - 取消订单数
- `is_online` - 是否在线
- `current_latitude` - 当前纬度
- `current_longitude` - 当前经度
- `created_at` - 创建时间
- `updated_at` - 更新时间

**使用情况**:
- ✅ 司机上线/下线管理
- ✅ 司机位置追踪
- ✅ 司机评分统计
- ✅ 订单分配

### 3. **vehicles** 表 - 车辆信息表 ✅ **正在使用**
**用途**: 存储司机的车辆信息

**字段**:
- `id` - 车辆ID（主键）
- `driver_id` - 司机ID（外键）
- `plate_number` - 车牌号
- `brand` - 品牌
- `model` - 型号
- `color` - 颜色
- `year` - 年份
- `seats` - 座位数
- `vehicle_type` - 车辆类型（ECONOMY/COMFORT/PREMIUM）
- `fuel_type` - 燃料类型
- `insurance_number` - 保险单号
- `insurance_expiry` - 保险到期日期
- `inspection_expiry` - 年检到期日期
- `is_active` - 是否激活
- `created_at` - 创建时间
- `updated_at` - 更新时间

**使用情况**:
- ✅ 司机车辆管理
- ✅ 乘客端显示司机车辆信息
- ✅ 车辆类型选择

### 4. **orders** 表 - 订单信息表 ✅ **正在使用**
**用途**: 存储所有订单信息

**字段**:
- `id` - 订单ID（主键）
- `order_number` - 订单号
- `passenger_id` - 乘客ID
- `driver_id` - 司机ID
- `vehicle_id` - 车辆ID
- `order_type` - 订单类型（REAL_TIME/RESERVATION）
- `status` - 订单状态（PENDING/ASSIGNED/PICKUP/IN_PROGRESS/COMPLETED/CANCELLED）
- `pickup_address` - 上车地址
- `pickup_latitude` - 上车纬度
- `pickup_longitude` - 上车经度
- `destination_address` - 目的地地址
- `destination_latitude` - 目的地纬度
- `destination_longitude` - 目的地经度
- `estimated_distance` - 预估距离
- `estimated_duration` - 预估时长
- `estimated_fare` - 预估费用
- `actual_distance` - 实际距离
- `actual_duration` - 实际时长
- `actual_fare` - 实际费用
- `service_fee` - 服务费
- `total_fare` - 总费用
- `payment_method` - 支付方式
- `payment_status` - 支付状态
- `scheduled_time` - 预约时间
- `pickup_time` - 上车时间
- `completion_time` - 完成时间
- `cancel_reason` - 取消原因
- `created_at` - 创建时间
- `updated_at` - 更新时间

**使用情况**:
- ✅ 订单创建和管理
- ✅ 订单状态跟踪
- ✅ 预约单功能
- ✅ 费用计算
- ✅ 司机收入统计

### 5. **passengers** 表 - 乘客专业信息表 ⚠️ **定义了但很少使用**
**用途**: 存储乘客的专业信息

**字段**:
- `id` - 乘客ID（主键）
- `user_id` - 关联用户ID（外键）
- `emergency_contact` - 紧急联系人
- `emergency_phone` - 紧急联系人电话
- `default_payment_method` - 默认支付方式
- `rating` - 评分
- `total_orders` - 总订单数
- `total_spent` - 总消费金额
- `created_at` - 创建时间
- `updated_at` - 更新时间

**使用情况**:
- ⚠️ **很少使用** - 目前系统主要直接使用users表中的用户ID作为乘客ID
- 🔧 **可以优化** - 可以用来存储乘客的详细信息和统计数据

## 📈 表使用频率分析

### 高频使用 🔥
1. **orders** - 每次下单、接单、状态更新都会操作
2. **drivers** - 司机上线、位置更新、状态查询频繁
3. **users** - 登录认证、用户信息查询

### 中频使用 📊
4. **vehicles** - 司机车辆管理、乘客查看司机信息时使用

### 低频使用 ⚠️
5. **passengers** - 目前基本未使用，主要用users表代替

## 🔧 优化建议

### 1. passengers表的利用
**当前问题**: passengers表定义了但很少使用，乘客相关功能主要依赖users表

**建议**:
- 可以用来存储乘客的详细偏好设置
- 乘客的历史订单统计
- 乘客的信用评分
- 常用地址收藏

### 2. 可能缺少的表
根据完整的网约车系统，可能还需要：

**支付相关表**:
- `payments` - 支付记录表
- `refunds` - 退款记录表

**评价相关表**:
- `reviews` - 评价表（司机评价乘客，乘客评价司机）

**优惠相关表**:
- `coupons` - 优惠券表
- `promotions` - 促销活动表

**系统配置表**:
- `system_configs` - 系统配置表
- `fare_rules` - 计费规则表

### 3. 索引优化建议
```sql
-- 订单表常用查询索引
CREATE INDEX idx_orders_passenger_status ON orders(passenger_id, status);
CREATE INDEX idx_orders_driver_status ON orders(driver_id, status);
CREATE INDEX idx_orders_created_at ON orders(created_at);

-- 司机表位置查询索引
CREATE INDEX idx_drivers_location ON drivers(current_latitude, current_longitude, is_online);

-- 用户表登录查询索引
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_phone ON users(phone);
```

## 📝 总结

当前系统的5个表基本满足了网约车的核心功能需求：
- ✅ 用户管理（users）
- ✅ 司机管理（drivers）
- ✅ 车辆管理（vehicles）
- ✅ 订单管理（orders）
- ⚠️ 乘客管理（passengers，使用率低）

系统设计相对简洁，核心功能完整，但在乘客详细信息管理和一些扩展功能（支付、评价、优惠等）方面还有优化空间。