# 司机端投诉功能实现

## 功能概述

为司机端添加了投诉乘客的功能，司机可以对乘车过程中遇到问题的乘客进行投诉。

## 实现的功能

### 1. 司机端投诉页面 (`DriverComplaints.vue`)
- **投诉列表展示**: 显示司机提交的所有投诉记录
- **创建投诉**: 司机可以选择已完成的订单投诉相关乘客
- **投诉详情查看**: 点击投诉记录查看详细信息
- **状态跟踪**: 实时显示投诉处理状态

### 2. 投诉类型
- **乘客行为不当**: 乘客在乘车过程中的不当行为
- **安全问题**: 涉及行车安全的问题
- **费用纠纷**: 与费用相关的争议
- **服务态度**: 乘客服务态度问题
- **其他问题**: 其他类型的投诉

### 3. 后端API增强
- **智能被投诉人识别**: 根据投诉人身份自动确定被投诉人
- **用户ID统一**: 确保投诉记录中存储的是用户ID而非角色ID
- **订单关联**: 支持通过订单信息自动推断投诉关系

## 文件结构

```
frontend/
├── src/
│   ├── views/
│   │   └── DriverComplaints.vue          # 司机端投诉页面
│   └── router/
│       └── index.js                      # 路由配置（已更新）
├── test-driver-complaints.html           # 功能测试页面
└── README-DRIVER-COMPLAINTS.md          # 本文档

backend/
└── src/main/java/com/taxi/
    └── controller/
        ├── ComplaintController.java      # 投诉控制器（已增强）
        └── OrderController.java          # 订单控制器（已增强）
```

## 核心实现逻辑

### 1. 被投诉人识别逻辑

```java
// 根据投诉人身份确定被投诉人
if (isDriverComplainant) {
    // 司机投诉乘客：从订单中获取乘客ID，然后查找对应的用户ID
    Passenger passenger = passengerMapper.selectById(order.getPassengerId());
    defendantUserId = passenger.getUserId();
} else {
    // 乘客投诉司机：从订单中获取司机ID，然后查找对应的用户ID
    Driver driver = driverMapper.selectById(order.getDriverId());
    defendantUserId = driver.getUserId();
}
```

### 2. 前端订单获取

```javascript
// 使用通用API获取司机的订单
const response = await fetch(`/api/orders/user/${userId.value}/orders`);
// 只显示已完成的订单供投诉选择
availableOrders.value = (result.data || []).filter(order => order.status === 'COMPLETED');
```

### 3. 菜单集成

在Dashboard.vue中为司机角色添加了投诉菜单项：

```vue
<el-menu-item index="/dashboard/driver-complaints">
  <el-icon><Warning /></el-icon>
  <span>我的投诉</span>
</el-menu-item>
```

## 使用流程

### 司机投诉流程
1. **登录系统**: 使用司机账户登录
2. **进入投诉页面**: 点击侧边栏"我的投诉"
3. **创建投诉**: 点击"投诉乘客"按钮
4. **选择订单**: 从已完成订单中选择要投诉的订单
5. **填写信息**: 选择投诉类型，填写标题和描述
6. **提交投诉**: 点击提交，系统自动识别被投诉的乘客
7. **跟踪状态**: 在投诉列表中查看处理进度

### 管理员处理流程
1. **查看投诉**: 在投诉管理页面查看所有投诉
2. **区分类型**: 系统显示投诉人和被投诉人信息
3. **处理投诉**: 调查后给出处理结果
4. **状态更新**: 更新投诉状态为已解决或已关闭

## 数据库设计

投诉表中的关键字段：
- `complainant_id`: 投诉人的用户ID
- `defendant_id`: 被投诉人的用户ID（统一使用用户ID）
- `order_id`: 关联的订单ID
- `complaint_type`: 投诉类型
- `status`: 处理状态

## 测试说明

### 测试页面
打开 `test-driver-complaints.html` 进行功能测试：

1. **司机登录测试**: 使用司机账户登录
2. **获取订单列表**: 查看司机的已完成订单
3. **创建投诉**: 选择订单并提交投诉
4. **查看投诉列表**: 验证投诉记录

### 测试数据要求
- 至少有一个司机账户
- 司机有已完成的订单记录
- 订单关联了乘客信息

## 技术特点

### 1. 智能识别
- 自动根据投诉人身份确定被投诉人
- 无需前端手动指定被投诉人ID

### 2. 数据一致性
- 统一使用用户ID作为投诉人和被投诉人标识
- 避免了角色ID和用户ID混淆的问题

### 3. 用户体验
- 简洁的投诉创建流程
- 直观的状态显示和跟踪
- 响应式设计适配移动端

### 4. 扩展性
- 支持多种投诉类型
- 可扩展的状态管理
- 灵活的权限控制

## 注意事项

1. **权限控制**: 确保只有司机可以访问司机投诉页面
2. **数据验证**: 前后端都需要验证投诉数据的完整性
3. **状态同步**: 投诉状态变更需要及时通知相关用户
4. **隐私保护**: 投诉内容需要适当的隐私保护措施

## 后续优化建议

1. **实时通知**: 添加投诉状态变更的实时通知
2. **证据上传**: 完善证据文件上传功能
3. **投诉统计**: 添加投诉数据统计和分析
4. **自动处理**: 对于简单投诉实现自动处理逻辑
5. **评价系统**: 结合用户评价系统进行综合判断