# 管理员端系统设计文档

## 概述

管理员端系统是一个基于Web的管理界面，采用前后端分离架构。前端使用Vue 3 + Element Plus构建响应式管理界面，后端基于现有的Spring Boot架构扩展管理员专用的API接口。

## 架构设计

### 系统架构图

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   管理员前端     │    │   后端API服务    │    │   数据库层       │
│  (Vue 3 + EP)   │◄──►│  (Spring Boot)  │◄──►│   (MySQL)       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   路由守卫       │    │   权限验证       │    │   Redis缓存     │
│   权限控制       │    │   JWT Token     │    │   会话管理       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### 技术栈

**前端技术栈：**
- Vue 3 (Composition API)
- Element Plus (UI组件库)
- Vue Router (路由管理)
- Pinia (状态管理)
- Axios (HTTP客户端)
- ECharts (数据可视化)

**后端技术栈：**
- Spring Boot (现有架构)
- Spring Security (权限控制)
- JWT (身份验证)
- MyBatis (数据访问)
- Redis (缓存和会话)

## 组件设计

### 前端组件结构

```
src/
├── admin/                    # 管理员端专用目录
│   ├── views/               # 页面组件
│   │   ├── Login.vue        # 管理员登录页
│   │   ├── Dashboard.vue    # 仪表板
│   │   ├── UserManagement.vue    # 用户管理
│   │   ├── OrderManagement.vue   # 订单管理
│   │   ├── ReviewManagement.vue  # 评价管理
│   │   ├── Statistics.vue        # 数据统计
│   │   ├── SystemMonitor.vue     # 系统监控
│   │   └── Settings.vue          # 系统设置
│   ├── components/          # 公共组件
│   │   ├── AdminLayout.vue  # 管理员布局
│   │   ├── AdminHeader.vue  # 顶部导航
│   │   ├── AdminSidebar.vue # 侧边菜单
│   │   ├── DataTable.vue    # 数据表格
│   │   ├── StatCard.vue     # 统计卡片
│   │   └── ChartContainer.vue # 图表容器
│   ├── stores/              # 状态管理
│   │   ├── admin.js         # 管理员状态
│   │   ├── users.js         # 用户管理状态
│   │   └── statistics.js    # 统计数据状态
│   └── router/              # 路由配置
│       └── admin.js         # 管理员路由
```

### 后端API设计

#### 管理员认证API

```java
@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {
    
    @PostMapping("/login")
    public Result<AdminLoginResponse> login(@RequestBody AdminLoginRequest request);
    
    @PostMapping("/logout")
    public Result<Void> logout();
    
    @GetMapping("/profile")
    public Result<AdminProfile> getProfile();
}
```

#### 用户管理API

```java
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    
    @GetMapping
    public Result<PageResult<UserInfo>> getUsers(@RequestParam Map<String, Object> params);
    
    @GetMapping("/{userId}")
    public Result<UserDetail> getUserDetail(@PathVariable Long userId);
    
    @PutMapping("/{userId}/status")
    public Result<Void> updateUserStatus(@PathVariable Long userId, @RequestBody StatusUpdateRequest request);
    
    @GetMapping("/statistics")
    public Result<UserStatistics> getUserStatistics();
}
```

#### 订单管理API

```java
@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {
    
    @GetMapping
    public Result<PageResult<OrderInfo>> getOrders(@RequestParam Map<String, Object> params);
    
    @GetMapping("/{orderId}")
    public Result<OrderDetail> getOrderDetail(@PathVariable Long orderId);
    
    @PutMapping("/{orderId}")
    public Result<Void> updateOrder(@PathVariable Long orderId, @RequestBody OrderUpdateRequest request);
    
    @GetMapping("/statistics")
    public Result<OrderStatistics> getOrderStatistics();
    
    @GetMapping("/export")
    public void exportOrders(@RequestParam Map<String, Object> params, HttpServletResponse response);
}
```

#### 评价管理API

