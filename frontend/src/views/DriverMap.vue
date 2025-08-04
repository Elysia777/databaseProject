<template>
  <div class="driver-map">
    <!-- 顶部状态栏 -->
    <div class="header">
      <div class="status-container">
        <div class="driver-status" :class="{ online: isOnline }">
          <div class="status-indicator"></div>
          <span>{{ isOnline ? '在线接单' : '离线' }}</span>
          <el-switch 
            v-model="isOnline" 
            @change="handleStatusChange"
            :loading="statusLoading"
          />
        </div>
        <div class="earnings-info">
          <span>今日收入: ¥{{ todayEarnings }}</span>
          <span>完成订单: {{ completedOrders }}</span>
        </div>
      </div>
    </div>

    <!-- 地图区域 -->
    <div id="driverMapContainer" class="map-container"></div>

    <!-- 订单通知弹窗列表 -->
    <div class="order-notifications-container">
      <div 
        v-for="order in pendingOrders" 
        :key="order.orderId"
        class="order-notification"
        :style="{ top: (pendingOrders.indexOf(order) * 180 + 20) + 'px' }"
      >
        <div class="notification-header">
          <h3>
            {{ order.orderType === 'RESERVATION' ? '📅 预约单' : '🚗 新订单' }} 
            #{{ order.orderNumber?.slice(-4) }}
          </h3>
          <div class="countdown" :class="{ warning: order.countdown <= 5 }">
            {{ order.countdown }}s
          </div>
        </div>
        <div class="order-details">
          <div class="route-info">
            <div class="location">
              <span class="label">上车点:</span>
              <span class="address">{{ order.pickupAddress }}</span>
            </div>
            <div class="location">
              <span class="label">目的地:</span>
              <span class="address">{{ order.destinationAddress }}</span>
            </div>
          </div>
          <div class="order-meta">
            <span class="distance">{{ parseFloat(order.distance || 0).toFixed(2) }}km</span>
            <span class="price">¥{{ order.estimatedFare || '待计算' }}</span>
            <div v-if="order.orderType === 'RESERVATION' && order.scheduledTime" class="scheduled-time">
              <span class="label">预约时间:</span>
              <span class="time">{{ formatScheduledTime(order.scheduledTime) }}</span>
            </div>
          </div>
        </div>
        <div class="notification-actions">
          <el-button 
            @click="rejectOrder(order.orderId)" 
            size="large"
            :disabled="order.processing"
          >
            拒单
          </el-button>
          <el-button 
            type="primary" 
            @click="acceptOrder(order.orderId)" 
            size="large"
            :loading="order.processing"
          >
            接单
          </el-button>
        </div>
      </div>
    </div>

    <!-- 当前订单面板 -->
    <div v-if="currentOrder" class="current-order-panel">
      <div class="order-header">
        <h3>当前订单</h3>
        <el-tag :type="getOrderStatusType(currentOrder.status)">
          {{ getOrderStatusText(currentOrder.status) }}
        </el-tag>
      </div>
      <div class="order-content">
        <div class="passenger-info">
          <span>乘客: {{ currentOrder.passengerName || '乘客' + currentOrder.passengerId }}</span>
          <span>电话: {{ currentOrder.passengerPhone || '***' }}</span>
        </div>
        
        <!-- 预约单显示预约时间 -->
        <div v-if="currentOrder.orderType === 'RESERVATION' && currentOrder.scheduledTime" class="scheduled-info">
          <div class="scheduled-time-info">
            <span class="icon">📅</span>
            <span>预约时间: {{ formatScheduledTime(currentOrder.scheduledTime) }}</span>
          </div>
        </div>
        
        <div class="route-details">
          <div class="route-item">
            <span class="icon">📍</span>
            <span>{{ currentOrder.pickupAddress }}</span>
          </div>
          <div class="route-item">
            <span class="icon">🎯</span>
            <span>{{ currentOrder.destinationAddress }}</span>
          </div>
        </div>
        <div class="order-actions">
          <el-button 
            v-if="currentOrder.status === 'ASSIGNED'" 
            type="primary" 
            @click="confirmArrival"
            size="large"
          >
            确认到达
          </el-button>
          <el-button 
            v-if="currentOrder.status === 'PICKUP'" 
            type="success" 
            @click="startTrip"
            size="large"
          >
            开始行程
          </el-button>
          <el-button 
            v-if="currentOrder.status === 'IN_PROGRESS'" 
            type="warning" 
            @click="completeOrder"
            size="large"
          >
            完成订单
          </el-button>
          
          <!-- 司机取消订单按钮 -->
          <el-button 
            v-if="canDriverCancelOrder" 
            type="danger" 
            @click="handleDriverCancelOrder"
            :loading="cancelLoading"
            size="large"
            plain
          >
            取消订单
          </el-button>
        </div>
      </div>
    </div>

    <!-- 导航信息 -->
    <div v-if="navigationInfo" class="navigation-panel">
      <div class="nav-header">
        <span>{{ navigationInfo.instruction }}</span>
        <el-button @click="stopNavigation" size="small">停止导航</el-button>
      </div>
      <div class="nav-details">
        <span>距离: {{ navigationInfo.distance }}</span>
        <span>预计: {{ navigationInfo.duration }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useDriverStore } from '@/stores/driver'
import { mapConfig, getMapApiUrl, getRestApiUrl, getSecurityConfig } from '@/config/map'
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

const userStore = useUserStore()
const driverStore = useDriverStore()

// 地图相关
let map = null
let driverMarker = null
let pickupMarker = null
let destinationMarker = null
let routeLine = null
let geolocation = null

// 统一的路径规划配置
const getDrivingConfig = () => ({
  map: null,
  panel: null,
  hideMarkers: true,
  showTraffic: false,
  policy: window.AMap.DrivingPolicy.LEAST_DISTANCE, // 最短距离，避免绕远路
  ferry: 0, // 不走轮渡，避免水路
  province: "全国", // 使用全国范围，避免地域限制
  extensions: "all", // 获取详细路径信息
  avoidPolygons: [], // 避让区域
  avoidRoad: "", // 避让道路
  isOutline: false // 不返回路线轮廓
})

// 状态管理 - 使用全局store
const isOnline = computed(() => driverStore.isOnline)
const currentPosition = computed(() => driverStore.currentPosition)
const todayEarnings = computed(() => driverStore.todayEarnings)
const completedOrders = computed(() => driverStore.completedOrders)
const pendingOrders = computed(() => driverStore.pendingOrders)
const currentOrder = computed(() => driverStore.currentOrder)
const navigationInfo = computed(() => driverStore.navigationInfo)

// 本地UI状态
const statusLoading = ref(false)
const cancelLoading = ref(false)
const orderTimers = new Map() // 每个订单的倒计时定时器
let navigationTimer = null

// WebSocket连接
let stompClient = null

