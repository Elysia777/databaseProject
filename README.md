# 网约车运营系统

## 项目简介
这是一个基于Vue.js + Spring Boot的网约车运营管理系统，提供完整的网约车服务解决方案。

## 功能特性
- 🚗 驾驶员注册、车辆注册、客户注册
- 📱 实时单智能派单
- 📅 预约单智能派单
- ⭐ 评价管理系统
- 📊 查询统计(销售数据等)
- 🔐 用户权限管理
- ⚠️ 异常处理(纠纷处理等)

## 技术栈
### 前端
- Vue.js 3
- Element Plus
- Vue Router
- Vuex/Pinia
- Axios
- ECharts

### 后端
- Spring Boot 2.7+
- Spring Security
- MyBatis Plus
- MySQL 8.0
- Redis
- WebSocket

## 项目结构
```
datebaseproject/
├── frontend/          # Vue前端项目
├── backend/           # Spring Boot后端项目
├── database/          # 数据库脚本
└── docs/             # 项目文档
```

## 快速开始

### 后端启动
```bash
cd backend
mvn spring-boot:run
```

### 前端启动
```bash
cd frontend
npm install
npm run dev
```

## 数据库设计
详见 `database/` 目录下的SQL脚本文件。 