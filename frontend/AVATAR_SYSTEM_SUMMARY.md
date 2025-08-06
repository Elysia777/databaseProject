# 🖼️ 头像系统说明与修复总结

## 📊 头像数据保存机制

### 1. 数据库存储结构
```sql
-- users表中存储头像URL
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    phone VARCHAR(20),
    email VARCHAR(100),
    real_name VARCHAR(50),
    id_card VARCHAR(18),
    avatar VARCHAR(255) COMMENT '头像URL',  -- 存储相对路径
    user_type ENUM('ADMIN', 'DRIVER', 'PASSENGER') NOT NULL,
    status ENUM('ACTIVE', 'INACTIVE', 'BANNED') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### 2. 文件系统存储
- **存储目录**: `backend/uploads/avatars/`
- **文件命名**: `UUID-原文件名.扩展名`
- **访问路径**: `/uploads/avatars/filename.jpg`

### 3. 头像上传流程
1. **前端上传** → `FileUploadController.uploadAvatar()`
2. **文件验证** → 类型、大小检查
3. **文件保存** → 保存到 `uploads/avatars/` 目录
4. **生成URL** → 相对路径 `/uploads/avatars/filename.jpg`
5. **更新数据库** → `userService.updateUserAvatar(userId, avatarUrl)`
6. **返回结果** → 前端获得头像URL

### 4. 静态资源配置
```java
// WebConfig.java
@Override
public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:" + uploadPath);
}
```

## 🔧 司机头像显示问题修复

### 问题描述
- **现象**: 乘客端看不到司机头像，显示默认头像
- **原因**: 头像URL是相对路径，前端没有加上服务器前缀

### 问题分析
1. **数据库存储**: `/uploads/avatars/filename.jpg` (相对路径)
2. **前端显示**: 直接使用相对路径 → 404错误
3. **正确访问**: 需要完整URL `http://localhost:8080/uploads/avatars/filename.jpg`

### 修复方案

#### 1. PassengerMap.vue 修复
```javascript
// 添加构建完整头像URL的方法
const buildAvatarUrl = (avatarPath) => {
  if (!avatarPath) return null;
  if (avatarPath.startsWith('http')) return avatarPath;
  return `http://localhost:8080${avatarPath}`;
};

// 修改头像显示逻辑
// 修复前
:src="driverInfo.avatar || getDefaultAvatar(driverInfo.name)"

// 修复后
:src="buildAvatarUrl(driverInfo.avatar) || getDefaultAvatar(driverInfo.name)"
```

#### 2. ReviewDialog.vue 修复
```javascript
// 同样添加buildAvatarUrl方法和修改显示逻辑
const buildAvatarUrl = (avatarPath) => {
  if (!avatarPath) return null;
  if (avatarPath.startsWith('http')) return avatarPath;
  return `http://localhost:8080${avatarPath}`;
};
```

## 🔄 司机信息传递流程

### 1. 订单分配时的司机信息传递
```java
// OrderDispatchService.java
User driverUser = userMapper.selectById(driver.getUserId());
webSocketNotificationService.notifyPassengerOrderAssigned(order.getPassengerId(), order, driver, driverUser);

// WebSocketNotificationService.java
if (driverUser != null) {
    driverInfo.put("name", driverUser.getRealName());
    driverInfo.put("phone", driverUser.getPhone());
    driverInfo.put("avatar", driverUser.getAvatar()); // 相对路径
}
```

### 2. 前端接收和显示
```javascript
// PassengerMap.vue
const updateDriverInfo = (data) => {
  const driverData = {
    id: data.driver.id,
    name: data.driver.name,
    phone: data.driver.phone,
    avatar: data.driver.avatar, // 接收到相对路径
    // ... 其他信息
  };
  orderStore.setDriverInfo(driverData);
};
```

## 🧪 测试验证

### 测试文件
- `test-driver-avatar-debug.html` - 头像调试工具
- `test-avatar-access.html` - 头像访问测试
- `test-driver-avatar-fix.html` - 修复效果验证

### 测试步骤
1. 运行后端服务 (端口8080)
2. 打开测试页面
3. 点击"测试所有司机头像"
4. 验证头像是否正常显示

### 预期结果
- ✅ 有头像的司机显示真实头像
- ✅ 无头像的司机显示默认头像
- ✅ 头像URL构建正确
- ✅ 网络请求返回200状态

## 📝 相关文件清单

### 后端文件
- `FileUploadController.java` - 头像上传处理
- `UserServiceImpl.java` - 用户头像更新
- `WebConfig.java` - 静态资源配置
- `WebSocketNotificationService.java` - 司机信息推送

### 前端文件
- `PassengerMap.vue` - 乘客地图页面（主要修复）
- `ReviewDialog.vue` - 评价对话框（辅助修复）
- `AvatarUpload.vue` - 头像上传组件

### 数据库
- `users` 表的 `avatar` 字段

## 🎯 修复效果
- ✅ 乘客端可以正常看到司机头像
- ✅ 头像加载失败时显示默认头像
- ✅ 支持相对路径和绝对路径的头像URL
- ✅ 保持向后兼容性

## 🔮 后续优化建议
1. **CDN支持**: 支持CDN头像URL
2. **缓存优化**: 添加头像缓存机制
3. **压缩处理**: 上传时自动压缩图片
4. **多尺寸支持**: 生成不同尺寸的头像
5. **默认头像优化**: 使用更美观的默认头像生成算法