onMounted(async () => {
  console.log('🚀 开始初始化司机地图页面...')
  
  // 立即注册全局函数，让store能够通知地图组件
  window.handleDriverMapUpdate = handleDriverOrderUpdate
  console.log('✅ 已注册全局司机地图消息处理函数')
  
  // 初始化司机状态（包括从后端检查当前订单）
  console.log('🔄 开始初始化司机状态...')
  await driverStore.initDriverState()
  console.log('✅ 司机状态初始化完成')
  
  // 初始化地图和统计数据
  initMap()
  loadTodayStats()
  
  // 延迟一点时间确保所有初始化完成后再处理订单恢复
  setTimeout(() => {
    console.log('🔄 检查是否需要恢复订单导航...')
    
    // 检查是否有进行中的订单需要恢复路径规划
    if (currentOrder.value) {
      console.log('🔄 检测到进行中的订单，恢复路径规划...')
      console.log('📋 订单信息:', currentOrder.value)
      restoreOrderNavigation()
    } else {
      console.log('📱 没有进行中的订单，无需恢复导航')
    }
  }, 2000)
})

onUnmounted(() => {
  // 断开WebSocket连接
  driverStore.disconnectWebSocket()
  
  // 清理全局函数
  if (window.handleDriverMapUpdate) {
    delete window.handleDriverMapUpdate
    console.log('✅ 已清理司机地图消息处理函数')
  }
  
  // 清理所有订单倒计时定时器
  orderTimers.forEach((timer) => {
    clearInterval(timer)
  })
  orderTimers.clear()
  
  if (navigationTimer) {
    clearInterval(navigationTimer)
  }
})

// 初始化地图
const initMap = () => {
  if (window.AMap) {
    createMap()
  } else {
    window._AMapSecurityConfig = getSecurityConfig()
    const script = document.createElement('script')
    script.src = getMapApiUrl()
    script.onload = () => setTimeout(createMap, 200)
    script.onerror = () => ElMessage.error('地图加载失败')
    document.head.appendChild(script)
  }
}

const createMap = () => {
  try {
    map = new window.AMap.Map('driverMapContainer', {
      resizeEnable: true,
      zoom: 16,
      center: [116.397428, 39.90923],
      mapStyle: 'amap://styles/normal'
    })

    // 初始化定位
    window.AMap.plugin(['AMap.Geolocation'], () => {
      geolocation = new window.AMap.Geolocation({
        enableHighAccuracy: true,
        timeout: 10000
      })
      
      map.addControl(geolocation)
      getCurrentLocation()
      
      // 定期更新位置
      setInterval(getCurrentLocation, 30000)
    })

  } catch (error) {
    console.error('地图初始化失败:', error)
    ElMessage.error('地图初始化失败')
  }
}

// 获取当前位置
const getCurrentLocation = () => {
  if (!geolocation) {
    console.error('❌ 地理位置服务未初始化')
    return
  }
  
  console.log('🌍 开始获取当前位置...')
  
  geolocation.getCurrentPosition((status, result) => {
    console.log('📍 定位结果:', status, result)
    
    if (status === 'complete') {
      const { lng, lat } = result.position
      driverStore.updateCurrentPosition({ lng, lat })
      
      console.log('✅ 位置获取成功:', lng, lat)
      
      // 更新司机位置标记
      updateDriverMarker(lng, lat)
      
      // 如果在线，上报位置
      if (isOnline.value) {
        reportLocation(lng, lat)
      }
    } else {
      console.error('❌ 位置获取失败:', status, result)
      
      // 如果定位失败，使用默认位置（大连理工大学）
      const defaultLng = 121.749849
      const defaultLat = 39.044237
      
      console.log('🔄 使用默认位置:', defaultLng, defaultLat)
      driverStore.updateCurrentPosition({ lng: defaultLng, lat: defaultLat })
      
      // 更新司机位置标记
      updateDriverMarker(defaultLng, defaultLat)
      
      // 如果在线，上报默认位置
      if (isOnline.value) {
        reportLocation(defaultLng, defaultLat)
      }
      
      ElMessage.warning('定位失败，使用默认位置')
    }
  })
}

// 更新司机位置标记
const updateDriverMarker = (lng, lat) => {
  if (driverMarker) {
    driverMarker.setPosition([lng, lat])
    // 只有在没有当前订单时才自动居中，避免导航时频繁调整视野
    if (!currentOrder.value) {
      map.setCenter([lng, lat])
    }
  } else {
    driverMarker = new window.AMap.Marker({
      position: [lng, lat],
      map,
      icon: new window.AMap.Icon({
        size: new window.AMap.Size(32, 32),
        image: '🚗'
      }),
      title: '我的位置'
    })
    map.setCenter([lng, lat])
  }
}

// 上报位置到服务器
const reportLocation = async (lng, lat) => {
  try {
    const formData = new URLSearchParams()
    formData.append('latitude', lat)
    formData.append('longitude', lng)
    
    await fetch(`/api/drivers/${userStore.user.driverId}/location`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        'Authorization': `Bearer ${userStore.token}`
      },
      body: formData
    })
  } catch (error) {
    console.error('位置上报失败:', error)
  }
}

// 处理司机订单更新消息（地图相关）
const handleDriverOrderUpdate = (data) => {
  console.log("🔔 司机地图收到订单更新:", data);
  console.log("📋 消息类型:", data.type);

  switch (data.type) {
    case "NEW_ORDER":
      console.log("📨 处理新订单推送");
      if (data.orderId) {
        // 新订单推送，添加到待处理队列并开始倒计时
        handleOrderPush(data);
      }
      break;
    case "ORDER_CANCELLED":
      console.log("❌ 处理订单取消");
      if (data.orderId) {
        // 移除待处理订单
        stopOrderCountdown(data.orderId);
        
        // 如果是当前订单被取消，重置状态
        if (currentOrder.value) {
          const currentOrderId = currentOrder.value.orderId || currentOrder.value.id
          const cancelledOrderId = data.orderId
          
          console.log('比较订单ID:', currentOrderId, 'vs', cancelledOrderId)
          
          if (currentOrderId == cancelledOrderId) { // 使用 == 处理类型转换
            console.log('✅ 当前订单被取消，重置状态')
            resetOrderState();
            ElMessage.warning('当前订单已被取消');
          }
        }
      }
      break;
    case "ORDER_STATUS_CHANGE":
      console.log("📊 处理订单状态变化");
      if (data.orderId && currentOrder.value && currentOrder.value.id === data.orderId) {
        // 更新当前订单状态
        driverStore.updateOrderStatus(data.status);
        
        // 根据状态更新地图显示
        switch (data.status) {
          case 'PICKUP':
            ElMessage.success('乘客已确认上车');
            break;
          case 'IN_PROGRESS':
            ElMessage.success('行程已开始');
            startNavigationToDestination();
            break;
          case 'COMPLETED':
            ElMessage.success('订单已完成');
            resetOrderState();
            break;
        }
      }
      break;
    case "ORDER_ASSIGNED":
      console.log("✅ 处理订单分配确认");
      // 订单分配确认，通常在接单后收到
      break;
    case "DRIVER_LOCATION":
      console.log("📍 处理司机位置更新");
      // 司机位置更新，可以忽略或用于其他司机位置显示
      break;
    default:
      console.log("❓ 未知消息类型:", data.type);
  }
};

