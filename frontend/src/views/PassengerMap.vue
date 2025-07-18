<template>
  <div class="passenger-map">
    <!-- 顶部搜索栏 -->
    <div class="header">
      <div class="search-container">
        <div class="location-item">
          <div class="location-icon pickup">
            <el-icon><Location /></el-icon>
          </div>
          <div class="location-info">
            <div class="location-label">上车地点</div>
            <div class="location-text">{{ pickupAddress || '正在定位...' }}</div>
          </div>
        </div>
        <div class="location-divider"></div>
        <div class="location-item">
          <div class="location-icon destination">
            <el-icon><Location /></el-icon>
          </div>
          <div class="location-info">
            <div class="location-label">目的地</div>
            <el-input
              id="destinationInput"
              v-model="destinationKeyword"
              placeholder="您要去哪里？"
              class="destination-input"
              @input="handleDestinationInput"
              @focus="handleDestinationFocus"
              clearable
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 地图区域 -->
    <div id="mapContainer" class="map-container"></div>

    <!-- 底部车型选择 -->
    <div class="bottom-panel">
      <div class="route-info" v-if="routeInfo">
        <div class="route-details">
          <span class="distance">{{ (routeInfo.distance / 1000).toFixed(1) }}km</span>
          <span class="duration">约{{ Math.ceil(routeInfo.duration / 60) }}分钟</span>
        </div>
      </div>
      
      <div class="car-types">
        <div class="car-type-item" :class="{ active: selectedCarType === 'economy' }" @click="selectCarType('economy')">
          <div class="car-icon">🚗</div>
          <div class="car-info">
            <div class="car-name">快车</div>
            <div class="car-price">¥{{ getPrice('economy') }}</div>
          </div>
        </div>
        <div class="car-type-item" :class="{ active: selectedCarType === 'comfort' }" @click="selectCarType('comfort')">
          <div class="car-icon">🚙</div>
          <div class="car-info">
            <div class="car-name">专车</div>
            <div class="car-price">¥{{ getPrice('comfort') }}</div>
          </div>
        </div>
        <div class="car-type-item" :class="{ active: selectedCarType === 'luxury' }" @click="selectCarType('luxury')">
          <div class="car-icon">🏎️</div>
          <div class="car-info">
            <div class="car-name">豪华车</div>
            <div class="car-price">¥{{ getPrice('luxury') }}</div>
          </div>
        </div>
      </div>
      
      <el-button 
        type="primary" 
        class="call-car-btn" 
        :disabled="!canOrder" 
        @click="handleCallCar"
        size="large"
      >
        {{ callCarText }}
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Location } from '@element-plus/icons-vue'
import { mapConfig, getMapApiUrl, getRestApiUrl, getSecurityConfig } from '@/config/map'

let map = null
let pickupMarker = null
let destMarker = null
let routeLine = null
let autocomplete = null  // 添加Autocomplete实例

const currentPosition = ref({ lng: 0, lat: 0 })
const pickupAddress = ref('')
const destination = ref(null)
const destinationKeyword = ref('')
const routeInfo = ref(null)
const selectedCarType = ref('economy')
const canOrder = ref(false)
const isCalling = ref(false)

// 移除防抖计时器，不再需要

// 车型价格配置
const carTypes = {
  economy: { basePrice: 10, perKm: 2.5, name: '快车' },
  comfort: { basePrice: 15, perKm: 3.5, name: '专车' },
  luxury: { basePrice: 25, perKm: 5.0, name: '豪华车' }
}

// 计算价格
const getPrice = (type) => {
  if (!routeInfo.value) return '--'
  const config = carTypes[type]
  const distance = routeInfo.value.distance / 1000
  const price = config.basePrice + distance * config.perKm
  return Math.round(price)
}

// 叫车按钮文本
const callCarText = computed(() => {
  if (isCalling.value) return '正在叫车...'
  if (!canOrder.value) return '请选择目的地'
  return '立即叫车'
})

