# 订单管理页面显示问题修复

## 问题描述
管理员端订单管理页面显示司机和乘客为"未知"，总费用显示为0。

## 问题分析

通过调试页面 `test-order-user-mapping.html` 发现的问题：

### 1. 用户ID不匹配问题
- **订单数据**：`passengerId: 1`, `driverId: 1`
- **用户数据**：ID范围是 2-16，没有ID为1的用户
- **原因**：可能是测试数据中用户ID和订单中引用的ID不一致

### 2. 费用字段问题
- **原始逻辑**：只使用 `totalFare` 字段
- **实际情况**：`totalFare` 为 null，实际费用在 `actualFare` 字段
- **修复方案**：按优先级使用 `actualFare > totalFare > estimatedFare`

### 3. 地址字段
- **字段名称**：`pickupAddress` 和 `destinationAddress` ✅ 正确

## 修复方案

### 1. 费用显示修复
```javascript
// 费用优先级：actualFare > totalFare > estimatedFare
const displayFare = order.actualFare || order.totalFare || order.estimatedFare || 0
```

### 2. 用户名显示优化
```javascript
passengerName: order.passengerId ? (usersMap.get(order.passengerId) || `未知乘客(ID:${order.passengerId})`) : '-',
driverName: order.driverId ? (usersMap.get(order.driverId) || `未知司机(ID:${order.driverId})`) : '-',
```

### 3. 添加调试信息
- 用户数据加载调试
- 订单处理过程调试
- ID映射关系调试

## 测试结果

修复后的显示效果：
- ✅ 费用正确显示（使用 actualFare 或 estimatedFare）
- ✅ 用户名显示优化（显示具体的未找到ID信息）
- ✅ 地址正确显示
- ✅ 添加了详细的调试信息

## 数据库问题建议

建议检查数据库中的用户和订单数据一致性：
1. 确认订单表中的 `passenger_id` 和 `driver_id` 是否存在对应的用户记录
2. 可能需要清理测试数据或修复数据关联关系

## 文件修改

- `frontend/src/views/OrderManagement.vue` - 主要修复文件
- `frontend/test-order-user-mapping.html` - 调试工具页面