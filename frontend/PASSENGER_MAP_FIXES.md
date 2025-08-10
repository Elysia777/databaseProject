# 🗺️ 乘客端地图修复总结

## 🎯 修复的问题

### 1. 页面切换后起始点和终点图标消失
**问题描述**: 从其他页面切换回乘客地图页面时，上车点和目的地标记消失，只剩下路线。

**根本原因**: 页面激活时没有恢复地图标记，只检查了订单状态。

**修复方案**: 
- 在`initializePassengerMap`函数中添加标记恢复逻辑
- 新增`restoreMapMarkers`函数来恢复所有地图标记
- 在页面重新激活时调用标记恢复函数

### 2. 取消订单后旧路径没有清除
**问题描述**: 取消订单后，地图上仍然显示司机路径和乘客路径，没有被清理干净。

**根本原因**: `resetOrderState`函数的清理逻辑不够彻底，只清理了部分路径。

**修复方案**:
- 增强`resetOrderState`函数，添加完整的路径清理逻辑
- 清理所有红色司机路径（通过颜色识别）
- 恢复乘客路径为蓝色

### 3. 司机位置更新后旧路径没有清除
**问题描述**: 司机位置更新时，旧的司机路径没有被清除，导致地图上出现多条路径。

**根本原因**: `updateDriverLocation`函数在更新位置时没有先清理旧路径。

**修复方案**:
- 在`updateDriverLocation`函数开始时调用`clearAllDriverRoutes`
- 新增`clearAllDriverRoutes`函数来彻底清理所有司机路径
- 确保先清理再规划新路径的顺序

## 🔧 修复代码

### 1. 标记恢复函数
```javascript
// 恢复地图标记（修复页面切换后标记消失的问题）
const restoreMapMarkers = () => {
  console.log('🔄 恢复地图标记...');
  
  if (!map) {
    console.log('⚠️ 地图未初始化，跳过标记恢复');
    return;
  }
  
  // 恢复上车点标记
  if (currentPosition.value && !pickupMarker) {
    console.log('📍 恢复上车点标记');
    pickupMarker = new window.AMap.Marker({
      position: [currentPosition.value.lng, currentPosition.value.lat],
      map,
      icon: new window.AMap.Icon({
        image: 'https://webapi.amap.com/theme/v1.3/markers/n/start.png',
        size: new window.AMap.Size(25, 34),
        imageSize: new window.AMap.Size(25, 34)
      }),
      title: '上车点',
      draggable: !currentOrder.value
    });
    
    // 重新绑定拖拽事件
    pickupMarker.on("dragend", handlePickupDrag);
  }
  
  // 恢复目的地标记
  if (destinationPosition.value && !destMarker) {
    console.log('🎯 恢复目的地标记');
    destMarker = new window.AMap.Marker({
      position: [destinationPosition.value.lng, destinationPosition.value.lat],
      map,
      icon: new window.AMap.Icon({
        image: 'https://webapi.amap.com/theme/v1.3/markers/n/end.png',
        size: new window.AMap.Size(25, 34),
        imageSize: new window.AMap.Size(25, 34)
      }),
      title: '目的地'
    });
  }
  
  // 恢复司机标记（如果有当前订单）
  if (currentOrder.value && driverInfo.value && !driverMarker) {
    console.log('🚗 恢复司机标记');
    driverMarker = new window.AMap.Marker({
      position: [driverInfo.value.longitude, driverInfo.value.latitude],
      map,
      icon: new window.AMap.Icon({
        image: 'https://webapi.amap.com/theme/v1.3/markers/n/mark_b.png',
        size: new window.AMap.Size(25, 34),
        imageSize: new window.AMap.Size(25, 34)
      }),
      title: '司机位置'
    });
  }
  
  console.log('✅ 地图标记恢复完成');
};
```

### 2. 路径清理函数
```javascript
// 清理所有司机路径（修复司机位置更新后旧路径没有清除的问题）
const clearAllDriverRoutes = () => {
  console.log('🧹 清理所有司机路径...');
  
  // 清理全局司机路径变量
  if (window.driverRouteLine) {
    map.remove(window.driverRouteLine);
    window.driverRouteLine = null;
    console.log('🗑️ 清理了全局司机路径变量');
  }
  
  // 清理所有红色路径（司机路径）
  const allOverlays = map.getAllOverlays();
  let cleanedCount = 0;
  
  allOverlays.forEach(overlay => {
    if (overlay.CLASS_NAME === 'AMap.Polyline') {
      try {
        const options = overlay.getOptions();
        if (options && options.strokeColor === '#FF6B6B') {
          map.remove(overlay);
          cleanedCount++;
        }
      } catch (error) {
        // 忽略错误
      }
    }
  });
  
  console.log(`🗑️ 清理了 ${cleanedCount} 条红色司机路径`);
};
```