// 初始化地图
onMounted(() => {
  console.log('开始初始化地图...')
  
  // 延迟一点时间确保DOM完全渲染
  setTimeout(() => {
    if (window.AMap) {
      console.log('高德地图已加载，直接初始化')
      initMap()
    } else {
      console.log('开始加载高德地图API...')
      
      // 添加安全密钥配置
      window._AMapSecurityConfig = getSecurityConfig();
      
      const script = document.createElement('script')
      script.src = getMapApiUrl()
      script.onload = () => {
        console.log('高德地图API加载成功')
        setTimeout(initMap, 200)
      }
      script.onerror = (error) => {
        console.error('高德地图API加载失败:', error)
        ElMessage.error('地图加载失败，请检查网络连接')
      }
      document.head.appendChild(script)
    }
  }, 500)
})

function initMap() {
  console.log('开始创建地图实例...')
  
  try {
    // 确保容器存在
    const container = document.getElementById('mapContainer')
    if (!container) {
      console.error('地图容器不存在')
      return
    }
    
    console.log('地图容器尺寸:', container.offsetWidth, container.offsetHeight)
    
    // 设置容器最小高度
    container.style.minHeight = '400px'
    container.style.backgroundColor = '#f0f0f0'
    
    // 使用更简单的地图配置
    map = new window.AMap.Map('mapContainer', {
      resizeEnable: true,
      zoom: 15,
      center: [116.397428, 39.90923], // 默认北京中心
      mapStyle: 'amap://styles/normal',
      viewMode: '2D'
    })
    
    console.log('地图实例创建成功')
    
    // 强制重新渲染地图
    setTimeout(() => {
      if (map) {
        map.resize()
        console.log('地图重新调整大小')
        
        // 添加地图加载完成事件
        map.on('complete', function() {
          console.log('地图加载完成')
        })
        
        // 添加地图错误事件
        map.on('error', function(error) {
          console.error('地图错误:', error)
        })
      }
    }, 300)
    
    // 定位
    window.AMap.plugin(['AMap.Geolocation', 'AMap.Autocomplete'], function () {
      console.log('开始定位...')
      const geolocation = new window.AMap.Geolocation({
        enableHighAccuracy: true,
        timeout: 10000
      })
      map.addControl(geolocation)
      geolocation.getCurrentPosition((status, result) => {
        console.log('定位结果:', status, result)
        if (status === 'complete') {
          const { lng, lat } = result.position
          currentPosition.value = { lng, lat }
          
          // 添加上车点标记
          pickupMarker = new window.AMap.Marker({
            position: [lng, lat],
            map,
            icon: new window.AMap.Icon({
              size: new window.AMap.Size(32, 32),
              image: 'https://webapi.amap.com/theme/v1.3/markers/n/mark_b.png'
            })
          })
          
          map.setCenter([lng, lat])
          console.log('定位成功，当前位置:', lng, lat)
          
          // 获取地址
          getAddressFromLocation(lng, lat)
          
          // 初始化Autocomplete
          initAutocomplete()
        } else {
          console.error('定位失败:', status)
          ElMessage.error('定位失败，请检查浏览器权限')
          pickupAddress.value = '定位失败'
          
          // 即使定位失败也初始化Autocomplete
          initAutocomplete()
        }
      })
    })
  } catch (error) {
    console.error('地图初始化失败:', error)
    ElMessage.error('地图初始化失败: ' + error.message)
  }
}