// 处理上线/下线状态变化
const handleStatusChange = async (online) => {
  statusLoading.value = true
  
  try {
    const endpoint = online ? 'online' : 'offline'
    const params = online ? `?latitude=${currentPosition.value.lat}&longitude=${currentPosition.value.lng}` : ''
    
    const response = await fetch(`/api/drivers/${userStore.user.driverId}/${endpoint}${params}`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    
    if (response.ok) {
      // 更新store中的在线状态
      driverStore.setOnlineStatus(online)
      
      ElMessage.success(online ? '已上线，开始接单' : '已下线')
      if (online) {
        startLocationTracking()
        // 司机上线时建立WebSocket连接
        console.log('司机上线，建立WebSocket连接...')
        driverStore.connectWebSocket()
      } else {
        stopLocationTracking()
        
        // 检查是否有进行中的订单
        if (currentOrder.value && ['ASSIGNED', 'PICKUP', 'IN_PROGRESS'].includes(currentOrder.value.status)) {
          console.log('司机下线，但有进行中的订单，保持WebSocket连接')
          ElMessage.warning('您有进行中的订单，将保持连接以接收订单更新')
          
          // 只清理待处理订单列表，保留当前订单
          driverStore.clearPendingOrders()
        } else {
          // 没有进行中的订单，可以断开WebSocket连接
          console.log('司机下线，无进行中订单，断开WebSocket连接')
          driverStore.disconnectWebSocket()
          
          // 清理待处理订单列表（保留收入统计和当前订单）
          driverStore.clearPendingOrders()
        }
      }
    } else {
      isOnline.value = !online
      ElMessage.error('状态切换失败')
    }
  } catch (error) {
    isOnline.value = !online
  } finally {
    statusLoading.value = false
  }
}

// 开始位置追踪
const startLocationTracking = () => {
  // 每30秒更新一次位置
  if (navigationTimer) clearInterval(navigationTimer)
  navigationTimer = setInterval(getCurrentLocation, 30000)
}

// 停止位置追踪
const stopLocationTracking = () => {
  if (navigationTimer) {
    clearInterval(navigationTimer)
    navigationTimer = null
  }
}

// WebSocket连接
const connectWebSocket = () => {
  try {
    console.log('开始建立WebSocket连接...')
    console.log('用户信息:', userStore.user)
    console.log('司机ID:', userStore.user?.driverId)
    
    if (!userStore.user?.driverId) {
      console.error('❌ 司机ID为空，无法建立WebSocket连接')
      ElMessage.error('司机信息不完整，请重新登录')
      return
    }
    
    const socket = new SockJS('/ws')
    stompClient = new Client({
      webSocketFactory: () => socket,
      debug: (str) => {
        console.log('STOMP Debug:', str)
      }
    })

    stompClient.onConnect = () => {
      console.log('✅ WebSocket连接成功')
      
      const driverId = userStore.user.driverId.toString()
      console.log('准备订阅司机队列，司机ID:', driverId)
      
      // 订阅订单推送
      const orderSubscription = stompClient.subscribe(`/user/${driverId}/queue/orders`, (message) => {
        console.log('🚗 收到订单推送:', message.body)
        try {
          const orderData = JSON.parse(message.body)
          console.log('解析后的订单数据:', orderData)
          handleOrderPush(orderData)
        } catch (error) {
          console.error('解析订单数据失败:', error)
        }
      })
      
      // 订阅通知
      const notificationSubscription = stompClient.subscribe(`/user/${driverId}/queue/notifications`, (message) => {
        console.log('📢 收到通知:', message.body)
        try {
          const data = JSON.parse(message.body)
          
          // 处理不同类型的通知
          if (data.type === 'ORDER_CANCELLED') {
            console.log('❌ 收到订单取消通知:', data)
            ElMessage.warning(data.reason || '订单已被取消')
            
            // 如果是当前订单被取消，重置状态
            if (currentOrder.value) {
              const currentOrderId = currentOrder.value.orderId || currentOrder.value.id
              const cancelledOrderId = data.orderId
              
              console.log('比较订单ID:', currentOrderId, 'vs', cancelledOrderId)
              
              if (currentOrderId == cancelledOrderId) { // 使用 == 而不是 === 来处理类型转换
                console.log('✅ 当前订单被取消，重置状态')
                resetOrderState()
              }
            }
          } else {
            ElMessage.info(data.message || data.reason)
          }
        } catch (error) {
          console.error('解析通知数据失败:', error)
        }
      })
      
      console.log('✅ 订阅完成')
      console.log('订单队列订阅:', orderSubscription)
      console.log('通知队列订阅:', notificationSubscription)
      
      // 发送司机连接消息给后端
      stompClient.publish({
        destination: '/app/driver/connect',
        body: JSON.stringify({
          driverId: driverId,
          timestamp: Date.now()
        })
      })
      
      console.log('✅ 已发送司机连接消息')
    }

    stompClient.onStompError = (frame) => {
      console.error('❌ WebSocket连接失败:', frame)
      ElMessage.error('WebSocket连接失败')
    }

    stompClient.onWebSocketError = (error) => {
      console.error('❌ WebSocket错误:', error)
      // 不显示错误消息，因为这在开发环境中很常见
    }

    stompClient.onDisconnect = () => {
      console.log('⚠️ WebSocket连接断开')
      // 如果司机仍在线，尝试重连
      if (isOnline.value) {
        console.log('🔄 尝试重新连接WebSocket...')
        setTimeout(() => {
          if (isOnline.value && !stompClient?.connected) {
            connectWebSocket()
          }
        }, 3000) // 3秒后重连
      }
    }

    stompClient.activate()
    console.log('WebSocket客户端已激活')
  } catch (error) {
    console.error('❌ WebSocket连接错误:', error)
    ElMessage.error('WebSocket连接失败')
  }
}

// 处理订单推送 - 新的多订单队列系统
const handleOrderPush = (orderData) => {
  if (orderData.type === 'NEW_ORDER') {
    console.log('🚗 处理新订单推送:', orderData)
    
    // 创建新的订单对象，包含完整的坐标信息
    const newOrderItem = {
      orderId: orderData.orderId,
      orderNumber: orderData.orderNumber,
      pickupAddress: orderData.pickupAddress,
      destinationAddress: orderData.destinationAddress,
      pickupLatitude: orderData.pickupLatitude,
      pickupLongitude: orderData.pickupLongitude,
      destinationLatitude: orderData.destinationLatitude,
      destinationLongitude: orderData.destinationLongitude,
      distance: orderData.distance || 0,
      estimatedFare: orderData.estimatedFare,
      passengerId: orderData.passengerId,
      passengerName: orderData.passengerName,
      passengerPhone: orderData.passengerPhone,
      countdown: 15,
      processing: false,
      timestamp: Date.now()
    }
    
    // 添加到待处理订单队列
    driverStore.addPendingOrder(newOrderItem)
    
    // 开始倒计时
    startOrderCountdown(orderData.orderId)
    
    // 播放通知音
    playNotificationSound()
    
    console.log('✅ 订单已添加到队列，当前队列长度:', pendingOrders.value.length)
    console.log('📍 订单坐标信息:', {
      pickup: [orderData.pickupLongitude, orderData.pickupLatitude],
      destination: [orderData.destinationLongitude, orderData.destinationLatitude]
    })
  }
}

// 开始订单倒计时 - 新的多订单系统
const startOrderCountdown = (orderId) => {
  console.log('🕐 开始订单倒计时:', orderId)
  
  const timer = setInterval(() => {
    const orderIndex = pendingOrders.value.findIndex(order => order.orderId === orderId)
    if (orderIndex === -1) {
      // 订单已被处理，清除定时器
      clearInterval(timer)
      orderTimers.delete(orderId)
      return
    }
    
    const order = pendingOrders.value[orderIndex]
    order.countdown--
    
    console.log(`⏰ 订单 ${orderId} 倒计时: ${order.countdown}s`)
    
    if (order.countdown <= 0) {
      console.log('⏰ 订单超时，自动拒绝:', orderId)
      clearInterval(timer)
      orderTimers.delete(orderId)
      autoRejectOrder(orderId)
    }
  }, 1000)
  
  orderTimers.set(orderId, timer)
}

// 停止订单倒计时
const stopOrderCountdown = (orderId) => {
  const timer = orderTimers.get(orderId)
  if (timer) {
    clearInterval(timer)
    orderTimers.delete(orderId)
    console.log('⏹️ 已停止订单倒计时:', orderId)
  }
}

// 接单 - 新的多订单系统
const acceptOrder = async (orderId) => {
  console.log('🎯 司机接单:', orderId)
  
  // 找到对应的订单
  const orderIndex = pendingOrders.value.findIndex(order => order.orderId === orderId)
  if (orderIndex === -1) {
    console.error('❌ 订单不存在:', orderId)
    ElMessage.error('订单不存在')
    return
  }
  
  const order = pendingOrders.value[orderIndex]
  order.processing = true
  
  // 停止倒计时
  stopOrderCountdown(orderId)
  
  try {
    const response = await fetch(`/api/drivers/${userStore.user.driverId}/accept-order/${orderId}`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    
    const result = await response.json()
    
    if (response.ok && result.code === 200) {
      console.log('✅ 接单成功:', orderId)
      console.log('订单详情:', result.data)
      
      // 设置为当前订单，优先使用后端返回的数据，缺失时使用队列中的数据
      const orderData = result.data
      const queueOrder = order // 队列中的订单数据作为备用
      
      const currentOrderData = {
        id: orderData.id || queueOrder.orderId,
        orderId: orderData.id || queueOrder.orderId,
        orderNumber: orderData.orderNumber || queueOrder.orderNumber,
        orderType: orderData.orderType || queueOrder.orderType, // 添加订单类型
        pickupAddress: orderData.pickupAddress || queueOrder.pickupAddress,
        destinationAddress: orderData.destinationAddress || queueOrder.destinationAddress,
        pickupLatitude: orderData.pickupLatitude || queueOrder.pickupLatitude,
        pickupLongitude: orderData.pickupLongitude || queueOrder.pickupLongitude,
        destinationLatitude: orderData.destinationLatitude || queueOrder.destinationLatitude,
        destinationLongitude: orderData.destinationLongitude || queueOrder.destinationLongitude,
        estimatedFare: orderData.estimatedFare || queueOrder.estimatedFare,
        passengerId: orderData.passengerId || queueOrder.passengerId,
        passengerName: orderData.passengerName || queueOrder.passengerName,
        passengerPhone: orderData.passengerPhone || queueOrder.passengerPhone,
        scheduledTime: orderData.scheduledTime || queueOrder.scheduledTime, // 添加预约时间
        status: 'ASSIGNED'
      }
      
      // 使用store设置当前订单
      driverStore.setCurrentOrder(currentOrderData)
      
      console.log('✅ 合并后的订单信息:', currentOrderData)
      console.log('📍 坐标检查:', {
        pickupLatitude: currentOrderData.pickupLatitude,
        pickupLongitude: currentOrderData.pickupLongitude,
        destinationLatitude: currentOrderData.destinationLatitude,
        destinationLongitude: currentOrderData.destinationLongitude
      })
      
      // 从待处理队列中移除
      driverStore.removePendingOrder(orderId)
      
      // 立即开始导航到上车点
      startNavigationToPickup()
      
      ElMessage.success('接单成功，开始导航到上车点')
    } else {
      console.error('❌ 接单失败:', orderId, result.message)
      order.processing = false
      ElMessage.error('接单失败: ' + (result.message || '订单可能已被其他司机接单'))
    }
  } catch (error) {
    console.error('❌ 接单网络错误:', error)
    order.processing = false
    ElMessage.error('网络错误')
  }
}

// 拒单 - 新的多订单系统
const rejectOrder = async (orderId) => {
  console.log('❌ 司机拒单:', orderId)
  
  // 找到对应的订单
  const orderIndex = pendingOrders.value.findIndex(order => order.orderId === orderId)
  if (orderIndex === -1) {
    console.error('❌ 订单不存在:', orderId)
    return
  }
  
  const order = pendingOrders.value[orderIndex]
  order.processing = true
  
  // 停止倒计时
  stopOrderCountdown(orderId)
  
  try {
    const response = await fetch(`/api/drivers/${userStore.user.driverId}/reject-order/${orderId}`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    
    if (response.ok) {
      console.log('✅ 拒单成功:', orderId)
      // 从待处理队列中移除
      pendingOrders.value.splice(orderIndex, 1)
      ElMessage.info('已拒绝订单')
    } else {
      console.error('❌ 拒单失败:', orderId)
      order.processing = false
      ElMessage.error('拒单失败')
    }
  } catch (error) {
    console.error('❌ 拒单网络错误:', error)
    order.processing = false
    ElMessage.error('网络错误')
  }
}

// 自动拒单（超时）
const autoRejectOrder = (orderId) => {
  console.log('⏰ 自动拒单（超时）:', orderId)
  
  // 找到对应的订单
  const orderIndex = pendingOrders.value.findIndex(order => order.orderId === orderId)
  if (orderIndex === -1) {
    console.error('❌ 订单不存在:', orderId)
    return
  }
  
  // 从待处理队列中移除
  pendingOrders.value.splice(orderIndex, 1)
  
  // 显示超时消息
  ElMessage.warning('订单已超时，自动拒绝')
  
  // 可选：向后端发送超时拒单通知
  fetch(`/api/drivers/${userStore.user.driverId}/reject-order/${orderId}`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${userStore.token}`
    },
    body: new URLSearchParams({
      reason: '超时未响应'
    })
  }).catch(error => {
    console.error('发送超时拒单通知失败:', error)
  })
}

// 开始导航到上车点（接单后立即调用）
const startNavigationToPickup = () => {
  if (!currentOrder.value) {
    console.error('❌ 无当前订单，无法开始导航')
    return
  }
  
  console.log('🧭 开始导航到上车点')
  console.log('订单信息:', currentOrder.value)
  
  // 检查订单是否有坐标信息
  if (!currentOrder.value.pickupLatitude || !currentOrder.value.pickupLongitude) {
    console.error('❌ 订单缺少上车点坐标信息')
    ElMessage.error('订单信息不完整，无法开始导航')
    return
  }
  
  // 检查司机当前位置
  if (!currentPosition.value.lng || !currentPosition.value.lat) {
    console.error('❌ 司机当前位置未获取')
    ElMessage.error('正在获取位置信息，请稍候...')
    // 尝试重新获取位置
    getCurrentLocation()
    return
  }
  
  // 添加上车点标记
  if (pickupMarker) map.remove(pickupMarker)
  
  const pickupPos = [currentOrder.value.pickupLongitude, currentOrder.value.pickupLatitude]
  console.log('上车点坐标:', pickupPos)
  console.log('司机位置:', [currentPosition.value.lng, currentPosition.value.lat])
  
  pickupMarker = new window.AMap.Marker({
    position: pickupPos,
    map,
    icon: new window.AMap.Icon({
      size: new window.AMap.Size(32, 32),
      image: 'https://webapi.amap.com/theme/v1.3/markers/n/start.png'
    }),
    title: '上车点'
  })
  
  // 添加目的地标记
  if (destinationMarker) map.remove(destinationMarker)
  
  const destPos = [currentOrder.value.destinationLongitude, currentOrder.value.destinationLatitude]
  console.log('目的地坐标:', destPos)
  
  destinationMarker = new window.AMap.Marker({
    position: destPos,
    map,
    icon: new window.AMap.Icon({
      size: new window.AMap.Size(32, 32),
      image: 'https://webapi.amap.com/theme/v1.3/markers/n/end.png'
    }),
    title: '目的地'
  })
  
  // 立即规划路线并开始导航
  console.log('🗺️ 开始规划路线...')
  planRoute(
    [currentPosition.value.lng, currentPosition.value.lat],
    pickupPos,
    '🚗 前往上车点接乘客'
  )
  
  // 开始实时位置追踪和导航更新
  startRealTimeNavigation()
  
  ElMessage.success('导航已开始，正在前往上车点')
}

// 开始导航到目的地（确认接到乘客后调用）
const startNavigationToDestination = () => {
  if (!currentOrder.value) {
    console.error('❌ 无当前订单，无法开始导航')
    return
  }
  
  console.log('🎯 开始导航到目的地')
  console.log('订单信息:', currentOrder.value)
  
  // 检查订单是否有目的地坐标信息
  if (!currentOrder.value.destinationLatitude || !currentOrder.value.destinationLongitude) {
    console.error('❌ 订单缺少目的地坐标信息')
    ElMessage.error('订单信息不完整，无法开始导航')
    return
  }
  
  // 检查司机当前位置
  if (!currentPosition.value.lng || !currentPosition.value.lat) {
    console.error('❌ 司机当前位置未获取')
    ElMessage.error('正在获取位置信息，请稍候...')
    getCurrentLocation()
    return
  }
  
  // 更新目的地标记（如果还没有的话）
  if (!destinationMarker) {
    const destPos = [currentOrder.value.destinationLongitude, currentOrder.value.destinationLatitude]
    console.log('目的地坐标:', destPos)
    
    destinationMarker = new window.AMap.Marker({
      position: destPos,
      map,
      icon: new window.AMap.Icon({
        size: new window.AMap.Size(32, 32),
        image: 'https://webapi.amap.com/theme/v1.3/markers/n/end.png'
      }),
      title: '目的地'
    })
  }
  
  // 立即规划路线并开始导航到目的地
  console.log('🗺️ 开始规划到目的地的路线...')
  planRoute(
    [currentPosition.value.lng, currentPosition.value.lat],
    [currentOrder.value.destinationLongitude, currentOrder.value.destinationLatitude],
    '🎯 前往目的地'
  )
  
  // 重新开始实时位置追踪和导航更新
  startRealTimeNavigation()
  
  ElMessage.success('导航已切换，正在前往目的地')
}

// 开始实时导航
const startRealTimeNavigation = () => {
  console.log('🧭 开始实时导航')
  
  // 每5秒更新一次位置和导航信息
  if (navigationTimer) clearInterval(navigationTimer)
  
  navigationTimer = setInterval(() => {
    // 更新当前位置
    getCurrentLocation()
    
    // 如果有当前订单，重新规划路线
    if (currentOrder.value) {
      if (currentOrder.value.status === 'ASSIGNED' || currentOrder.value.status === 'PICKUP') {
        // 前往上车点（包括已到达但还未开始行程的状态）
        const pickupPos = [currentOrder.value.pickupLongitude, currentOrder.value.pickupLatitude]
        planRoute(
          [currentPosition.value.lng, currentPosition.value.lat],
          pickupPos,
          '🚗 前往上车点接乘客'
        )
      } else if (currentOrder.value.status === 'IN_PROGRESS') {
        // 前往目的地
        const destPos = [currentOrder.value.destinationLongitude, currentOrder.value.destinationLatitude]
        planRoute(
          [currentPosition.value.lng, currentPosition.value.lat],
          destPos,
          '🎯 前往目的地'
        )
      }
    }
  }, 5000) // 每5秒更新一次
}

// 停止实时导航
const stopRealTimeNavigation = () => {
  if (navigationTimer) {
    clearInterval(navigationTimer)
    navigationTimer = null
  }
  console.log('⏹️ 已停止实时导航')
}

// 显示到上车点的路线
const showRouteToPickup = () => {
  if (!currentOrder.value) return
  
  // 添加上车点标记
  if (pickupMarker) map.remove(pickupMarker)
  
  const pickupPos = [currentOrder.value.pickupLongitude, currentOrder.value.pickupLatitude]
  pickupMarker = new window.AMap.Marker({
    position: pickupPos,
    map,
    icon: new window.AMap.Icon({
      size: new window.AMap.Size(32, 32),
      image: 'https://webapi.amap.com/theme/v1.3/markers/n/start.png'
    }),
    title: '上车点'
  })
  
  // 规划路线
  planRoute(
    [currentPosition.value.lng, currentPosition.value.lat],
    pickupPos,
    '前往上车点接乘客'
  )
}

// 显示到目的地的路线
const showRouteToDestination = () => {
  if (!currentOrder.value) return
  
  // 添加目的地标记
  if (destinationMarker) map.remove(destinationMarker)
  
  const destPos = [currentOrder.value.destinationLongitude, currentOrder.value.destinationLatitude]
  destinationMarker = new window.AMap.Marker({
    position: destPos,
    map,
    icon: new window.AMap.Icon({
      size: new window.AMap.Size(32, 32),
      image: 'https://webapi.amap.com/theme/v1.3/markers/n/end.png'
    }),
    title: '目的地'
  })
  
  // 规划路线
  planRoute(
    [currentPosition.value.lng, currentPosition.value.lat],
    destPos,
    '前往目的地'
  )
}

// 路线规划 - 使用Web端JS API
const planRoute = async (origin, destination, instruction) => {
  try {
    console.log('🗺️ 开始路线规划:', {
      origin: origin,
      destination: destination,
      instruction: instruction
    })
    
    // 验证坐标有效性
    if (!origin || !destination || origin.length !== 2 || destination.length !== 2) {
      console.error('❌ 坐标参数无效:', { origin, destination })
      ElMessage.error('坐标信息无效，无法规划路线')
      return
    }
    
    // 检查坐标是否为有效数字
    if (isNaN(origin[0]) || isNaN(origin[1]) || isNaN(destination[0]) || isNaN(destination[1])) {
      console.error('❌ 坐标包含非数字值:', { origin, destination })
      ElMessage.error('坐标格式错误，无法规划路线')
      return
    }
    
    // 检查AMap.Driving是否可用
    if (!window.AMap || !window.AMap.Driving) {
      console.error('❌ AMap.Driving不可用')
      useFallbackRoute(origin, destination, instruction)
      return
    }
    
    // 使用Web端JS API的Driving服务
    const driving = new window.AMap.Driving(getDrivingConfig())
    
    driving.search(
      new window.AMap.LngLat(origin[0], origin[1]),
      new window.AMap.LngLat(destination[0], destination[1]),
      (status, result) => {
        console.log('🗺️ 司机端路线规划结果:', status, result)
        console.log('🚗 使用的路径规划策略:', window.AMap.DrivingPolicy.LEAST_DISTANCE)
        console.log('⛴️ 轮渡设置:', 0)
        
        if (status === 'complete' && result.routes && result.routes.length > 0) {
          const route = result.routes[0]
          
          // 显示导航信息
          navigationInfo.value = {
            instruction,
            distance: (route.distance / 1000).toFixed(1) + 'km',
            duration: Math.ceil(route.time / 60) + '分钟'
          }
          
          console.log('✅ 导航信息已更新:', navigationInfo.value)
          
          // 在地图上绘制路线
          if (routeLine) {
            map.remove(routeLine)
            routeLine = null
          }
          
          // 获取路径点
          const pathPoints = []
          route.steps.forEach(step => {
            if (step.path && step.path.length > 0) {
              step.path.forEach(point => {
                pathPoints.push([point.lng, point.lat])
              })
            }
          })
          
          console.log('🛣️ 解析到路径点数量:', pathPoints.length)
          
          if (pathPoints.length > 0) {
            routeLine = new window.AMap.Polyline({
              path: pathPoints,
              strokeColor: '#409EFF',
              strokeWeight: 6,
              strokeOpacity: 0.8,
              strokeStyle: 'solid',
              lineJoin: 'round',
              lineCap: 'round'
            })
            
            map.add(routeLine)
            console.log('✅ 路线已绘制到地图上')
            
            // 只在第一次规划路线时调整地图视野
            if (!window.routeInitialized) {
              try {
                const markers = []
                if (driverMarker) markers.push(driverMarker)
                if (pickupMarker) markers.push(pickupMarker)
                if (destinationMarker) markers.push(destinationMarker)
                
                if (markers.length > 0) {
                  map.setFitView(markers.concat([routeLine]), false, [50, 50, 50, 50])
                  console.log('✅ 地图视野已调整（首次）')
                  window.routeInitialized = true
                }
              } catch (error) {
                console.warn('⚠️ 地图视野调整失败:', error)
              }
            } else {
              console.log('📍 路线已更新，保持当前地图视野')
            }
          } else {
            console.warn('⚠️ 没有有效的路径点，使用备用方案')
            useFallbackRoute(origin, destination, instruction)
          }
          
        } else {
          console.error('❌ 路线规划失败:', status, result)
          ElMessage.warning('路线规划失败，使用直线距离')
          useFallbackRoute(origin, destination, instruction)
        }
      }
    )
    
  } catch (error) {
    console.error('❌ 路线规划异常:', error)
    ElMessage.warning('路线规划异常，使用直线距离')
    useFallbackRoute(origin, destination, instruction)
  }
}

// 备用路线方案 - 使用直线距离
const useFallbackRoute = (origin, destination, instruction) => {
  try {
    console.log('🔄 使用直线备用方案')
    
    // 清除旧路线
    if (routeLine) {
      map.remove(routeLine)
      routeLine = null
    }
    
    // 绘制直线
    routeLine = new window.AMap.Polyline({
      path: [origin, destination],
      strokeColor: '#FF6B6B',
      strokeWeight: 4,
      strokeOpacity: 0.6,
      strokeStyle: 'dashed'
    })
    map.add(routeLine)
    
    // 计算直线距离
    if (window.AMap && window.AMap.GeometryUtil) {
      const distance = window.AMap.GeometryUtil.distance(origin, destination)
      const duration = Math.ceil(distance / 1000 * 2) // 假设平均速度30km/h
      
      navigationInfo.value = {
        instruction: instruction + ' (直线距离)',
        distance: (distance / 1000).toFixed(1) + 'km',
        duration: duration + '分钟'
      }
    } else {
      navigationInfo.value = {
        instruction: instruction + ' (估算)',
        distance: '计算中...',
        duration: '计算中...'
      }
    }
    
    console.log('✅ 备用路线已设置')
    
  } catch (error) {
    console.error('❌ 备用方案也失败了:', error)
    navigationInfo.value = {
      instruction: instruction + ' (无法计算)',
      distance: '未知',
      duration: '未知'
    }
  }
}

// 确认到达
const confirmArrival = async () => {
  try {
    // 兼容不同的订单ID字段名
    const orderId = currentOrder.value.orderId || currentOrder.value.id
    
    if (!orderId) {
      console.error('❌ 订单ID不存在:', currentOrder.value)
      ElMessage.error('订单信息异常，请刷新页面')
      return
    }
    
    console.log('🚗 确认到达上车点，订单ID:', orderId)
    
    const response = await fetch(`/api/orders/${orderId}/pickup`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    
    if (response.ok) {
      currentOrder.value.status = 'PICKUP'
      driverStore.updateOrderStatus('PICKUP')
      ElMessage.success('已确认到达上车点')
    } else {
      const errorText = await response.text()
      console.error('❌ 确认到达失败:', response.status, errorText)
      ElMessage.error('确认失败')
    }
  } catch (error) {
    console.error('❌ 确认到达网络错误:', error)
    ElMessage.error('网络错误')
  }
}

// 开始行程
const startTrip = async () => {
  try {
    // 兼容不同的订单ID字段名
    const orderId = currentOrder.value.orderId || currentOrder.value.id
    
    if (!orderId) {
      console.error('❌ 订单ID不存在:', currentOrder.value)
      ElMessage.error('订单信息异常，请刷新页面')
      return
    }
    
    console.log('🚗 开始行程，订单ID:', orderId)
    
    const response = await fetch(`/api/orders/${orderId}/start`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    
    if (response.ok) {
      currentOrder.value.status = 'IN_PROGRESS'
      driverStore.updateOrderStatus('IN_PROGRESS')
      
      // 停止到上车点的导航，开始到目的地的导航
      stopRealTimeNavigation()
      startNavigationToDestination()
      
      ElMessage.success('行程已开始，开始导航到目的地')
    } else {
      const errorText = await response.text()
      console.error('❌ 开始行程失败:', response.status, errorText)
      ElMessage.error('开始行程失败')
    }
  } catch (error) {
    console.error('❌ 开始行程网络错误:', error)
    ElMessage.error('网络错误')
  }
}

// 完成订单
const completeOrder = async () => {
  try {
    await ElMessageBox.confirm('确认完成此订单？', '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    // 兼容不同的订单ID字段名
    const orderId = currentOrder.value.orderId || currentOrder.value.id
    
    if (!orderId) {
      console.error('❌ 订单ID不存在:', currentOrder.value)
      ElMessage.error('订单信息异常，请刷新页面')
      return
    }
    
    console.log('🏁 完成订单，订单ID:', orderId)
    
    const response = await fetch(`/api/orders/${orderId}/complete`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    
    if (response.ok) {
      ElMessage.success('订单已完成')
      
      // 更新统计数据
      completedOrders.value++
      todayEarnings.value += (currentOrder.value.estimatedFare || 0)
      
      // 清理订单和地图
      resetOrderState()
    } else {
      const errorText = await response.text()
      console.error('❌ 完成订单失败:', response.status, errorText)
      ElMessage.error('完成订单失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
    
    }
  }
}

// 判断司机是否可以取消订单
const canDriverCancelOrder = computed(() => {
  return currentOrder.value && 
         (currentOrder.value.status === 'ASSIGNED' || 
          currentOrder.value.status === 'PICKUP');
});

// 司机取消订单
const handleDriverCancelOrder = async () => {
  if (!currentOrder.value) return;
  
  try {
    await ElMessageBox.confirm(
      '确定要取消订单吗？取消后订单将重新分配给其他司机，您将无法再次接到此订单。',
      '确认取消',
      {
        confirmButtonText: '确定取消',
        cancelButtonText: '继续服务',
        type: 'warning',
      }
    );
    
    cancelLoading.value = true;
    
    // 兼容不同的订单ID字段名
    const orderId = currentOrder.value.orderId || currentOrder.value.id
    
    if (!orderId) {
      console.error('❌ 订单ID不存在:', currentOrder.value)
      ElMessage.error('订单信息异常，请刷新页面')
      cancelLoading.value = false
      return
    }
    
    console.log('🚫 司机取消订单，订单ID:', orderId)
    
    const response = await fetch(`/api/orders/${orderId}/cancel-by-driver?driverId=${userStore.user.driverId}`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    });
    
    const result = await response.json();
    
    if (response.ok && result.code === 200) {
      ElMessage.success('订单已取消，正在重新分配给其他司机');
      resetOrderState();
    } else {
      ElMessage.error('取消失败: ' + (result.message || '未知错误'));
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消订单错误:', error);
      ElMessage.error('取消失败，请重试');
    }
  } finally {
    cancelLoading.value = false;
  }
};

// 恢复订单导航状态（页面刷新后调用）
const restoreOrderNavigation = () => {
  if (!currentOrder.value) {
    console.log('❌ 没有当前订单，无需恢复导航')
    return
  }
  
  console.log('🔄 恢复订单导航状态...')
  console.log('订单状态:', currentOrder.value.status)
  console.log('订单信息:', currentOrder.value)
  
  // 等待地图初始化完成
  setTimeout(() => {
    if (!map) {
      console.error('❌ 地图未初始化，无法恢复导航')
      return
    }
    
    try {
      // 根据订单状态恢复相应的导航
      switch (currentOrder.value.status) {
        case 'ASSIGNED':
          console.log('🧭 恢复到上车点的导航')
          showRouteToPickup()
          break
        case 'PICKUP':
          console.log('🧭 恢复到上车点的导航（等待乘客上车）')
          showRouteToPickup()
          break
        case 'IN_PROGRESS':
          console.log('🧭 恢复到目的地的导航')
          showRouteToDestination()
          startRealTimeNavigation()
          break
        default:
          console.log('⚠️ 订单状态不需要导航:', currentOrder.value.status)
      }
      
      // 恢复订单相关的地图标记
      if (currentOrder.value.pickupLatitude && currentOrder.value.pickupLongitude) {
        // 添加上车点标记
        const pickupPos = [currentOrder.value.pickupLongitude, currentOrder.value.pickupLatitude]
        if (!pickupMarker) {
          pickupMarker = new window.AMap.Marker({
            position: pickupPos,
            map,
            icon: new window.AMap.Icon({
              size: new window.AMap.Size(32, 32),
              image: '📍'
            }),
            title: '上车点'
          })
        }
      }
      
      if (currentOrder.value.destinationLatitude && currentOrder.value.destinationLongitude) {
        // 添加目的地标记
        const destPos = [currentOrder.value.destinationLongitude, currentOrder.value.destinationLatitude]
        if (!destinationMarker) {
          destinationMarker = new window.AMap.Marker({
            position: destPos,
            map,
            icon: new window.AMap.Icon({
              size: new window.AMap.Size(32, 32),
              image: '🏁'
            }),
            title: '目的地'
          })
        }
      }
      
      console.log('✅ 订单导航状态恢复完成')
      
    } catch (error) {
      console.error('❌ 恢复导航状态失败:', error)
    }
  }, 2000) // 等待2秒确保地图完全初始化
}

// 重置订单状态
const resetOrderState = () => {
  console.log('🔄 重置订单状态...')
  
  // 使用store清除订单状态
  driverStore.setCurrentOrder(null)
  driverStore.clearOrderState() // 使用store的清理方法
  
  // 停止实时导航
  stopRealTimeNavigation()
  
  // 清理地图标记
  if (pickupMarker) {
    console.log('🗑️ 清理上车点标记')
    map.remove(pickupMarker)
    pickupMarker = null
  }
  if (destinationMarker) {
    console.log('🗑️ 清理目的地标记')
    map.remove(destinationMarker)
    destinationMarker = null
  }
  if (routeLine) {
    console.log('🗑️ 清理路线')
    map.remove(routeLine)
    routeLine = null
  }
  
  // 重置路线初始化标记
  window.routeInitialized = false
  
  console.log('✅ 订单状态重置完成')
}

// 停止导航
const stopNavigation = () => {
  navigationInfo.value = null
  if (routeLine) {
    map.remove(routeLine)
    routeLine = null
  }
}

// 加载今日统计
const loadTodayStats = async () => {
  try {
    const response = await fetch(`/api/drivers/${userStore.user.driverId}/today-stats`, {
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    
    if (response.ok) {
      const data = await response.json()
      driverStore.updateTodayEarnings(data.earnings || 0)
      driverStore.updateCompletedOrders(data.completedOrders || 0)
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// 获取订单状态类型
const getOrderStatusType = (status) => {
  const types = {
    'ASSIGNED': 'primary',
    'PICKUP': 'warning',
    'IN_PROGRESS': 'success'
  }
  return types[status] || 'info'
}

// 获取订单状态文本
const getOrderStatusText = (status) => {
  // 如果是预约单，显示特殊文本
  if (currentOrder.value && currentOrder.value.orderType === 'RESERVATION') {
    const texts = {
      'ASSIGNED': '预约单已接单，请于预约时间前到达',
      'PICKUP': '已到达上车点，等待乘客',
      'IN_PROGRESS': '预约行程进行中'
    }
    return texts[status] || status
  }
  
  // 实时单的状态文本
  const texts = {
    'ASSIGNED': '已接单',
    'PICKUP': '已到达',
    'IN_PROGRESS': '行程中'
  }
  return texts[status] || status
}

// 格式化预约时间
const formatScheduledTime = (scheduledTime) => {
  if (!scheduledTime) return '';
  
  const date = new Date(scheduledTime);
  const now = new Date();
  const isToday = date.toDateString() === now.toDateString();
  const isTomorrow = date.toDateString() === new Date(now.getTime() + 24 * 60 * 60 * 1000).toDateString();
  
  let dateStr;
  if (isToday) {
    dateStr = '今天';
  } else if (isTomorrow) {
    dateStr = '明天';
  } else {
    dateStr = `${date.getMonth() + 1}月${date.getDate()}日`;
  }
  
  const timeStr = `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
  
  return `${dateStr} ${timeStr}`;
};

// 播放通知音
const playNotificationSound = () => {
  try {
    const audioContext = new (window.AudioContext || window.webkitAudioContext)()
    const oscillator = audioContext.createOscillator()
    const gainNode = audioContext.createGain()
    
    oscillator.connect(gainNode)
    gainNode.connect(audioContext.destination)
    
    oscillator.frequency.value = 800
    oscillator.type = 'sine'
    
    gainNode.gain.setValueAtTime(0.3, audioContext.currentTime)
    gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.5)
    
    oscillator.start(audioContext.currentTime)
    oscillator.stop(audioContext.currentTime + 0.5)
  } catch (error) {
    console.log('无法播放提示音:', error)
  }
}
</script>

<style scoped>
.driver-map {
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
  padding: 15px 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.status-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.driver-status {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 500;
}

.driver-status.online {
  color: #28a745;
}

.status-indicator {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #dc3545;
}

.driver-status.online .status-indicator {
  background: #28a745;
}

.earnings-info {
  display: flex;
  gap: 20px;
  font-size: 14px;
  color: #666;
}

.map-container {
  width: 100vw;
  height: calc(100vh - 70px);
  margin-top: 70px;
  background: #f0f0f0;
  position: relative;
  z-index: 1;
}

.order-notification {
  position: fixed;
  top: 20px;
  right: 20px;
  background: white;
  border-radius: 15px;
  padding: 20px;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
  z-index: 200;
  width: 320px;
  animation: slideInRight 0.3s ease-out;
  border-left: 4px solid #409EFF;
}

@keyframes slideInRight {
  from {
    opacity: 0;
    transform: translateX(100%);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.notification-header h3 {
  margin: 0;
  color: #333;
  font-size: 18px;
}

.countdown {
  background: #ff4444;
  color: white;
  padding: 5px 10px;
  border-radius: 15px;
  font-weight: bold;
  font-size: 14px;
}

.order-details {
  margin-bottom: 20px;
}

.route-info {
  margin-bottom: 15px;
}

.location {
  display: flex;
  margin-bottom: 8px;
  font-size: 14px;
}

.location .label {
  width: 60px;
  color: #666;
  flex-shrink: 0;
}

.location .address {
  color: #333;
  font-weight: 500;
}

.order-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  background: #f8f9fa;
  border-radius: 8px;
  flex-wrap: wrap;
  gap: 10px;
}

.distance {
  color: #666;
}

.price {
  color: #28a745;
  font-weight: bold;
  font-size: 16px;
}

.scheduled-time {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 5px;
  padding-top: 8px;
  border-top: 1px solid #e9ecef;
}

.scheduled-time .label {
  color: #666;
  font-size: 12px;
}

.scheduled-time .time {
  color: #007bff;
  font-weight: bold;
  font-size: 14px;
}

.notification-actions {
  display: flex;
  gap: 10px;
}

.notification-actions .el-button {
  flex: 1;
}

.current-order-panel {
  position: absolute;
  bottom: 20px;
  left: 20px;
  right: 20px;
  background: white;
  border-radius: 15px;
  padding: 20px;
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.1);
  z-index: 100;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.order-header h3 {
  margin: 0;
  color: #333;
  font-size: 16px;
}

.passenger-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 15px;
  font-size: 14px;
  color: #666;
}

.scheduled-info {
  margin-bottom: 15px;
  padding: 10px;
  background: #e3f2fd;
  border-radius: 8px;
  border-left: 4px solid #2196f3;
}

.scheduled-time-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #1976d2;
  font-weight: 500;
}

.scheduled-time-info .icon {
  font-size: 16px;
}

.route-details {
  margin-bottom: 20px;
}

.route-item {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
  font-size: 14px;
}

.route-item .icon {
  width: 20px;
  text-align: center;
}

.order-actions {
  display: flex;
  gap: 10px;
}

.order-actions .el-button {
  flex: 1;
}

.navigation-panel {
  position: absolute;
  top: 80px;
  left: 20px;
  right: 20px;
  background: rgba(0, 0, 0, 0.8);
  color: white;
  border-radius: 10px;
  padding: 15px;
  z-index: 100;
}

.nav-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-weight: 500;
}

.nav-details {
  display: flex;
  gap: 20px;
  font-size: 14px;
  opacity: 0.9;
}

@media (max-width: 768px) {
  .order-notification {
    left: 10px;
    right: 10px;
    transform: translateY(-50%);
    min-width: auto;
  }
  
  .current-order-panel {
    left: 10px;
    right: 10px;
  }
  
  .navigation-panel {
    left: 10px;
    right: 10px;
  }
  
  .earnings-info {
    flex-direction: column;
    gap: 5px;
  }
}
</style>