```java
@RestController
@RequestMapping("/api/admin/reviews")
public class AdminReviewController {
    
    @GetMapping
    public Result<PageResult<ReviewInfo>> getReviews(@RequestParam Map<String, Object> params);
    
    @GetMapping("/{reviewId}")
    public Result<ReviewDetail> getReviewDetail(@PathVariable Long reviewId);
    
    @PutMapping("/{reviewId}/status")
    public Result<Void> updateReviewStatus(@PathVariable Long reviewId, @RequestBody ReviewStatusRequest request);
    
    @GetMapping("/statistics")
    public Result<ReviewStatistics> getReviewStatistics();
}
```

## 数据模型

### 管理员用户表

```sql
CREATE TABLE admin_users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '管理员用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码(加密)',
    real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    role ENUM('SUPER_ADMIN', 'ADMIN', 'OPERATOR') NOT NULL DEFAULT 'ADMIN' COMMENT '角色',
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
    last_login_at TIMESTAMP COMMENT '最后登录时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### 管理员操作日志表

```sql
CREATE TABLE admin_operation_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    admin_id BIGINT NOT NULL COMMENT '管理员ID',
    operation_type VARCHAR(50) NOT NULL COMMENT '操作类型',
    operation_desc TEXT COMMENT '操作描述',
    target_type VARCHAR(50) COMMENT '操作对象类型',
    target_id BIGINT COMMENT '操作对象ID',
    ip_address VARCHAR(45) COMMENT 'IP地址',
    user_agent TEXT COMMENT '用户代理',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_admin_id (admin_id),
    INDEX idx_operation_type (operation_type),
    INDEX idx_created_at (created_at)
);
```

## 界面设计

### 布局设计

管理员端采用经典的后台管理布局：

```
┌─────────────────────────────────────────────────────────┐
│                    顶部导航栏                            │
│  Logo  |  面包屑导航  |        |  用户信息  |  退出     │
├─────────────────────────────────────────────────────────┤
│        │                                                │
│  侧边   │                                                │
│  菜单   │              主内容区域                         │
│        │                                                │
│        │                                                │
│        │                                                │
└─────────────────────────────────────────────────────────┘
```

### 主要页面设计

#### 1. 仪表板页面
- 核心指标卡片（今日订单、在线司机、总收入等）
- 实时数据图表（订单趋势、用户增长等）
- 快捷操作入口
- 系统状态监控

#### 2. 用户管理页面
- 用户列表表格（支持分页、搜索、筛选）
- 用户详情弹窗
- 批量操作功能
- 用户状态管理

#### 3. 订单管理页面
- 订单列表表格（多条件筛选）
- 订单详情查看
- 订单状态修改
- 数据导出功能

#### 4. 评价管理页面
- 评价列表展示
- 评价内容审核
- 差评处理工具
- 评价统计分析

## 安全设计

### 身份验证
- 管理员专用登录接口
- JWT Token机制
- Token自动刷新
- 登录失败限制

### 权限控制
- 基于角色的权限控制(RBAC)
- 接口级权限验证
- 前端路由守卫
- 操作日志记录

### 数据安全
- 敏感数据脱敏显示
- 数据访问审计
- 操作确认机制
- 数据备份策略

## 错误处理

### 前端错误处理
- 全局错误拦截
- 用户友好的错误提示
- 网络异常处理
- 权限异常处理

### 后端错误处理
- 统一异常处理
- 详细的错误日志
- 错误码标准化
- 异常监控告警

## 测试策略

### 单元测试
- 核心业务逻辑测试
- API接口测试
- 权限验证测试
- 数据处理测试

### 集成测试
- 前后端集成测试
- 数据库集成测试
- 第三方服务集成测试

### 用户验收测试
- 管理员操作流程测试
- 权限控制验证
- 性能压力测试
- 安全渗透测试

## 部署方案

### 开发环境
- 前端开发服务器(Vite)
- 后端开发服务器(Spring Boot)
- 本地数据库和Redis

### 生产环境
- 前端静态资源部署(Nginx)
- 后端服务部署(Docker)
- 数据库集群
- Redis集群
- 负载均衡和反向代理