// 初始化Autocomplete
const initAutocomplete = () => {
  try {
    console.log('开始初始化Autocomplete...')
    
    // 确保AMap.Autocomplete插件已加载
    if (!window.AMap.Autocomplete) {
      console.error('AMap.Autocomplete插件未加载')
      return
    }
    
    // 确保输入框存在
    const inputElement = document.getElementById('destinationInput')
    if (!inputElement) {
      console.error('目的地输入框不存在')
      return
    }
    
    // 创建Autocomplete实例
    autocomplete = new window.AMap.Autocomplete({
      input: 'destinationInput',
      city: '全国',  // 支持全国搜索
      citylimit: false,  // 不限制城市
      type: '商务写字楼|购物相关|餐饮服务|风景名胜|交通设施服务|生活服务|体育休闲服务|医疗保健服务|住宿服务|科教文化服务|政府机构及社会团体|商务住宅|汽车服务|汽车销售|汽车维修|摩托车服务|餐饮相关|购物相关|生活服务|商务住宅|政府机构及社会团体|科教文化服务|交通设施服务|金融保险服务|公司企业|道路附属设施|地名地址信息|公共设施',
      extensions: 'all'
    })
    
    console.log('Autocomplete实例创建成功')
    
    // 监听选择事件
    autocomplete.on('select', (e) => {
      console.log('Autocomplete选择结果:', e)
      
      if (e.poi) {
        const poi = e.poi
        const destinationItem = {
          name: poi.name,
          address: poi.address || poi.district + poi.adcode,
          location: poi.location,
          distance: poi.distance ? poi.distance + 'm' : '',
          type: poi.type,
          tel: poi.tel || '',
          rating: poi.biz_ext?.rating || '',
          source: 'autocomplete',
          city: poi.cityname || poi.pname || '',
          province: poi.pname || ''
        }
        
        console.log('Autocomplete选择的目的地:', destinationItem)
        
        // 选择目的地
        selectDestination(destinationItem)
      }
    })
    
    // 监听搜索事件
    autocomplete.on('search', (e) => {
      console.log('Autocomplete搜索事件:', e)
    })
    
    // 监听错误事件
    autocomplete.on('error', (e) => {
      console.error('Autocomplete错误:', e)
    })
    
    console.log('Autocomplete初始化完成')
    
  } catch (error) {
    console.error('Autocomplete初始化失败:', error)
    ElMessage.error('自动完成功能初始化失败')
  }
}

// 删除热门地点加载功能，只使用Autocomplete

// 目的地输入
const handleDestinationInput = async () => {
  if (!destinationKeyword.value) {
    return
  }
  
  // 只使用Autocomplete，不进行自定义搜索
  console.log('使用Autocomplete搜索:', destinationKeyword.value)
}

// 删除自定义搜索功能，只使用Autocomplete

// 删除相关度计算函数，不再需要

// 删除热门地点获取功能，只使用Autocomplete

// 根据坐标获取地址
const getAddressFromLocation = async (lng, lat) => {
  try {
    console.log('开始获取地址:', lng, lat)
    
    // 直接调用高德地图逆地理编码API
    const response = await fetch(getRestApiUrl('geocode/regeo', {
      location: `${lng},${lat}`,
      extensions: 'all'
    }))
    const data = await response.json()
    
    console.log('地址解析结果:', data)
    
    if (data.status === '1' && data.regeocode) {
      pickupAddress.value = data.regeocode.formatted_address
      console.log('获取到地址:', data.regeocode.formatted_address)
    } else {
      console.log('地址获取失败，使用备用方案')
      // 备用方案：使用坐标作为地址
      pickupAddress.value = `位置 (${lat.toFixed(6)}, ${lng.toFixed(6)})`
    }
  } catch (error) {
    console.error('地址获取异常:', error)
    // 备用方案：使用坐标作为地址
    pickupAddress.value = `位置 (${lat.toFixed(6)}, ${lng.toFixed(6)})`
  }
}

// 目的地输入框聚焦
const handleDestinationFocus = () => {
  console.log('目的地输入框聚焦')
  
  // 显示Autocomplete状态
  console.log('Autocomplete状态:', {
    autocomplete: !!autocomplete,
    inputElement: !!document.getElementById('destinationInput'),
    AMapAutocomplete: !!window.AMap?.Autocomplete
  })
}

