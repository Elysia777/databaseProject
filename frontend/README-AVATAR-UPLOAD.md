# 头像上传功能测试指南

## 🎯 功能概述

头像上传功能已完全实现，包括：
- 用户头像上传（支持JPG、PNG、GIF，最大5MB）
- 实时上传进度显示
- 头像预览和删除
- 默认头像生成
- 完整的错误处理

## 🚀 快速开始

### 1. 启动后端服务
确保Spring Boot应用已在IDEA中启动，运行在 `http://localhost:8080`

### 2. 启动测试服务器
为了避免CORS问题，需要通过HTTP服务器访问测试页面：

#### 方法一：使用Node.js（推荐）
```bash
cd frontend
node start-test-server.js
```

#### 方法二：使用Python
```bash
cd frontend
python start-test-server.py
```

#### 方法三：直接访问（如果已有HTTP服务器）
将 `test-avatar-upload-server.html` 放在你的HTTP服务器目录中访问

### 3. 测试功能
浏览器会自动打开测试页面，或手动访问：
- http://localhost:3000/test-avatar-upload-server.html

## 📋 测试步骤

1. **连接检测**：页面会自动检测后端服务连接状态
2. **选择头像**：点击头像区域或"选择头像"按钮
3. **上传文件**：选择图片文件（JPG/PNG/GIF，最大5MB）
4. **查看进度**：观察上传进度条
5. **预览头像**：上传成功后查看头像预览
6. **删除头像**：点击"删除头像"按钮测试删除功能

## 🔧 技术实现

### 后端API
- **上传接口**：`POST /api/upload/avatar`
- **删除接口**：`DELETE /api/upload/avatar/{userId}`
- **健康检查**：`GET /api/auth/health`

### 文件存储
- **存储路径**：`uploads/avatars/`
- **访问URL**：`http://localhost:8080/uploads/avatars/filename.jpg`
- **文件命名**：UUID + 原始扩展名

### 前端组件
- **Vue组件**：`src/components/AvatarUpload.vue`
- **测试页面**：`test-avatar-upload-server.html`

## 🛠️ 配置说明

### application.yml配置
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 5MB

file:
  upload:
    path: uploads/
```

### 数据库字段
```sql
-- users表中的avatar字段
avatar VARCHAR(255) COMMENT '头像URL'
```

## 🧪 测试场景

### 正常流程
1. ✅ 选择有效图片文件
2. ✅ 上传进度显示
3. ✅ 头像预览更新
4. ✅ 删除头像功能

### 异常处理
1. ❌ 文件类型不支持
2. ❌ 文件大小超限
3. ❌ 网络连接失败
4. ❌ 后端服务未启动

## 🔍 故障排除

### 连接失败
- 确保后端服务已启动
- 检查端口8080是否被占用
- 验证防火墙设置

### 上传失败
- 检查文件格式和大小
- 确认uploads目录权限
- 查看后端日志错误信息

### CORS错误
- 使用提供的测试服务器
- 不要直接打开HTML文件（file://协议）

## 📱 在Vue应用中使用

```vue
<template>
  <AvatarUpload 
    :userId="user.id" 
    :userName="user.name"
    size="large"
    @avatar-updated="handleAvatarUpdate"
  />
</template>

<script>
import AvatarUpload from '@/components/AvatarUpload.vue'

export default {
  components: { AvatarUpload },
  methods: {
    handleAvatarUpdate(avatarUrl) {
      console.log('头像已更新:', avatarUrl)
      // 更新用户信息
      this.user.avatar = avatarUrl
    }
  }
}
</script>
```

## 🎉 功能特性

- ✅ **多格式支持**：JPG、PNG、GIF
- ✅ **大小限制**：最大5MB
- ✅ **进度显示**：实时上传进度
- ✅ **预览功能**：即时头像预览
- ✅ **默认头像**：基于用户名生成
- ✅ **删除功能**：支持删除已上传头像
- ✅ **错误处理**：完整的异常处理机制
- ✅ **响应式设计**：适配不同屏幕尺寸
- ✅ **Vue组件**：可复用的组件化实现

现在头像上传功能已经完全实现并可以正常使用了！