# 投诉功能菜单配置更新

## 📋 更新内容

已成功为投诉系统添加了菜单项和路由配置，现在用户可以通过侧边栏菜单访问投诉功能。

## ✅ 已完成的更新

### 1. 路由配置 (frontend/src/router/index.js)
```javascript
// 管理员投诉管理路由
{
  path: "complaint-management",
  name: "ComplaintManagement",
  component: () => import("@/views/ComplaintManagement.vue"),
}

// 乘客投诉路由
{
  path: "my-complaints", 
  name: "PassengerComplaints",
  component: () => import("@/views/PassengerComplaints.vue"),
}
```

### 2. 菜单配置 (frontend/src/views/Dashboard.vue)

#### 管理员菜单
```html
<el-menu-item index="/dashboard/complaint-management">
  <el-icon><Warning /></el-icon>
  <span>投诉管理</span>
</el-menu-item>
```

#### 乘客菜单
```html
<el-menu-item index="/dashboard/my-complaints">
  <el-icon><Warning /></el-icon>
  <span>我的投诉</span>
</el-menu-item>
```

### 3. 页面标题映射
```javascript
// 管理员页面标题
'/dashboard/complaint-management': '投诉管理'

// 乘客页面标题  
'/dashboard/my-complaints': '我的投诉'
```

## 🎯 菜单显示逻辑

### 管理员用户
- ✅ 管理概览
- ✅ 用户管理
- ✅ 订单管理
- ✅ 评价管理
- ✅ 车辆管理
- ✅ **投诉管理** (新增)
- ✅ 个人设置

### 乘客用户
- ✅ 叫车
- ✅ 我的行程
- ✅ **我的投诉** (新增)
- ✅ 个人设置

### 司机用户
- ✅ 接单地图
- ✅ 我的订单
- ✅ 收入统计
- ✅ 我的车辆
- ✅ 个人设置

## 🔗 访问路径

### 管理员投诉管理
- **URL**: `/dashboard/complaint-management`
- **组件**: `ComplaintManagement.vue`
- **权限**: 仅管理员可访问

### 乘客投诉功能
- **URL**: `/dashboard/my-complaints`
- **组件**: `PassengerComplaints.vue`
- **权限**: 仅乘客可访问

## 🧪 测试方法

### 1. 管理员测试
1. 以管理员身份登录系统
2. 查看左侧菜单是否显示"投诉管理"
3. 点击"投诉管理"菜单项
4. 验证是否正确跳转到投诉管理页面
5. 测试投诉管理功能

### 2. 乘客测试
1. 以乘客身份登录系统
2. 查看左侧菜单是否显示"我的投诉"
3. 点击"我的投诉"菜单项
4. 验证是否正确跳转到投诉页面
5. 测试投诉提交和查看功能

### 3. 司机测试
1. 以司机身份登录系统
2. 确认菜单中没有投诉相关选项（司机暂不需要投诉功能）

## 📁 测试文件

- `frontend/test-complaint-menu.html` - 菜单配置测试页面
- `frontend/test-complaint-system.html` - 完整功能测试页面

## ⚠️ 注意事项

1. **权限控制**: 菜单项会根据用户类型自动显示/隐藏
2. **图标显示**: 使用了Element Plus的Warning图标
3. **路由守卫**: 已有的路由守卫会自动处理权限验证
4. **页面标题**: 面包屑导航会自动显示正确的页面标题

## 🔧 故障排除

### 菜单不显示
1. 检查用户是否已正确登录
2. 验证用户类型是否正确（管理员/乘客）
3. 确认Dashboard.vue文件更新是否生效

### 页面无法访问
1. 检查路由配置是否正确
2. 验证组件文件是否存在
3. 查看浏览器控制台是否有错误信息

### 图标不显示
1. 确认Element Plus图标库已正确加载
2. 检查Warning图标是否可用
3. 可以尝试使用其他图标替代

## 🚀 下一步

投诉功能的菜单和路由配置已完成，用户现在可以通过以下方式访问：

1. **管理员**: 侧边栏 → 投诉管理
2. **乘客**: 侧边栏 → 我的投诉

建议进行完整的功能测试，确保投诉系统的所有功能都能正常工作。