// 选择目的地
const selectDestination = (item) => {
  console.log('选择目的地:', item)
  
  destination.value = item
  destinationKeyword.value = item.name
  
  // 地图上显示目的地
  if (destMarker) map.remove(destMarker)
  
  // 解析位置坐标
  let lng, lat
  if (typeof item.location === 'string') {
    [lng, lat] = item.location.split(',').map(Number)
  } else if (item.location && item.location.lng && item.location.lat) {
    lng = item.location.lng
    lat = item.location.lat
  } else {
    console.error('无法解析目的地坐标:', item)
    return
  }
  
  console.log('目的地坐标:', lng, lat)
  
  destMarker = new window.AMap.Marker({
    position: [lng, lat],
    map,
    icon: new window.AMap.Icon({
      size: new window.AMap.Size(32, 32),
      image: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzIiIGhlaWdodD0iMzIiIHZpZXdCb3g9IjAgMCAzMiAzMiIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPGNpcmNsZSBjeD0iMTYiIGN5PSIxNiIgcj0iMTIiIGZpbGw9IiNGRjQ0NDQiLz4KPGNpcmNsZSBjeD0iMTYiIGN5PSIxNiIgcj0iNiIgZmlsbD0id2hpdGUiLz4KPC9zdmc+'
    })
  })
  
  // 立即调整地图视野以显示起点和终点
  if (pickupMarker && destMarker) {
    map.setFitView([pickupMarker, destMarker], false, [50, 50, 50, 50])
  }
  
  // 路径规划
  showRoute()
}

// 路径规划
const showRoute = async () => {
  if (!destination.value) return
  
  const origin = `${currentPosition.value.lng},${currentPosition.value.lat}`
  const dest = destination.value.location
  
  try {
    // 直接调用高德地图路径规划API
    const response = await fetch(getRestApiUrl('direction/driving', {
      origin: origin,
      destination: dest
    }))
    const data = await response.json()
    
    console.log('路径规划结果:', data)
    
    if (data.status === '1' && data.route && data.route.paths && data.route.paths.length > 0) {
      // 清除旧路线
      if (routeLine) map.remove(routeLine)
      
      // 获取路径信息
      const path = data.route.paths[0]
      routeInfo.value = {
        distance: parseInt(path.distance),
        duration: parseInt(path.duration)
      }
      canOrder.value = true
      
      // 在地图上绘制路径
      if (path.steps && path.steps.length > 0) {
        const pathPoints = []
        path.steps.forEach(step => {
          if (step.polyline) {
            step.polyline.split(';').forEach(point => {
              const [lng, lat] = point.split(',').map(Number)
              pathPoints.push([lng, lat])
            })
          }
        })
        
        if (pathPoints.length > 0) {
          routeLine = new window.AMap.Polyline({
            path: pathPoints,
            strokeColor: '#409EFF',
            strokeWeight: 6,
            strokeOpacity: 0.8
          })
          map.add(routeLine)
          // 调整地图视野以显示完整路径
          map.setFitView([pickupMarker, destMarker, routeLine], false, [50, 50, 50, 50])
        }
      }
    } else {
      console.log('路径规划失败，使用模拟数据')
      // 备用方案：使用模拟数据
      const distance = Math.random() * 10000 + 1000 // 1-11公里
      const duration = Math.random() * 1800 + 600 // 10-40分钟
      routeInfo.value = {
        distance: distance,
        duration: duration
      }
      canOrder.value = true
      
      // 在地图上画一条简单的直线
      if (routeLine) map.remove(routeLine)
      routeLine = new window.AMap.Polyline({
        path: [
          [currentPosition.value.lng, currentPosition.value.lat],
          [dest.split(',')[0], dest.split(',')[1]]
        ],
        strokeColor: '#409EFF',
        strokeWeight: 6,
        strokeOpacity: 0.8
      })
      map.add(routeLine)
      // 调整地图视野以显示完整路径
      map.setFitView([pickupMarker, destMarker, routeLine], false, [50, 50, 50, 50])
    }
  } catch (error) {
    console.error('路径规划异常:', error)
    // 备用方案：使用模拟数据
    const distance = Math.random() * 10000 + 1000
    const duration = Math.random() * 1800 + 600
    routeInfo.value = {
      distance: distance,
      duration: duration
    }
    canOrder.value = true
  }
}

