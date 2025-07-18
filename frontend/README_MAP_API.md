# 高德地图API集成说明

## API类型说明

### 1. Web端JS API
- **用途**: 地图显示、标记、路线绘制、Autocomplete自动完成等
- **特点**: 在浏览器中直接运行，需要加载JavaScript库
- **密钥类型**: Web端JS API密钥
- **申请地址**: https://lbs.amap.com/api/javascript-api/guide/abc/prepare
- **服务类型**: 选择"Web端(JS API)"

### 2. REST API
- **用途**: 搜索POI、路径规划、地址解析等
- **特点**: 通过HTTP请求调用，返回JSON数据
- **密钥类型**: REST API密钥
- **申请地址**: https://lbs.amap.com/api/webservice/guide/create-project/get-key
- **服务类型**: 选择"Web服务"

## 配置步骤

### 第一步：申请密钥

1. **申请Web端JS API密钥**（用于地图显示和Autocomplete）
   - 访问：https://lbs.amap.com/api/javascript-api/guide/abc/prepare
   - 创建应用，选择"Web端(JS API)"
   - 绑定域名：添加您的网站域名（如：localhost、127.0.0.1等）
   - 获取密钥

2. **申请REST API密钥**（用于搜索和路径规划）
   - 访问：https://lbs.amap.com/api/webservice/guide/create-project/get-key
   - 创建应用，选择"Web服务"
   - 绑定域名：添加您的网站域名
   - 获取密钥

3. **申请安全密钥**（用于Web端JS API安全验证）
   - 在Web端JS API应用中，找到"安全密钥"选项
   - 生成安全密钥

### 第二步：配置密钥

编辑 `frontend/src/config/map.js` 文件：

```javascript
export const mapConfig = {
  // Web端JS API密钥 - 用于地图显示、Autocomplete等
  apiKey: '您的Web端JS API密钥',
  
  // REST API密钥 - 用于搜索、路径规划等
  restApiKey: '您的REST API密钥',
  
  // 安全密钥 - 用于Web端JS API安全验证
  securityJsCode: '您的安全密钥',
  
  // 其他配置...
}
```

### 第三步：验证配置

1. **检查Web端JS API密钥**：
   - 打开浏览器开发者工具
   - 查看Console是否有"Autocomplete实例创建成功"的日志
   - 如果没有，检查密钥是否正确申请了Web端JS API服务

2. **检查REST API密钥**：
   - 测试搜索功能是否正常
   - 检查Network面板中的API请求是否返回正确数据

## 常见问题

### 1. Autocomplete不工作
**可能原因**：
- 使用了REST API密钥而不是Web端JS API密钥
- 域名未正确绑定
- 安全密钥配置错误

**解决方案**：
- 确保使用Web端JS API密钥
- 检查域名绑定设置
- 验证安全密钥配置

### 2. 搜索功能不工作
**可能原因**：
- 使用了Web端JS API密钥而不是REST API密钥
- 域名未正确绑定

**解决方案**：
- 确保使用REST API密钥
- 检查域名绑定设置

### 3. 密钥无效错误
**错误信息**: `USERKEY_PLAT_NOMATCH`
**原因**: API密钥与平台不匹配
**解决方案**: 
- Web端JS API功能必须使用Web端JS API密钥
- REST API功能必须使用REST API密钥

## 功能说明

### Autocomplete自动完成
- 使用Web端JS API的AMap.Autocomplete插件
- 支持全国范围搜索
- 自动显示搜索建议
- 点击选择后自动填充地址

### 搜索功能
- 使用REST API进行POI搜索
- 支持多种搜索策略
- 包含备用数据方案

### 路径规划
- 使用REST API进行路径规划
- 显示距离和时间
- 在地图上绘制路径

## 测试方法

1. **启动前端服务**：
   ```bash
   cd frontend
   npm run dev
   ```

2. **测试地图显示**：
   - 访问乘客地图页面
   - 检查地图是否正常显示

3. **测试Autocomplete**：
   - 在目的地输入框中输入文字
   - 检查是否出现自动完成建议

4. **测试搜索功能**：
   - 输入搜索关键词
   - 检查是否显示搜索结果

5. **测试路径规划**：
   - 选择目的地
   - 检查是否显示路径和价格 