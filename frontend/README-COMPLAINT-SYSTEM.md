# 投诉系统功能实现

## 📋 系统概述

为出租车管理系统实现了完整的投诉功能，包括乘客端投诉提交和管理员端投诉处理两个主要模块。

## ✨ 功能特性

### 乘客端功能
- **投诉提交**：针对已完成的订单提交投诉
- **投诉类型**：服务质量、安全问题、费用问题、行为不当、其他
- **证据上传**：支持上传图片作为投诉证据
- **投诉管理**：查看自己的投诉列表和处理状态
- **投诉详情**：查看投诉的详细信息和处理结果

### 管理员端功能
- **投诉列表**：查看所有投诉记录
- **筛选功能**：按状态、类型筛选投诉
- **投诉处理**：更新投诉状态和处理结果
- **退款处理**：支持退款金额设置
- **统计数据**：投诉数量和类型统计

## 🗄️ 数据库设计

### 投诉表 (complaints)
```sql
CREATE TABLE complaints (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '投诉ID',
    order_id BIGINT COMMENT '订单ID',
    complainant_id BIGINT NOT NULL COMMENT '投诉人ID',
    defendant_id BIGINT NOT NULL COMMENT '被投诉人ID',
    complaint_type ENUM('SERVICE', 'SAFETY', 'PAYMENT', 'BEHAVIOR', 'OTHER') NOT NULL COMMENT '投诉类型',
    title VARCHAR(100) NOT NULL COMMENT '投诉标题',
    description TEXT NOT NULL COMMENT '投诉描述',
    evidence_files JSON COMMENT '证据文件',
    status ENUM('PENDING', 'INVESTIGATING', 'RESOLVED', 'CLOSED') DEFAULT 'PENDING' COMMENT '处理状态',
    admin_id BIGINT COMMENT '处理管理员ID',
    resolution TEXT COMMENT '处理结果',
    resolution_time DATETIME COMMENT '处理时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
);
```

## 🔧 技术实现

### 后端实现

#### 1. 实体类
- `Complaint.java` - 投诉实体类
- `CreateComplaintRequest.java` - 创建投诉请求DTO
- `ProcessComplaintRequest.java` - 处理投诉请求DTO

#### 2. 数据访问层
- `ComplaintMapper.java` - 投诉数据访问接口
- `ComplaintMapper.xml` - MyBatis映射文件

#### 3. 控制器
- `ComplaintController.java` - 投诉相关API接口

### 前端实现

#### 1. 乘客端页面
- `PassengerComplaints.vue` - 乘客投诉管理页面
- 功能：提交投诉、查看投诉列表、查看投诉详情

#### 2. 管理员端页面
- `ComplaintManagement.vue` - 投诉管理页面
- 功能：投诉列表、筛选、处理、统计

## 📡 API接口

### 乘客端接口

#### 创建投诉
```http
POST /api/complaints/create?complainantId={userId}
Content-Type: application/json

{
  "orderId": 1,
  "defendantId": 2,
  "complaintType": "SERVICE",
  "title": "服务质量问题",
  "description": "详细描述投诉内容",
  "evidenceFiles": ["file1.jpg", "file2.jpg"]
}
```

#### 获取用户投诉列表
```http
GET /api/complaints/user/{userId}?page=1&size=20
```

#### 获取投诉详情
```http
GET /api/complaints/{complaintId}
```

### 管理员端接口

#### 获取投诉列表
```http
GET /api/complaints/admin/list?page=1&size=20&status=PENDING&complaintType=SERVICE
```

#### 处理投诉
```http
POST /api/complaints/admin/process/{complaintId}?adminId={adminId}
Content-Type: application/json

{
  "status": "RESOLVED",
  "resolution": "处理结果说明",
  "refundAmount": 50.00
}
```

#### 获取统计数据
```http
GET /api/complaints/admin/stats
```

## 🎯 使用流程

### 乘客投诉流程
1. 乘客登录系统
2. 进入"我的投诉"页面
3. 点击"提交投诉"按钮
4. 选择要投诉的订单
5. 填写投诉信息（类型、标题、描述）
6. 上传证据文件（可选）
7. 提交投诉
8. 查看投诉处理状态

### 管理员处理流程
1. 管理员登录系统
2. 进入"投诉管理"页面
3. 查看投诉列表和统计数据
4. 筛选需要处理的投诉
5. 点击"处理"按钮
6. 更新投诉状态
7. 填写处理结果
8. 设置退款金额（如需要）
9. 确认处理

## 📱 页面功能

### 乘客投诉页面 (PassengerComplaints.vue)
- **投诉列表**：显示用户的所有投诉记录
- **状态标签**：不同颜色标识投诉状态
- **投诉详情**：点击查看完整投诉信息
- **提交投诉**：弹窗表单提交新投诉
- **文件上传**：支持图片证据上传
- **响应式设计**：适配移动端

### 投诉管理页面 (ComplaintManagement.vue)
- **统计卡片**：显示投诉数量统计
- **投诉表格**：列表显示所有投诉
- **筛选功能**：按状态和类型筛选
- **处理对话框**：更新投诉状态和结果
- **退款功能**：支持设置退款金额
- **分页功能**：支持大量数据分页

## 🧪 测试

### 测试页面
- `test-complaint-system.html` - 完整的功能测试页面

### 测试用例
1. **API接口测试**
   - 创建投诉接口
   - 获取投诉列表接口
   - 处理投诉接口
   - 统计数据接口

2. **乘客端功能测试**
   - 投诉提交功能
   - 投诉列表查看
   - 投诉详情查看

3. **管理员端功能测试**
   - 投诉列表管理
   - 投诉筛选功能
   - 投诉处理功能
   - 统计数据展示

4. **完整流程测试**
   - 从投诉提交到处理完成的完整流程

## 🚀 部署说明

### 数据库准备
1. 确保 `complaints` 表已创建
2. 检查外键约束是否正确设置
3. 验证索引是否创建

### 后端部署
1. 确保所有Java类已编译
2. 检查MyBatis映射文件路径
3. 验证API接口可访问

### 前端部署
1. 确保Vue组件已正确导入
2. 检查路由配置
3. 验证页面可正常访问

## 📈 扩展功能

### 已实现功能
- ✅ 投诉提交和管理
- ✅ 多种投诉类型支持
- ✅ 证据文件上传
- ✅ 投诉状态管理
- ✅ 管理员处理功能
- ✅ 退款处理支持
- ✅ 统计数据展示

### 可扩展功能
- 📧 邮件通知功能
- 📱 短信通知功能
- 🔔 实时消息推送
- 📊 更详细的统计报表
- 🤖 自动化处理规则
- 📝 投诉模板功能
- 🔍 全文搜索功能
- 📈 投诉趋势分析

## 🐛 注意事项

1. **权限控制**：确保只有相关用户可以查看和处理投诉
2. **数据验证**：前后端都需要进行数据验证
3. **文件上传**：需要配置文件上传路径和大小限制
4. **性能优化**：大量投诉数据时需要考虑分页和索引优化
5. **安全考虑**：防止恶意投诉和数据泄露

## 📝 更新日志

### v1.0.0 (2025-08-13)
- ✅ 实现投诉数据库表结构
- ✅ 创建后端API接口
- ✅ 实现乘客端投诉功能
- ✅ 实现管理员端投诉管理
- ✅ 添加投诉统计功能
- ✅ 支持退款处理
- ✅ 创建测试页面和文档
- ✅ 实现响应式设计