// 选择车型
const selectCarType = (type) => {
  selectedCarType.value = type
}

// 叫车
const handleCallCar = async () => {
  if (!canOrder.value) return
  
  isCalling.value = true
  
  try {
    // 模拟叫车过程
    await new Promise(resolve => setTimeout(resolve, 2000))
    
    ElMessage.success(`已为您呼叫${carTypes[selectedCarType.value].name}，司机正在赶来...`)
    
    // 这里可以调用实际的下单API
    // const orderData = {
    //   pickupAddress: pickupAddress.value,
    //   destinationAddress: destination.value.name,
    //   pickupLatitude: currentPosition.value.lat,
    //   pickupLongitude: currentPosition.value.lng,
    //   destinationLatitude: destination.value.location.split(',')[1],
    //   destinationLongitude: destination.value.location.split(',')[0],
    //   carType: selectedCarType.value,
    //   estimatedPrice: getPrice(selectedCarType.value)
    // }
    
  } catch (error) {
    ElMessage.error('叫车失败，请重试')
  } finally {
    isCalling.value = false
  }
}
</script>

<style scoped>
.passenger-map {
  position: relative;
  width: 100vw;
  height: 100vh;
  background: #f5f5f5;
  overflow: hidden;
}

.header {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: white;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  height: 120px;
}

.search-container {
  display: flex;
  align-items: center;
  gap: 15px;
}

.location-item {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.location-icon {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 12px;
}

.location-icon.pickup {
  background: #409EFF;
}

.location-icon.destination {
  background: #FF4444;
}

.location-info {
  flex: 1;
}

.location-label {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.location-text {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.location-divider {
  width: 1px;
  height: 40px;
  background: #eee;
}

.destination-input {
  border: none;
  padding: 0;
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.destination-input :deep(.el-input__wrapper) {
  box-shadow: none;
  padding: 0;
}

/* 删除自定义搜索相关样式，只保留Autocomplete */

.map-container {
  width: 100vw;
  height: calc(100vh - 120px - 200px);
  margin-top: 120px;
  background: #f0f0f0;
  position: relative;
  z-index: 1;
  min-height: 400px;
  border: 1px solid #ddd;
}

.bottom-panel {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: white;
  border-radius: 20px 20px 0 0;
  padding: 20px;
  box-shadow: 0 -4px 12px rgba(0, 0, 0, 0.1);
  z-index: 100;
}

.route-info {
  text-align: center;
  margin-bottom: 20px;
}

.route-details {
  display: flex;
  justify-content: center;
  gap: 20px;
}

.distance, .duration {
  font-size: 14px;
  color: #666;
}

.car-types {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
}

.car-type-item {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 15px;
  border: 2px solid #f0f0f0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.car-type-item:hover {
  border-color: #409EFF;
}

.car-type-item.active {
  border-color: #409EFF;
  background: #f0f8ff;
}

.car-icon {
  font-size: 24px;
}

.car-info {
  flex: 1;
}

.car-name {
  font-size: 14px;
  color: #333;
  margin-bottom: 4px;
}

.car-price {
  font-size: 16px;
  color: #409EFF;
  font-weight: bold;
}

.call-car-btn {
  width: 100%;
  height: 50px;
  border-radius: 25px;
  font-size: 16px;
  font-weight: bold;
}

.call-car-btn:disabled {
  background: #ccc;
  border-color: #ccc;
}
</style> 