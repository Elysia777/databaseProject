# 网约车运营系统 - 高德地图版

基于高德地图API的完整网约车运营系统，包含乘客端、司机端和管理后台。

## 🌟 系统特性

### 乘客端功能
- ✅ **地图选点下单**：拖拽地图选择起点，搜索目的地
- ✅ **实时定位**：自动获取当前位置，精准定位
- ✅ **路线规划**：显示起点到终点的最优路线和预估费用
- ✅ **实时追踪**：查看司机位置，预估到达时间
- ✅ **订单管理**：查看订单状态，支持取消订单
- ✅ **在线支付**：订单完成后的支付流程

### 司机端功能
- ✅ **在线接单**：实时接收订单推送，支持接单/拒单
- ✅ **导航服务**：到上车点和目的地的导航路线
- ✅ **位置上报**：实时上报位置给乘客
- ✅ **订单管理**：确认到达、开始行程、完成订单
- ✅ **状态管理**：上线/下线状态控制

### 技术特性
- 🗺️ **高德地图API**：精准的地图服务和路线规划
- 🔄 **实时通信**：WebSocket实现实时消息推送
- 🔒 **分布式锁**：Redis防止订单重复接单
- 📨 **消息队列**：RabbitMQ确保消息可靠传递
- 📱 **响应式设计**：支持移动端和桌面端
- 🚀 **高性能**：Redis缓存提升系统性能

## 🚀 快速开始

### 1. 环境准备

确保以下服务已启动：

```bash
# Redis (端口 6379)
redis-server

# RabbitMQ (端口 5672, 管理界面 15672)
rabbitmq-server

# MySQL (端口 3306)
mysql -u root -p
```

### 2. 后端启动

```bash
cd backend
mvn spring-boot:run
```

后端服务将在 http://localhost:8080 启动

### 3. 前端启动

#### Vue项目（推荐）
```bash
# 使用启动脚本
start-vue-frontend.bat

# 或手动启动
cd frontend
npm install
npm run dev
```

Vue项目将在 http://localhost:5173 启动

#### 静态HTML页面（测试用）
也可以直接打开浏览器访问：
- **测试首页**：`frontend/index.html`
- **多司机测试**：`frontend/multi-driver-order-test.html`

## 📱 使用指南

### 乘客端使用流程

1. **打开乘客端页面**
   ```
   frontend/passenger-app.html
   ```

2. **设置起点**
   - 点击"📍"按钮自动定位当前位置
   - 或者在地图上点击选择起点

3. **选择目的地**
   - 在目的地输入框中搜索地点
   - 从搜索结果中选择目的地

4. **确认下单**
   - 系统自动规划路线并显示预估费用
   - 点击"确认下单"创建订单

5. **等待接单**
   - 系统自动为您寻找附近司机
   - 司机接单后可查看司机信息

6. **实时追踪**
   - 查看司机实时位置
   - 预估司机到达时间

7. **完成行程**
   - 司机到达后上车
   - 行程结束后完成支付

### 司机端使用流程

1. **打开司机端页面**
   ```
   frontend/driver-app.html
   ```

2. **司机上线**
   - 输入司机ID
   - 点击"🟢 上线接单"
   - 系统自动获取当前位置

3. **接收订单**
   - 收到订单推送通知
   - 查看订单详情（距离、费用等）
   - 选择"✅ 接单"或"❌ 拒单"

4. **前往接客**
   - 接单成功后查看到上车点的路线
   - 点击"开始导航"进行导航
   - 实时位置会同步给乘客

5. **确认到达**
   - 到达上车点后点击"📍 确认到达"
   - 等待乘客上车

6. **开始行程**
   - 乘客上车后点击"🚀 开始行程"
   - 导航到目的地

7. **完成订单**
   - 到达目的地后点击"✅ 完成订单"
   - 订单完成，司机变为空闲状态

## 🧪 测试功能

### 多司机测试
访问 `frontend/multi-driver-order-test.html` 可以：
- 同时测试多个司机
- 验证订单互斥机制
- 测试WebSocket实时通信

### 系统测试工具
- **订单测试**：`frontend/order-test-page.html`
- **WebSocket测试**：`frontend/websocket-test.html`
- **系统综合测试**：`frontend/taxi-test.html`

## 🔧 配置说明

### 高德地图API配置

在 `backend/src/main/resources/application.yml` 中配置：

```yaml
amap:
  key: e78a59e4a55e6634055068f28955d282  # Web端API Key
  security-key: b8568193f0510e07a52f8d5f79fb0223  # 安全密钥
  base-url: https://restapi.amap.com/v3
```

### 数据库配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/taxi_operation_system
    username: root
    password: 123456
```

### Redis配置

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
```

### RabbitMQ配置

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

## 📊 系统架构

```
┌─────────────────┐    ┌─────────────────┐
│   乘客端 (Web)   │    │   司机端 (Web)   │
│  passenger-app   │    │   driver-app    │
└─────────────────┘    └─────────────────┘
         │                       │
         └───────────┬───────────┘
                     │
         ┌─────────────────┐
         │  WebSocket API  │
         │   (实时通信)     │
         └─────────────────┘
                     │
         ┌─────────────────┐
         │  Spring Boot    │
         │    后端服务      │
         └─────────────────┘
                     │
    ┌────────────────┼────────────────┐
    │                │                │
┌─────────┐    ┌─────────┐    ┌─────────┐
│  MySQL  │    │  Redis  │    │RabbitMQ │
│ (数据库) │    │ (缓存)   │    │(消息队列)│
└─────────┘    └─────────┘    └─────────┘
```

## 🔍 API接口

### 订单相关
- `POST /api/orders/create` - 创建订单
- `GET /api/orders/{id}` - 获取订单详情
- `POST /api/orders/{id}/cancel` - 取消订单
- `POST /api/orders/{id}/pickup` - 确认到达
- `POST /api/orders/{id}/start` - 开始行程
- `POST /api/orders/{id}/complete` - 完成订单

### 司机相关
- `POST /api/drivers/{id}/online` - 司机上线
- `POST /api/drivers/{id}/offline` - 司机下线
- `POST /api/drivers/{id}/accept-order/{orderId}` - 接单
- `POST /api/drivers/{id}/location` - 更新位置

## 🐛 常见问题

### 1. 地图不显示
- 检查高德地图API Key是否正确
- 确保网络连接正常
- 检查浏览器控制台是否有错误

### 2. WebSocket连接失败
- 确保后端服务已启动
- 检查防火墙设置
- 确认端口8080未被占用

### 3. 订单推送不及时
- 检查RabbitMQ服务状态
- 确认WebSocket连接正常
- 查看后端日志排查问题

### 4. 定位不准确
- 允许浏览器获取位置权限
- 使用HTTPS协议访问（生产环境）
- 检查GPS信号强度

## 📝 开发说明

### 前端技术栈
- HTML5 + CSS3 + JavaScript
- 高德地图JavaScript API
- WebSocket (SockJS + STOMP)
- 响应式设计

### 后端技术栈
- Spring Boot 3.2.0
- Spring WebSocket
- MyBatis
- Redis
- RabbitMQ
- MySQL

### 部署建议
- 使用Nginx作为反向代理
- 配置HTTPS证书
- 使用Docker容器化部署
- 配置负载均衡

## 📄 许可证

本项目仅供学习和研究使用。

## 🤝 贡献

欢迎提交Issue和Pull Request来改进这个项目。

---

**注意**：使用前请确保已获得高德地图API的使用授权，并遵守相关服务条款。