# 🔧 经纬度字段名和数值修复指南

## 问题分析

你的输入数据存在两个问题：
1. **字段名和值不匹配**：latitude字段存储了经度值，longitude字段存储了纬度值
2. **前端可能传参错误**：在调用API时就已经把参数搞反了

## 当前错误状态

### 你的输入
```json
{
  "driverId": 1,
  "latitude": 116.4540,   // ❌ 字段名是latitude，但值是经度
  "longitude": 39.9340    // ❌ 字段名是longitude，但值是纬度
}
```

### 数据库存储结果
```
currentLatitude = 116.4540   // ❌ 纬度字段存储了经度值
currentLongitude = 39.9340   // ❌ 经度字段存储了纬度值
```

## 🎯 正确的应该是

### 正确输入
```json
{
  "driverId": 1,
  "latitude": 39.9340,    // ✅ 纬度字段存储纬度值
  "longitude": 116.4540   // ✅ 经度字段存储经度值
}
```

### 正确的数据库存储
```
currentLatitude = 39.9340    // ✅ 纬度字段存储纬度值
currentLongitude = 116.4540  // ✅ 经度字段存储经度值
```

## 🔍 检查清单

### 1. 检查前端代码
查看你的前端JavaScript代码，确保：
```javascript
// ❌ 错误的调用方式
const wrongData = {
    latitude: longitude,   // 把经度赋值给了latitude
    longitude: latitude    // 把纬度赋值给了longitude
};

// ✅ 正确的调用方式
const correctData = {
    latitude: latitude,    // 纬度赋值给latitude
    longitude: longitude   // 经度赋值给longitude
};
```

### 2. 检查API调用
确保Apifox或其他API工具中的参数正确：
```
❌ 错误：
POST /api/drivers/1/online?latitude=116.4540&longitude=39.9340

✅ 正确：
POST /api/drivers/1/online?latitude=39.9340&longitude=116.4540
```

### 3. 检查地图API获取坐标的代码
如果你使用地图API获取坐标，确保：
```javascript
// 不同地图API返回格式可能不同
// 百度地图
const baiduResult = {
    lat: 39.9340,  // 纬度
    lng: 116.4540  // 经度
};

// 高德地图
const gaodeResult = {
    latitude: 39.9340,   // 纬度
    longitude: 116.4540  // 经度
};

// 确保正确映射到你的API参数
const apiParams = {
    latitude: baiduResult.lat,    // 或 gaodeResult.latitude
    longitude: baiduResult.lng    // 或 gaodeResult.longitude
};
```

## 🛠️ 修复步骤

### 步骤1: 清理错误数据
```sql
-- 查看当前错误数据
SELECT id, current_latitude, current_longitude 
FROM drivers 
WHERE current_latitude > 100;  -- 纬度不应该超过100

-- 如果确认数据错误，交换经纬度
UPDATE drivers 
SET 
    current_latitude = current_longitude,
    current_longitude = current_latitude,
    updated_at = NOW()
WHERE current_latitude > 100 AND current_longitude < 100;
```

### 步骤2: 修复前端代码
检查并修复你的前端代码中的坐标获取和传递逻辑。

### 步骤3: 重新测试
使用正确的坐标重新测试所有功能。

## 📍 北京地区测试坐标

```json
{
  "天安门广场": { "latitude": 39.9042, "longitude": 116.4074 },
  "西单商业区": { "latitude": 39.9163, "longitude": 116.3972 },
  "鸟巢体育场": { "latitude": 39.9928, "longitude": 116.3906 },
  "颐和园": { "latitude": 39.9999, "longitude": 116.2750 },
  "北京大学": { "latitude": 39.9990, "longitude": 116.3059 }
}
```

## 🧭 记忆技巧

- **Latitude** = **纬度** = **南北** = 北京约 **39.xx**
- **Longitude** = **经度** = **东西** = 北京约 **116.xx**
- **纬度范围**: 39.4 - 41.0 (北京地区)
- **经度范围**: 115.4 - 117.5 (北京地区)

## ⚠️ 常见错误来源

1. **地图API返回格式混淆**
2. **前端变量名使用错误**
3. **复制粘贴时顺序搞反**
4. **不同坐标系转换错误**
5. **测试数据随意填写**

记住：**纬度小经度大**（北京地区：纬度39.xx < 经度116.xx）