### 3. 增强的订单重置函数
```javascript
// 重置订单状态（增强版 - 修复取消订单后旧路径没有清除的问题）
const resetOrderState = () => {
  console.log("🔄 重置订单状态");

  orderStore.clearOrderState();
  stopDriverTracking();

  // 清理司机标记
  if (driverMarker) {
    map.remove(driverMarker);
    driverMarker = null;
    console.log('🗑️ 已清理司机标记');
  }

  // 清理所有司机路径
  clearAllDriverRoutes();

  // 恢复乘客路径为蓝色
  if (routeLine) {
    routeLine.setOptions({
      strokeColor: "#409EFF",
      strokeWeight: 6,
      strokeOpacity: 0.8,
    });
    console.log('🔄 乘客路径已恢复为蓝色');
  }

  // 重置状态
  window.routeInitialized = false;
  canOrder.value = true;
  isCalling.value = false;

  // 恢复上车点标记的可拖拽状态
  updatePickupMarkerDraggable();

  // 重新检查未支付订单
  orderStore.checkUnpaidOrders();
  
  console.log('✅ 订单状态重置完成');
};
```

### 4. 增强的司机位置更新函数
```javascript
const updateDriverLocation = (data) => {
  console.log("📍 更新司机位置:", data);

  if (data.driverId === driverInfo.value?.id) {
    // 先清理所有旧的司机路径（修复司机位置更新后旧路径没有清除的问题）
    clearAllDriverRoutes();
    
    // 通过store更新司机位置信息
    if (driverInfo.value) {
      const updatedDriver = {
        ...driverInfo.value,
        latitude: data.latitude,
        longitude: data.longitude,
      };
      orderStore.setDriverInfo(updatedDriver);
      console.log("✅ 司机位置已通过store更新");
    }

    // 更新司机标记位置
    if (driverMarker) {
      driverMarker.setPosition([data.longitude, data.latitude]);
      driverMarker.setAnimation("AMAP_ANIMATION_BOUNCE");
      setTimeout(() => {
        if (driverMarker) {
          driverMarker.setAnimation("AMAP_ANIMATION_NONE");
        }
      }, 1000);

      // 重新规划路径
      if (orderStatus.value === "IN_PROGRESS") {
        currentPosition.value = {
          lng: data.longitude,
          lat: data.latitude,
        };
        showRoute();
      } else {
        updateDriverRoute(data.latitude, data.longitude);
      }

      updateSharedMapView(false);
    } else {
      showDriverOnMap(data.latitude, data.longitude);
      updateSharedMapView(true);
    }
  }
};
```

## 🧪 测试验证

### 测试工具
- `frontend/test-passenger-map-fixes.html` - 专门的修复测试页面

### 测试场景

#### 1. 页面切换测试
1. 在乘客端创建订单，确保有起始点和终点标记
2. 切换到其他页面（如个人资料页面）
3. 切换回乘客地图页面
4. **预期结果**: 所有标记都应该正确显示

#### 2. 订单取消测试
1. 创建订单，等待司机接单
2. 确保地图上有司机路径（红色）和乘客路径（蓝色）
3. 取消订单
4. **预期结果**: 司机路径和司机标记被清除，乘客路径恢复为蓝色

#### 3. 司机位置更新测试
1. 有活跃订单时，观察司机路径
2. 司机移动位置（可以通过禁用/启用位置权限模拟）
3. **预期结果**: 旧的司机路径被清除，新的司机路径被绘制

### 调试命令
```javascript
// 检查地图状态
console.log('地图状态:', {
  mapExists: !!window.map,
  pickupMarker: !!window.pickupMarker,
  destMarker: !!window.destMarker,
  driverMarker: !!window.driverMarker,
  routeLine: !!window.routeLine,
  driverRouteLine: !!window.driverRouteLine
});

// 手动恢复标记
if (typeof window.restoreMapMarkers === 'function') {
  window.restoreMapMarkers();
}

// 手动清理司机路径
if (typeof window.clearAllDriverRoutes === 'function') {
  window.clearAllDriverRoutes();
}

// 手动重置订单状态
if (typeof window.resetOrderState === 'function') {
  window.resetOrderState();
}
```

## ✅ 修复效果

### 修复前的问题
- ❌ 页面切换后标记消失
- ❌ 取消订单后路径残留
- ❌ 司机位置更新后路径累积

### 修复后的效果
- ✅ 页面切换后所有标记正确恢复
- ✅ 取消订单后所有司机相关元素被清理
- ✅ 司机位置更新时先清理旧路径再绘制新路径
- ✅ 路径显示一致性得到保证
- ✅ 用户体验显著改善

## 🔮 后续优化建议

1. **性能优化**: 考虑使用防抖机制来减少频繁的路径清理和重绘
2. **状态管理**: 进一步完善地图状态的统一管理
3. **错误处理**: 增加更多的错误处理和恢复机制
4. **用户反馈**: 添加路径更新时的视觉反馈（如加载动画）

这些修复确保了乘客端地图的稳定性和一致性，解决了用户体验中的关键问题。