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
            <div class="location-text">
              {{ pickupAddress || "正在定位..." }}
            </div>
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
              :disabled="currentOrder !== null"
              clearable
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 地图区域 -->
    <div id="mapContainer" class="map-container"></div>

    <!-- 底部车型选择面板 -->
    <div v-if="!currentOrder" class="bottom-panel">
      <!-- 订单类型选择 -->
      <div class="booking-type-selector">
        <el-radio-group v-model="bookingType" @change="handleBookingTypeChange">
          <el-radio-button label="immediate" :disabled="hasActiveOrder">立即叫车</el-radio-button>
          <el-radio-button label="scheduled" :disabled="hasActiveOrder">预约用车</el-radio-button>
        </el-radio-group>
      </div>

      <!-- 预约时间选择 -->
      <div v-if="bookingType === 'scheduled'" class="scheduled-time-selector">
        <div class="time-picker-container">
          <div class="time-display">
            <span v-if="scheduledTime">{{ formatDisplayTime(scheduledTime) }}</span>
            <span v-else class="placeholder">请选择预约时间</span>
          </div>
          <button type="button" class="time-picker-btn" @click="showTimePicker = true">
            选择时间
          </button>
        </div>

        <!-- 自定义滚轮时间选择器 -->
        <div v-if="showTimePicker" class="time-picker-overlay" @click="closeTimePicker">
          <div class="time-picker-modal" @click.stop>
            <div class="time-picker-header">
              <h3>选择预约时间</h3>
              <button class="close-btn" @click="closeTimePicker">×</button>
            </div>
            
            <div class="time-picker-wheels">
              <!-- 日期选择 -->
              <div class="wheel-container">
                <div class="wheel-label">日期</div>
                <div class="wheel" ref="dateWheel">
                  <div 
                    v-for="(date, index) in availableDates" 
                    :key="index"
                    :class="['wheel-item', { active: selectedDateIndex === index }]"
                    @click="selectDate(index)"
                  >
                    {{ date.label }}
                  </div>
                </div>
              </div>

              <!-- 小时选择 -->
              <div class="wheel-container">
                <div class="wheel-label">小时</div>
                <div class="wheel" ref="hourWheel">
                  <div 
                    v-for="hour in availableHours" 
                    :key="hour"
                    :class="['wheel-item', { active: selectedHour === hour }]"
                    @click="selectHour(hour)"
                  >
                    {{ String(hour).padStart(2, '0') }}
                  </div>
                </div>
              </div>

              <!-- 分钟选择 -->
              <div class="wheel-container">
                <div class="wheel-label">分钟</div>
                <div class="wheel" ref="minuteWheel">
                  <div 
                    v-for="minute in availableMinutes" 
                    :key="minute"
                    :class="['wheel-item', { active: selectedMinute === minute }]"
                    @click="selectMinute(minute)"
                  >
                    {{ String(minute).padStart(2, '0') }}
                  </div>
                </div>
              </div>
            </div>

            <div class="time-picker-footer">
              <button class="cancel-btn" @click="closeTimePicker">取消</button>
              <button class="confirm-btn" @click="confirmTime">确定</button>
            </div>
          </div>
        </div>
      </div>

      <div class="route-info" v-if="routeInfo">
        <div class="route-details">
          <span class="distance"
            >{{ (routeInfo.distance / 1000).toFixed(1) }}km</span
          >
          <span class="duration"
            >约{{ Math.ceil(routeInfo.duration / 60) }}分钟</span
          >
        </div>
      </div>

      <div class="car-types">
        <div
          class="car-type-item"
          :class="{
            active: selectedCarType === 'economy',
            disabled: currentOrder !== null || hasActiveOrder,
          }"
          @click="(currentOrder || hasActiveOrder) ? null : selectCarType('economy')"
        >
          <div class="car-icon">🚗</div>
          <div class="car-info">
            <div class="car-name">快车</div>
            <div class="car-price">¥{{ getPrice("economy") }}</div>
          </div>
        </div>
      </div>

      <el-button
        type="primary"
        class="call-car-btn"
        :disabled="!canOrder || currentOrder !== null || hasUnpaidOrders || hasActiveOrder || (bookingType === 'scheduled' && !scheduledTime)"
        @click="hasUnpaidOrders ? goToMyTrips() : handleCallCar()"
        size="large"
      >
        {{ callCarText }}
      </el-button>
    </div>

    <!-- 紧凑的司机信息条 -->
    <div 
      v-if="currentOrder && driverInfo && (orderStatus === 'ASSIGNED' || orderStatus === 'PICKUP' || orderStatus === 'IN_PROGRESS')" 
      class="driver-info-bar"
      @click="toggleDriverDetails"
    >
      <div class="driver-bar-content">
        <div class="driver-avatar-small">
          <img 
            :src="buildAvatarUrl(driverInfo.avatar) || getDefaultAvatar(driverInfo.name)" 
            :alt="driverInfo.name"
            @error="handleAvatarError"
          />
        </div>
        <div class="driver-basic-info">
          <div class="driver-name-small">{{ driverInfo.name }}</div>
          <div class="vehicle-brief">{{ driverInfo.plateNumber || '暂无车牌' }} · {{ driverInfo.vehicleBrand || '未知' }} {{ driverInfo.vehicleModel || '' }}</div>
        </div>
        <div class="driver-rating-small" v-if="driverInfo.rating">
          <span class="rating-score-small">{{ driverInfo.rating.toFixed(1) }}</span>
          <span class="star-small">★</span>
        </div>
        <div class="expand-icon">
          <el-icon><ArrowDown v-if="!showDriverDetails" /><ArrowUp v-else /></el-icon>
        </div>
      </div>
    </div>

    <!-- 订单状态面板 -->
    <div v-if="currentOrder" class="order-panel">
      <div class="order-status">
        <div class="status-text">{{ orderStore.getStatusText() }}</div>

        <!-- 取消订单按钮 -->
        <div v-if="canCancelOrder" class="cancel-order-section">
          <el-button
            type="danger"
            @click="handleCancelOrder"
            :loading="cancelLoading"
            size="large"
            plain
          >
            取消订单
          </el-button>
        </div>
      </div>
    </div>

    <!-- 可滚动的司机详细信息面板 -->
    <div 
      v-if="currentOrder && driverInfo && showDriverDetails && (orderStatus === 'ASSIGNED' || orderStatus === 'PICKUP' || orderStatus === 'IN_PROGRESS')" 
      class="driver-details-panel"
    >
      <div class="panel-header">
        <h3>司机信息</h3>
        <el-button text @click="toggleDriverDetails">
          <el-icon><Close /></el-icon>
        </el-button>
      </div>
      
      <div class="panel-content">
        <div class="driver-profile">
          <div class="driver-avatar-large">
            <img 
              :src="buildAvatarUrl(driverInfo.avatar) || getDefaultAvatar(driverInfo.name)" 
              :alt="driverInfo.name"
              @error="handleAvatarError"
            />
          </div>
          
          <div class="driver-info">
            <div class="driver-name-large">{{ driverInfo.name }}</div>
            <div class="driver-rating-large" v-if="driverInfo.rating">
              <span class="rating-stars">
                <span v-for="i in 5" :key="i" class="star" :class="{ filled: i <= Math.floor(driverInfo.rating) }">★</span>
              </span>
              <span class="rating-score">{{ driverInfo.rating.toFixed(1) }}分</span>
            </div>
            <div class="driver-phone">{{ driverInfo.phone }}</div>
          </div>
        </div>

        <div class="vehicle-info-section">
          <h4>车辆信息</h4>
          <div class="vehicle-details-grid">
            <div class="detail-item">
              <span class="label">车牌号</span>
              <span class="value plate-number-compact">{{ driverInfo.plateNumber || '暂无' }}</span>
            </div>
            <div class="detail-item">
              <span class="label">车型</span>
              <span class="value">{{ driverInfo.vehicleBrand || '未知' }} {{ driverInfo.vehicleModel || '' }}</span>
            </div>
            <div class="detail-item">
              <span class="label">颜色</span>
              <span class="value">{{ driverInfo.vehicleColor || '未知' }}</span>
            </div>
            <div class="detail-item">
              <span class="label">类型</span>
              <span class="value">{{ getVehicleTypeText(driverInfo.vehicleType) }}</span>
            </div>
          </div>
        </div>

        <div class="action-buttons">
          <el-button 
            type="primary" 
            @click="callDriver"
            :icon="Phone"
            size="large"
            style="width: 100%;"
          >
            拨打司机电话
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, onUnmounted, onActivated, onDeactivated, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Location, Phone, ArrowDown, ArrowUp, Close } from "@element-plus/icons-vue";
import { useUserStore } from "@/stores/user";
import { useOrderStore } from "@/stores/order";
import {
  mapConfig,
  getMapApiUrl,
  getRestApiUrl,
  getSecurityConfig,
} from "@/config/map";
// WebSocket现在由全局store管理，不需要在组件中导入

const userStore = useUserStore();
const orderStore = useOrderStore();

// 地图相关变量
let map = null;
let pickupMarker = null;
let destMarker = null;
let routeLine = null;
let autocomplete = null;
let currentDriving = null;

// 响应式数据
const currentPosition = ref({ lng: 0, lat: 0 });
const pickupAddress = ref("");
const destination = ref(null);
const destinationKeyword = ref("");
const routeInfo = ref(null);
const selectedCarType = ref("economy");
const canOrder = ref(false);
const isCalling = ref(false);

// 预约单相关数据
const bookingType = ref("immediate"); // "immediate" | "scheduled"
const scheduledTime = ref(null);
const hasActiveOrder = ref(false);

// 时间选择器相关数据
const showTimePicker = ref(false);
const selectedDateIndex = ref(0);
const selectedHour = ref(null);
const selectedMinute = ref(null);
const availableDates = ref([]);
const availableHours = ref([]);
const availableMinutes = ref([]);

// 订单状态相关（使用全局store）
const cancelLoading = ref(false);
const showPaymentDialog = ref(false);
const completedOrder = ref(null);
const selectedPaymentMethod = ref('');

// 司机详情面板控制
const showDriverDetails = ref(false);

// 从store获取订单状态
const currentOrder = computed(() => orderStore.currentOrder);
const driverInfo = computed(() => orderStore.driverInfo);
const orderStatus = computed(() => orderStore.orderStatus);
const hasUnpaidOrders = computed(() => orderStore.hasUnpaidOrders);
const canCancelOrder = computed(() => orderStore.canCancelOrder);

// 监听订单状态变化，自动更新拖拽状态
watch([currentOrder, orderStatus], () => {
  console.log('📊 订单状态变化，更新拖拽状态');
  setTimeout(() => {
    updatePickupMarkerDraggable();
  }, 100);
}, { immediate: false });

// WebSocket现在由全局store管理

// 司机位置相关变量
let driverMarker = null;

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
  isOutline: false, // 不返回路线轮廓
});

onUnmounted(() => {
  stopDriverTracking();
  
  // 清理全局函数
  if (window.handleMapOrderUpdate) {
    delete window.handleMapOrderUpdate;
    console.log("✅ 已清理全局地图消息处理函数");
  }
});

// 车型价格配置
const carTypes = {
  economy: { basePrice: 10, perKm: 2.5, name: "快车" },
  comfort: { basePrice: 15, perKm: 3.5, name: "专车" },
  luxury: { basePrice: 25, perKm: 5.0, name: "豪华车" },
};

// 计算价格
const getPrice = (type) => {
  if (!routeInfo.value) return "--";
  const config = carTypes[type];
  const distance = routeInfo.value.distance / 1000;
  const price = config.basePrice + distance * config.perKm;
  return Math.round(price);
};

// 叫车按钮文本
const callCarText = computed(() => {
  if (hasUnpaidOrders.value) return "请先完成支付";
  if (currentOrder.value) return "订单进行中";
  if (hasActiveOrder.value) return "您已有进行中的订单";
  if (isCalling.value) {
    return bookingType.value === "scheduled" ? "正在创建预约单..." : "正在叫车...";
  }
  if (!canOrder.value) return "请选择目的地";
  if (bookingType.value === "scheduled" && !scheduledTime.value) return "请选择预约时间";
  return bookingType.value === "scheduled" ? "创建预约单" : "立即叫车";
});

// getStatusText 方法已移至 orderStore 中统一管理



// 取消订单
const handleCancelOrder = async () => {
  if (!currentOrder.value) return;

  try {
    await ElMessageBox.confirm(
      "确定要取消订单吗？取消后将无法恢复。",
      "确认取消",
      {
        confirmButtonText: "确定取消",
        cancelButtonText: "继续等待",
        type: "warning",
      }
    );

    cancelLoading.value = true;

    console.log("🚫 准备取消订单:", currentOrder.value);
    console.log("🆔 订单ID:", currentOrder.value.id);
    console.log("📋 订单类型:", currentOrder.value.orderType);
    console.log("👨‍✈️ 司机ID:", currentOrder.value.driverId);
    
    // 根据订单类型选择不同的取消接口
    const cancelUrl = currentOrder.value.orderType === "RESERVATION" 
      ? `/api/orders/${currentOrder.value.id}/cancel-scheduled`
      : `/api/orders/${currentOrder.value.id}/cancel`;
    
    console.log("📞 请求URL:", cancelUrl);

    const response = await fetch(cancelUrl, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${userStore.token}`,
      },
    });

    const result = await response.json();

    console.log("📋 取消订单响应:", response.status, result);

    if (response.ok && result.code === 200) {
      console.log("✅ 订单取消成功，后端已通知司机");
      
      // 立即清理乘客端的订单状态和地图元素
      resetOrderState();
      
      // 清理地图上的司机相关元素
      clearAllDriverRoutes();
      
      // 恢复乘客路径为蓝色
      if (routeLine && routeLine.setOptions) {
        routeLine.setOptions({
          strokeColor: '#1890ff',
          strokeWeight: 6,
          strokeOpacity: 0.8
        });
      }
      
      ElMessage.success("订单已取消，司机已收到通知");
      
      // 刷新活跃订单状态，确保UI正确更新
      await checkActiveOrder();
    } else {
      console.error("❌ 取消订单失败:", result);
      ElMessage.error("取消失败: " + (result.message || "未知错误"));
    }
  } catch (error) {
    if (error !== "cancel") {
      console.error("取消订单错误:", error);
      ElMessage.error("取消失败，请重试");
    }
  } finally {
    cancelLoading.value = false;
  }
};

// 检查乘客是否有活跃订单
const checkActiveOrder = async () => {
  try {
    const passengerId = userStore.user?.passengerId || userStore.user?.id;
    if (!passengerId) return;

    const response = await fetch(`/api/orders/passenger/${passengerId}/has-active`, {
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    });

    if (response.ok) {
      const result = await response.json();
      hasActiveOrder.value = result.data || false;
    }
  } catch (error) {
    console.error('检查活跃订单失败:', error);
  }
};

// 处理订单类型切换
const handleBookingTypeChange = (type) => {
  console.log('订单类型切换:', type);
  if (type === 'scheduled') {
    scheduledTime.value = null;
    initializeTimePicker();
  }
};

// 初始化时间选择器
const initializeTimePicker = () => {
  const now = new Date();
  
  // 生成可选日期（今天和未来6天）
  availableDates.value = [];
  for (let i = 0; i < 7; i++) {
    const date = new Date(now);
    date.setDate(date.getDate() + i);
    
    let label;
    if (i === 0) {
      label = '今天';
    } else if (i === 1) {
      label = '明天';
    } else {
      label = `${date.getMonth() + 1}月${date.getDate()}日`;
    }
    
    availableDates.value.push({
      date: date,
      label: label,
      isToday: i === 0
    });
  }
  
  // 默认选择今天
  selectedDateIndex.value = 0;
  updateAvailableHours();
};

// 更新可选小时
const updateAvailableHours = () => {
  const selectedDate = availableDates.value[selectedDateIndex.value];
  const now = new Date();
  
  availableHours.value = [];
  
  if (selectedDate.isToday) {
    // 今天：从当前时间+30分钟后开始
    const minTime = new Date(now.getTime() + 30 * 60 * 1000);
    const startHour = minTime.getHours();
    
    for (let hour = startHour; hour <= 23; hour++) {
      availableHours.value.push(hour);
    }
    
    // 默认选择第一个可用小时
    if (availableHours.value.length > 0) {
      selectedHour.value = availableHours.value[0];
    }
  } else {
    // 其他日期：全天可选
    for (let hour = 0; hour <= 23; hour++) {
      availableHours.value.push(hour);
    }
    
    // 默认选择9点
    selectedHour.value = 9;
  }
  
  updateAvailableMinutes();
};

// 更新可选分钟
const updateAvailableMinutes = () => {
  const selectedDate = availableDates.value[selectedDateIndex.value];
  const now = new Date();
  
  availableMinutes.value = [];
  
  if (selectedDate.isToday && selectedHour.value === now.getHours()) {
    // 今天的当前小时：从当前分钟+30分钟后开始
    const minTime = new Date(now.getTime() + 30 * 60 * 1000);
    const startMinute = Math.ceil(minTime.getMinutes() / 15) * 15; // 向上取整到15分钟倍数
    
    for (let minute = startMinute; minute <= 45; minute += 15) {
      availableMinutes.value.push(minute);
    }
  } else {
    // 其他情况：每15分钟一个选项
    for (let minute = 0; minute <= 45; minute += 15) {
      availableMinutes.value.push(minute);
    }
  }
  
  // 默认选择第一个可用分钟
  if (availableMinutes.value.length > 0) {
    selectedMinute.value = availableMinutes.value[0];
  } else {
    selectedMinute.value = 0;
  }
};

// 选择日期
const selectDate = (index) => {
  selectedDateIndex.value = index;
  updateAvailableHours();
};

// 选择小时
const selectHour = (hour) => {
  selectedHour.value = hour;
  updateAvailableMinutes();
};

// 选择分钟
const selectMinute = (minute) => {
  selectedMinute.value = minute;
};

// 确认时间选择
const confirmTime = () => {
  if (selectedHour.value === null || selectedMinute.value === null) {
    ElMessage.warning('请选择完整的时间');
    return;
  }
  
  const selectedDate = availableDates.value[selectedDateIndex.value];
  const dateTime = new Date(selectedDate.date);
  dateTime.setHours(selectedHour.value, selectedMinute.value, 0, 0);
  
  scheduledTime.value = dateTime;
  showTimePicker.value = false;
};

// 关闭时间选择器
const closeTimePicker = () => {
  showTimePicker.value = false;
};

// 格式化显示时间
const formatDisplayTime = (date) => {
  if (!date) return '';
  
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

// 格式化时间给后端
const formatDateTimeForBackend = (date) => {
  if (!date) return '';
  
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  const seconds = String(date.getSeconds()).padStart(2, '0');
  
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
};

// 旧的时间验证方法已移除，使用新的滚轮选择器
// 初始化地图
let isPassengerMapInitialized = false

// 统一的初始化函数
const initializePassengerMap = async (isReactivation = false) => {
  const logPrefix = isReactivation ? '🔄 重新激活' : '🚀 首次初始化'
  console.log(`${logPrefix}乘客地图页面...`)

  // 立即注册全局函数，让store能够通知地图组件
  window.handleMapOrderUpdate = handleOrderUpdate
  console.log("✅ 已注册全局地图消息处理函数")

  // 如果是重新激活且已经初始化过，只需要重新检查状态
  if (isReactivation && isPassengerMapInitialized) {
    console.log('🔄 页面重新激活，检查订单状态...')
    
    // 确保用户信息完整性
    try {
      await userStore.ensureUserInfo()
    } catch (error) {
      console.error('❌ 用户状态检查失败:', error.message)
      ElMessage.error(error.message)
      return
    }
    
    // 重新检查活跃订单
    await checkActiveOrder()
    
    // 恢复地图标记（修复页面切换后标记消失的问题）
    setTimeout(() => {
      restoreMapMarkers()
    }, 500)
    
    // 重新连接WebSocket（如果需要）
    if (!orderStore.isWebSocketConnected) {
      console.log('🔄 重新连接WebSocket...')
      await orderStore.connectWebSocket()
    }
    
    return
  }

  // 完整初始化流程
  // 初始化时间选择器
  initializeTimePicker()

  // 初始化订单状态（包括检查未支付订单和当前订单）
  console.log("🔄 开始初始化订单状态...")
  await orderStore.initOrderState()
  console.log("✅ 订单状态初始化完成")
  
  // 检查是否有活跃订单
  await checkActiveOrder()

  // 延迟初始化地图，确保DOM完全加载
  setTimeout(() => {
    console.log("🗺️ 开始初始化地图...")
    if (window.AMap) {
      console.log("高德地图已加载，直接初始化")
      initMap()
    } else {
      console.log("开始加载高德地图API...")

      window._AMapSecurityConfig = getSecurityConfig()

      const script = document.createElement("script")
      script.src = getMapApiUrl()
      script.onload = () => {
        console.log("高德地图API加载成功")
        setTimeout(initMap, 200)
      }
      script.onerror = (error) => {
        console.error("高德地图API加载失败:", error)
        ElMessage.error("地图加载失败，请检查网络连接")
      }
      document.head.appendChild(script)
    }
  }, 500)
  
  isPassengerMapInitialized = true
}

onMounted(async () => {
  await initializePassengerMap(false)
})

// 页面激活时（从其他页面切换回来）
onActivated(async () => {
  console.log('📱 乘客地图页面被激活')
  await initializePassengerMap(true)
})

// 页面失活时（切换到其他页面）
onDeactivated(() => {
  console.log('📱 乘客地图页面失活')
  // 不清理状态，保持连接
})

function initMap() {
  console.log("开始创建地图实例...");

  try {
    const container = document.getElementById("mapContainer");
    if (!container) {
      console.error("地图容器不存在");
      return;
    }

    container.style.minHeight = "400px";
    container.style.backgroundColor = "#f0f0f0";

    map = new window.AMap.Map("mapContainer", {
      resizeEnable: true,
      zoom: 15,
      center: [116.397428, 39.90923],
      mapStyle: "amap://styles/normal",
      viewMode: "2D",
    });

    console.log("地图实例创建成功");

    map.on("complete", function () {
      console.log("地图加载完成");
    });

    map.on("error", function (error) {
      console.error("地图错误:", error);
    });

    // 定位
    window.AMap.plugin(["AMap.Geolocation", "AMap.Autocomplete"], function () {
      console.log("开始定位...");
      const geolocation = new window.AMap.Geolocation({
        enableHighAccuracy: true,
        timeout: 10000,
      });
      map.addControl(geolocation);
      geolocation.getCurrentPosition((status, result) => {
        console.log("定位结果:", status, result);
        if (status === "complete") {
          const { lng, lat } = result.position;
          currentPosition.value = { lng, lat };

          // 添加上车点标记
          pickupMarker = new window.AMap.Marker({
            position: [lng, lat],
            map,
            draggable: true,
            cursor: "move",
            icon: new window.AMap.Icon({
              size: new window.AMap.Size(26, 13),
              image: "https://webapi.amap.com/images/car.png",
              imageSize: new window.AMap.Size(26, 13)
            }),
            offset: new AMap.Pixel(0, 0), // 相对于基点的偏移位置
            title: "拖拽调整上车位置",
          });

          // 延迟更新拖拽状态，确保地图完全初始化
          setTimeout(() => {
            updatePickupMarkerDraggable();
          }, 500);
          
          // 也在地图完全加载后再次更新
          setTimeout(() => {
            updatePickupMarkerDraggable();
          }, 1000);

          // 添加拖拽事件监听器（智能吸附版本）
          pickupMarker.on("dragend", async (e) => {
            // 如果已有订单，不允许拖拽
            if (currentOrder.value) {
              console.log("⚠️ 订单已发起，不允许修改上车点");
              // 恢复到原位置
              pickupMarker.setPosition([
                currentPosition.value.lng,
                currentPosition.value.lat,
              ]);
              return;
            }

            const dragPosition = e.lnglat;
            console.log(
              "🚩 上车点被拖拽到位置:",
              dragPosition.lng,
              dragPosition.lat
            );

            // 智能吸附到最近的POI
            await snapToNearestPOI(dragPosition.lng, dragPosition.lat);
          });

          map.setCenter([lng, lat]);
          console.log("定位成功，当前位置:", lng, lat);

          // 初始定位时也使用智能吸附功能
          snapToNearestPOI(lng, lat);
          initAutocomplete();
          
          // 🔑 关键：恢复订单相关的地图元素
          restoreOrderMapElements();
        } else {
          console.error("定位失败:", status);
          ElMessage.error("定位失败，请检查浏览器权限");
          pickupAddress.value = "定位失败";
          initAutocomplete();
          
          // 即使定位失败，也尝试恢复订单地图元素
          restoreOrderMapElements();
        }
      });
    });
  } catch (error) {
    console.error("地图初始化失败:", error);
    ElMessage.error("地图初始化失败: " + error.message);
  }
}

// 初始化Autocomplete
const initAutocomplete = () => {
  try {
    console.log("开始初始化Autocomplete...");

    if (!window.AMap.Autocomplete) {
      console.error("AMap.Autocomplete插件未加载");
      return;
    }

    const inputElement = document.getElementById("destinationInput");
    if (!inputElement) {
      console.error("目的地输入框不存在");
      return;
    }

    autocomplete = new window.AMap.Autocomplete({
      input: "destinationInput",
      city: "全国",
      citylimit: false,
      extensions: "all",
    });

    console.log("Autocomplete实例创建成功");

    autocomplete.on("select", (e) => {
      console.log("Autocomplete选择结果:", e);

      if (e.poi) {
        const poi = e.poi;
        const destinationItem = {
          name: poi.name,
          address: poi.address || poi.district + poi.adcode,
          location: poi.location,
          distance: poi.distance ? poi.distance + "m" : "",
          type: poi.type,
          tel: poi.tel || "",
          rating: poi.biz_ext?.rating || "",
          source: "autocomplete",
          city: poi.cityname || poi.pname || "",
          province: poi.pname || "",
        };

        console.log("Autocomplete选择的目的地:", destinationItem);
        selectDestination(destinationItem);
      }
    });

    console.log("Autocomplete初始化完成");
  } catch (error) {
    console.error("Autocomplete初始化失败:", error);
    ElMessage.error("自动完成功能初始化失败");
  }
};

// 根据坐标获取地址
const getAddressFromLocation = async (lng, lat) => {
  try {
    console.log("开始获取地址:", lng, lat);

    // 使用Web端JS API的逆地理编码功能
    if (!window.AMap) {
      console.error("❌ 高德地图API未加载");
      pickupAddress.value = `位置 (${lat.toFixed(6)}, ${lng.toFixed(6)})`;
      return;
    }

    // 创建逆地理编码实例
    const geocoder = new window.AMap.Geocoder({
      radius: 100, // 搜索半径100米
      extensions: "base" // 返回基础信息即可
    });

    // 执行逆地理编码
    geocoder.getAddress([lng, lat], (status, result) => {
      console.log("地址解析结果:", status, result);

      if (status === 'complete' && result.info === 'OK' && result.regeocode) {
        const address = result.regeocode.formattedAddress;
        pickupAddress.value = address;
        console.log("获取到地址:", address);
        
        // 更新标记标题
        if (pickupMarker) {
          pickupMarker.setTitle(`上车点: ${address}`);
        }
      } else {
        console.log("地址获取失败，使用备用方案");
        pickupAddress.value = `位置 (${lat.toFixed(6)}, ${lng.toFixed(6)})`;
      }
    });

  } catch (error) {
    console.error("地址获取异常:", error);
    pickupAddress.value = `位置 (${lat.toFixed(6)}, ${lng.toFixed(6)})`;
  }
};

// 智能吸附到最近的POI（使用Web端JS API）
const snapToNearestPOI = async (lng, lat) => {
  try {
    console.log("🧲 开始智能吸附，查找最近的POI...");
    ElMessage.info("正在智能匹配最近地点...");

    // 使用Web端JS API的逆地理编码功能
    if (!window.AMap) {
      console.error("❌ 高德地图API未加载");
      fallbackToOriginalPosition(lng, lat);
      return;
    }

    // 创建逆地理编码实例
    const geocoder = new window.AMap.Geocoder({
      radius: 200, // 搜索半径200米
      extensions: "all" // 返回详细信息
    });

    // 执行逆地理编码
    geocoder.getAddress([lng, lat], (status, result) => {
      console.log("🔍 POI搜索结果:", status, result);

      if (status === 'complete' && result.info === 'OK' && result.regeocode) {
        const regeocode = result.regeocode;
        
        // 检查是否有POI信息
        if (regeocode.pois && regeocode.pois.length > 0) {
          // 找到最近的POI
          const nearestPoi = regeocode.pois[0];
          const poiDistance = parseFloat(nearestPoi.distance);
          
          console.log("📍 找到最近POI:", nearestPoi.name, "距离:", poiDistance + "米");
          
          // 如果POI距离小于100米，则吸附到该POI
          if (poiDistance < 100) {
            const poiLocation = nearestPoi.location;
            const poiLng = poiLocation.lng;
            const poiLat = poiLocation.lat;
            
            console.log("🎯 吸附到POI:", nearestPoi.name, "坐标:", poiLng, poiLat);
            
            // 更新位置到POI的精确坐标
            currentPosition.value = { lng: poiLng, lat: poiLat };
            
            // 移动地图中心到POI位置
            if (map) {
              map.setCenter([poiLng, poiLat]);
              console.log("📍 地图中心已移动到POI位置");
            }
            
            // 移动标记到POI位置
            if (pickupMarker) {
              pickupMarker.setPosition([poiLng, poiLat]);
              pickupMarker.setTitle(`上车点: ${nearestPoi.name}`);
            }
            
            // 更新地址显示为POI名称
            pickupAddress.value = nearestPoi.name;
            
            // 显示吸附成功消息
            ElMessage.success(`已自动匹配到: ${nearestPoi.name}`);
            
            // 如果有目的地，重新规划路线
            if (destination.value) {
              showRoute();
            }
            
            return; // 成功吸附，直接返回
          }
        }
        
        // 如果没有找到合适的POI，使用格式化地址
        console.log("📍 未找到合适的POI，使用格式化地址");
        currentPosition.value = { lng, lat };
        
        if (pickupMarker) {
          pickupMarker.setPosition([lng, lat]);
        }
        
        // 使用逆地理编码的格式化地址
        pickupAddress.value = regeocode.formattedAddress || `位置 (${lat.toFixed(6)}, ${lng.toFixed(6)})`;
        
        // 如果有目的地，重新规划路线
        if (destination.value) {
          showRoute();
        }
        
        ElMessage.info("已更新到拖拽位置");
        
      } else {
        console.error("❌ 逆地理编码失败:", status, result);
        fallbackToOriginalPosition(lng, lat);
      }
    });
    
  } catch (error) {
    console.error("❌ 智能吸附失败:", error);
    fallbackToOriginalPosition(lng, lat);
  }
};

// 回退到原始位置的处理函数
const fallbackToOriginalPosition = (lng, lat) => {
  console.log("📍 回退到原始位置处理");
  
  currentPosition.value = { lng, lat };
  
  if (pickupMarker) {
    pickupMarker.setPosition([lng, lat]);
  }
  
  // 使用简单的地址格式
  pickupAddress.value = `位置 (${lat.toFixed(6)}, ${lng.toFixed(6)})`;
  
  if (destination.value) {
    showRoute();
  }
  
  ElMessage.warning("智能匹配失败，已使用拖拽位置");
};

// 处理起点点击事件
const handlePickupClick = () => {
  if (currentOrder.value) {
    console.log("⚠️ 订单已发起，不允许修改上车点");
    return;
  }
  
  console.log("📍 重新定位上车点");
  ElMessage.info("正在重新定位...");
  
  // 重新获取当前位置
  if (map && window.AMap) {
    const geolocation = new window.AMap.Geolocation({
      enableHighAccuracy: true,
      timeout: 10000,
    });
    
    geolocation.getCurrentPosition(async (status, result) => {
      if (status === "complete") {
        const { lng, lat } = result.position;
        console.log("🎯 重新定位成功:", lng, lat);
        
        // 使用智能吸附功能
        await snapToNearestPOI(lng, lat);
        
        ElMessage.success("定位更新成功");
      } else {
        console.error("❌ 重新定位失败:", status);
        ElMessage.error("定位失败，请检查位置权限");
      }
    });
  }
};

// 目的地输入
const handleDestinationInput = async () => {
  // 如果已有订单，不允许修改目的地
  if (currentOrder.value) {
    return;
  }

  if (!destinationKeyword.value) {
    return;
  }
  console.log("使用Autocomplete搜索:", destinationKeyword.value);
};

// 目的地输入框聚焦
const handleDestinationFocus = () => {
  // 如果已有订单，不允许修改目的地
  if (currentOrder.value) {
    return;
  }
  console.log("目的地输入框聚焦");
};

// 选择车型
const selectCarType = (type) => {
  // 如果已有订单，不允许修改车型
  if (currentOrder.value) {
    return;
  }
  selectedCarType.value = type;
};

// 选择目的地
const selectDestination = (item) => {
  // 如果已有订单，不允许修改目的地
  if (currentOrder.value) {
    console.log("⚠️ 订单已发起，不允许修改目的地");
    return;
  }

  console.log("选择目的地:", item);

  destination.value = item;
  destinationKeyword.value = item.name;

  if (destMarker) map.remove(destMarker);
  if (routeLine) {
    map.remove(routeLine);
    routeLine = null;
  }

  canOrder.value = false;
  routeInfo.value = null;

  let lng, lat;
  if (typeof item.location === "string") {
    [lng, lat] = item.location.split(",").map(Number);
  } else if (item.location && item.location.lng && item.location.lat) {
    lng = item.location.lng;
    lat = item.location.lat;
  } else {
    console.error("无法解析目的地坐标:", item);
    return;
  }

  console.log("目的地坐标:", lng, lat);

  destMarker = new window.AMap.Marker({
    position: [lng, lat],
    map,
    icon: new window.AMap.Icon({
      size: new window.AMap.Size(32, 32),
      image:
        "data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzIiIGhlaWdodD0iMzIiIHZpZXdCb3g9IjAgMCAzMiAzMiIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPGNpcmNsZSBjeD0iMTYiIGN5PSIxNiIgcj0iMTIiIGZpbGw9IiNGRjQ0NDQiLz4KPGNpcmNsZSBjeD0iMTYiIGN5PSIxNiIgcj0iNiIgZmlsbD0id2hpdGUiLz4KPC9zdmc+",
    }),
  });

  if (pickupMarker && destMarker) {
    map.setFitView([pickupMarker, destMarker], false, [50, 50, 50, 50]);
  }

  showRoute();
};

// 停止追踪司机位置（已移除轮询逻辑，司机位置通过WebSocket推送）
const stopDriverTracking = () => {
  console.log("⏹️ 已停止追踪司机位置");
};

// 更新上车点标记的可拖拽状态
const updatePickupMarkerDraggable = () => {
  if (pickupMarker) {
    // 只有在订单真正进行中时才禁用拖拽（司机已接单或更进一步的状态）
    const isOrderInProgress = currentOrder.value && 
                             ['ASSIGNED', 'PICKUP', 'IN_PROGRESS'].includes(orderStatus.value);
    const isDraggable = !isOrderInProgress;
    
    console.log('🔧 更新上车点拖拽状态:', {
      hasOrder: !!currentOrder.value,
      orderStatus: orderStatus.value,
      isOrderInProgress: isOrderInProgress,
      isDraggable: isDraggable,
      markerExists: !!pickupMarker
    });
    
    try {
      pickupMarker.setDraggable(isDraggable);
      pickupMarker.setCursor(isDraggable ? "move" : "default");
      pickupMarker.setTitle(isDraggable ? "拖拽调整上车位置" : "上车点");
      
      console.log('✅ 上车点拖拽状态更新成功:', isDraggable ? '可拖拽' : '不可拖拽');
    } catch (error) {
      console.error('❌ 更新上车点拖拽状态失败:', error);
    }
  } else {
    console.warn('⚠️ 上车点标记不存在，无法更新拖拽状态');
  }
};

// 路径规划
const showRoute = async () => {
  if (!destination.value) return;

  console.log("开始路径规划...");

  let destLng, destLat;
  if (typeof destination.value.location === "string") {
    [destLng, destLat] = destination.value.location.split(",").map(Number);
  } else if (
    destination.value.location &&
    destination.value.location.lng &&
    destination.value.location.lat
  ) {
    destLng = destination.value.location.lng;
    destLat = destination.value.location.lat;
  } else {
    console.error("无法解析目的地坐标");
    return;
  }

  try {
    if (routeLine) {
      map.remove(routeLine);
      routeLine = null;
    }

    if (currentDriving) {
      currentDriving.clear();
      currentDriving = null;
    }

    console.log("已清除旧路径，开始新的路径规划");

    currentDriving = new window.AMap.Driving(getDrivingConfig());
    currentDriving.search(
      new window.AMap.LngLat(
        currentPosition.value.lng,
        currentPosition.value.lat
      ),
      new window.AMap.LngLat(destLng, destLat),
      (status, result) => {
        console.log("🗺️ 乘客端路线规划结果:", status, result);
        console.log(
          "🚗 使用的路径规划策略:",
          window.AMap.DrivingPolicy.LEAST_DISTANCE
        );
        console.log("⛴️ 轮渡设置:", 0);

        if (
          status === "complete" &&
          result.routes &&
          result.routes.length > 0
        ) {
          const route = result.routes[0];

          routeInfo.value = {
            distance: route.distance,
            duration: route.time,
          };
          canOrder.value = true;

          console.log("路线规划成功:", {
            distance: route.distance + "m",
            duration: Math.round(route.time / 60) + "分钟",
          });

          const path = route.steps.reduce((points, step) => {
            return points.concat(step.path);
          }, []);

          if (path.length > 0) {
            routeLine = new window.AMap.Polyline({
              path: path,
              strokeColor: "#409EFF",
              strokeWeight: 6,
              strokeOpacity: 0.8,
              strokeStyle: "solid",
              lineJoin: "round",
              lineCap: "round",
            });
            map.add(routeLine);

            // 安全地调整地图视野
            // const elements = [];
            // if (pickupMarker) elements.push(pickupMarker);
            // if (destMarker) elements.push(destMarker);
            // if (routeLine) elements.push(routeLine);
            
            // if (elements.length > 0) {
            //   map.setFitView(elements, false, [50, 50, 50, 50]);
            // }
          }
        } else {
          console.log("路径规划失败，使用备用方案");
          handleRoutePlanningFallback(destLng, destLat);
        }
      }
    );
  } catch (error) {
    console.error("路径规划异常:", error);
    handleRoutePlanningFallback(destLng, destLat);
  }
};

// 路径规划备用方案
const handleRoutePlanningFallback = (destLng, destLat) => {
  const distance = window.AMap.GeometryUtil.distance(
    [currentPosition.value.lng, currentPosition.value.lat],
    [destLng, destLat]
  );

  const duration = (distance / 1000) * 3.6 * 60;

  routeInfo.value = {
    distance: Math.round(distance),
    duration: Math.round(duration * 60),
  };
  canOrder.value = true;

  console.log("使用备用路线信息:", {
    distance: Math.round(distance) + "m",
    duration: Math.round(duration) + "分钟",
  });

  if (routeLine) map.remove(routeLine);
  routeLine = new window.AMap.Polyline({
    path: [
      [currentPosition.value.lng, currentPosition.value.lat],
      [destLng, destLat],
    ],
    strokeColor: "#409EFF",
    strokeWeight: 6,
    strokeOpacity: 0.6,
    strokeStyle: "dashed",
  });
  map.add(routeLine);

  // 安全地调整地图视野
  const elements = [];
  if (pickupMarker) elements.push(pickupMarker);
  if (destMarker) elements.push(destMarker);
  if (routeLine) elements.push(routeLine);
  
  if (elements.length > 0) {
    map.setFitView(elements, false, [50, 50, 50, 50]);
  }
};

// 叫车
const handleCallCar = async () => {
  if (!canOrder.value) return;

  if (currentOrder.value || hasActiveOrder.value) {
    ElMessage.warning("您已有进行中的订单，请等待完成后再下单");
    return;
  }

  // 预约单需要选择时间
  if (bookingType.value === 'scheduled' && !scheduledTime.value) {
    ElMessage.warning("请选择预约时间");
    return;
  }

  isCalling.value = true;

  try {
    let destLng, destLat;
    if (typeof destination.value.location === "string") {
      [destLng, destLat] = destination.value.location.split(",").map(Number);
    } else if (
      destination.value.location &&
      destination.value.location.lng &&
      destination.value.location.lat
    ) {
      destLng = destination.value.location.lng;
      destLat = destination.value.location.lat;
    }

    // 确保用户信息完整性
    try {
      await userStore.ensureUserInfo()
    } catch (error) {
      console.error('❌ 用户状态检查失败:', error)
      ElMessage.error(error.message)
      return
    }

    const passengerId = userStore.user.passengerId || userStore.user.id
    if (!passengerId) {
      console.error('❌ 无法获取乘客ID，用户数据:', userStore.user)
      ElMessage.error('用户数据异常，请重新登录')
      return
    }

    console.log('🆔 使用乘客ID:', passengerId)
    console.log('👤 用户信息:', userStore.user)

    const orderData = {
      passengerId: passengerId,
      pickupAddress: pickupAddress.value,
      pickupLatitude: currentPosition.value.lat,
      pickupLongitude: currentPosition.value.lng,
      destinationAddress: destination.value.name,
      destinationLatitude: destLat,
      destinationLongitude: destLng,
      estimatedDistance: routeInfo.value ? parseFloat((routeInfo.value.distance / 1000).toFixed(2)) : 0, // 添加距离信息（公里），默认0
      estimatedDuration: routeInfo.value ? Math.round(routeInfo.value.duration / 60) : 0, // 添加预估时长（分钟），默认0
      estimatedFare: getPrice(selectedCarType.value),
    };

    let response, result;

    if (bookingType.value === 'scheduled') {
      // 创建预约单
      orderData.scheduledTime = formatDateTimeForBackend(scheduledTime.value);
      
      console.log("发送预约单数据:", orderData);

      response = await fetch("/api/orders/scheduled", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${userStore.token}`,
        },
        body: JSON.stringify(orderData),
      });
    } else {
      // 创建实时单
      orderData.orderType = "REAL_TIME";
      orderData.carType = selectedCarType.value;
      
      console.log("发送实时单数据:", orderData);

      response = await fetch("/api/orders/create", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${userStore.token}`,
        },
        body: JSON.stringify(orderData),
      });
    }

    result = await response.json();

    if (response.ok && result.code === 200) {
      if (bookingType.value === 'scheduled') {
        // 预约单创建成功
        const scheduledOrder = result.data;
        
        ElMessage.success(`预约单创建成功！预约时间：${new Date(scheduledTime.value).toLocaleString()}`);
        
        // 设置为当前订单
        orderStore.setCurrentOrder(scheduledOrder);
        
        // 重置表单
        bookingType.value = 'immediate';
        scheduledTime.value = null;
        
        // 刷新活跃订单状态
        await checkActiveOrder();
        
      } else {
        // 实时单创建成功
        const newOrder = {
          id: result.data,
          orderNumber: `ORDER${result.data}`,
          pickupAddress: pickupAddress.value,
          destinationAddress: destination.value.name,
          pickupLatitude: currentPosition.value.lat,
          pickupLongitude: currentPosition.value.lng,
          destinationLatitude: destLat,
          destinationLongitude: destLng,
          estimatedDistance: routeInfo.value ? parseFloat((routeInfo.value.distance / 1000).toFixed(2)) : 0, // 添加距离信息，默认0
          estimatedDuration: routeInfo.value ? Math.round(routeInfo.value.duration / 60) : 0, // 添加预估时长，默认0
          estimatedFare: getPrice(selectedCarType.value),
          carType: selectedCarType.value,
          status: "PENDING",
        };

        orderStore.setCurrentOrder(newOrder);

        ElMessage.success(
          `已为您呼叫${carTypes[selectedCarType.value].name}，正在为您寻找司机...`
        );

        // 更新UI状态，禁用相关操作
        canOrder.value = false;
        updatePickupMarkerDraggable();
      }
      
      isCalling.value = false;
      
    } else {
      ElMessage.error((bookingType.value === 'scheduled' ? "创建预约单失败: " : "下单失败: ") + (result.message || "未知错误"));
      isCalling.value = false;
    }
  } catch (error) {
    console.error("下单错误:", error);
    ElMessage.error(bookingType.value === 'scheduled' ? "创建预约单失败，请重试" : "叫车失败，请重试");
    isCalling.value = false;
  }
};

// WebSocket连接现在由全局store管理

// 处理订单更新
const handleOrderUpdate = (data) => {
  console.log("🔔 收到订单更新:", data);
  console.log("📋 消息类型:", data.type);
  console.log("🆔 当前订单:", currentOrder.value);

  switch (data.type) {
    case "ORDER_ASSIGNED":
      console.log("🚗 处理司机接单消息");
      handleOrderAssigned(data);
      break;
    case "DRIVER_LOCATION":
      console.log("📍 处理司机位置更新");
      updateDriverLocation(data);
      break;
    case "ORDER_STATUS_CHANGE":
      console.log("📊 处理订单状态变化");
      handleStatusChange(data);
      break;
    default:
      console.log("❓ 未知消息类型:", data.type);
  }
};

// 处理订单被接单
const handleOrderAssigned = (data) => {
  console.log("司机接单数据:", data);

  if (data.order) {
    const updatedOrder = {
      ...currentOrder.value,
      ...data.order,
      status: "ASSIGNED",
    };
    orderStore.setCurrentOrder(updatedOrder);
  }

  if (data.driver) {
    const driverData = {
      id: data.driver.id || data.driver.driverId,
      name: data.driver.name || data.driver.driverName || "司机",
      phone: data.driver.phone || data.driver.phoneNumber,
      avatar: data.driver.avatar,
      rating: data.driver.rating || 5.0,
      // 车辆信息
      plateNumber: data.driver.plateNumber || "暂无车牌",
      vehicleBrand: data.driver.vehicleBrand || "未知",
      vehicleModel: data.driver.vehicleModel || "",
      vehicleColor: data.driver.vehicleColor || "未知",
      vehicleType: data.driver.vehicleType || "ECONOMY",
      vehicleSeats: data.driver.vehicleSeats || 5,
      vehicleYear: data.driver.vehicleYear,
      vehicleInfo: data.driver.vehicleInfo || data.driver.carModel || `${data.driver.vehicleBrand || '未知'} ${data.driver.vehicleModel || ''} ${data.driver.plateNumber || ''}`.trim(),
      carModel: data.driver.carModel || `${data.driver.vehicleBrand || '未知'} ${data.driver.vehicleModel || ''}`.trim(),
      latitude: data.driver.latitude,
      longitude: data.driver.longitude,
    };
    orderStore.setDriverInfo(driverData);
  }

  orderStore.updateOrderStatus("ASSIGNED");

  ElMessage.success("司机已接单，正在前往接您");

  if (
    driverInfo.value &&
    driverInfo.value.latitude &&
    driverInfo.value.longitude
  ) {
    showDriverOnMap(driverInfo.value.latitude, driverInfo.value.longitude);
    // 第一次显示司机时调整地图视野
    updateSharedMapView(true);
    startDriverTracking();
  }
};

// 在地图上显示司机
const showDriverOnMap = (lat, lng) => {
  console.log("🚗 在共享地图上显示司机位置:", lat, lng);

  if (driverMarker) {
    map.remove(driverMarker);
  }

  driverMarker = new window.AMap.Marker({
    position: [lng, lat],
    map,
    icon: new window.AMap.Icon({
      size: new window.AMap.Size(40, 40),
      image: createDriverIcon(),
      imageOffset: new window.AMap.Pixel(-20, -20),
    }),
    title: `司机 ${driverInfo.value?.name || ""}`,
    zIndex: 100,
    animation: "AMAP_ANIMATION_DROP",
  });

  // 初始化司机到上车点的路线
  updateDriverRoute(lat, lng);
};

// 创建司机图标
const createDriverIcon = () => {
  const svg = `
    <svg width="40" height="40" viewBox="0 0 40 40" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <filter id="shadow" x="-50%" y="-50%" width="200%" height="200%">
          <feDropShadow dx="2" dy="2" stdDeviation="2" flood-color="#000" flood-opacity="0.3"/>
        </filter>
      </defs>
      <rect x="8" y="15" width="24" height="12" rx="2" fill="#409EFF" filter="url(#shadow)"/>
      <rect x="10" y="17" width="8" height="8" rx="1" fill="#87CEEB"/>
      <rect x="22" y="17" width="8" height="8" rx="1" fill="#87CEEB"/>
      <circle cx="12" cy="30" r="3" fill="#333"/>
      <circle cx="28" cy="30" r="3" fill="#333"/>
      <polygon points="32,20 36,18 36,22" fill="#FF6B6B"/>
    </svg>
  `;
  return "data:image/svg+xml;base64," + btoa(svg);
};

// 更新司机到上车点的路线
const updateDriverRoute = async (driverLat, driverLng) => {
  // 只有在司机前往上车点时才显示司机路线（ASSIGNED 或 PICKUP 状态）
  if (
    !currentOrder.value ||
    (orderStatus.value !== "ASSIGNED" && orderStatus.value !== "PICKUP")
  ) {
    return;
  }

  console.log("🛣️ 更新司机到上车点的路线");

  // 获取上车点坐标（从订单信息中获取，这是乘客设置的上车点）
  const pickupLat = currentOrder.value.pickupLatitude;
  const pickupLng = currentOrder.value.pickupLongitude;

  if (!pickupLat || !pickupLng) {
    console.error("❌ 缺少上车点坐标信息");
    return;
  }

  console.log("📍 司机位置:", driverLat, driverLng);
  console.log("📍 上车点位置:", pickupLat, pickupLng);

  try {
    // 清除旧的司机路线
    if (window.driverRouteLine) {
      map.remove(window.driverRouteLine);
      window.driverRouteLine = null;
    }
    if(routeLine){
      map.remove(routeLine);
    }
    if(currentDriving){
      map.remove(currentDriving);
    }
    // 使用高德地图路线规划API
    const driving = new window.AMap.Driving(getDrivingConfig());

    driving.search(
      new window.AMap.LngLat(driverLng, driverLat),
      new window.AMap.LngLat(pickupLng, pickupLat),
      (status, result) => {
        console.log("🗺️ 乘客端司机路线规划结果:", status, result);
        console.log(
          "🚗 司机路线规划策略:",
          window.AMap.DrivingPolicy.LEAST_DISTANCE
        );
        console.log("⛴️ 司机路线轮渡设置:", 0);

        if (
          status === "complete" &&
          result.routes &&
          result.routes.length > 0
        ) {
          const route = result.routes[0];

          // 获取路径点
          const pathPoints = [];
          route.steps.forEach((step) => {
            if (step.path && step.path.length > 0) {
              step.path.forEach((point) => {
                pathPoints.push([point.lng, point.lat]);
              });
            }
          });

          if (pathPoints.length > 0) {
            // 绘制司机到上车点的路线（使用不同颜色区分）
            window.driverRouteLine = new window.AMap.Polyline({
              path: pathPoints,
              strokeColor: "#FF6B6B", // 红色表示司机路线
              strokeWeight: 4,
              strokeOpacity: 0.8,
              strokeStyle: "solid",
              lineJoin: "round",
              lineCap: "round",
              zIndex: 50,
            });

            map.add(window.driverRouteLine);
            console.log("✅ 司机到上车点的路线已更新");
          }
        } else {
          console.warn("⚠️ 司机路线规划失败，使用直线显示");
          // 使用直线连接司机和上车点
          window.driverRouteLine = new window.AMap.Polyline({
            path: [
              [driverLng, driverLat],
              [pickupLng, pickupLat],
            ],
            strokeColor: "#FF6B6B",
            strokeWeight: 4,
            strokeOpacity: 0.6,
            strokeStyle: "dashed",
            zIndex: 50,
          });

          map.add(window.driverRouteLine);
        }
      }
    );
  } catch (error) {
    console.error("❌ 更新司机路线失败:", error);
  }
};

// 更新共享地图视图
const updateSharedMapView = (forceRefit = false) => {
  try {
    console.log("🗺️ 更新共享地图视图, forceRefit:", forceRefit);

    const markers = [];
    const overlays = [];

    if (pickupMarker) markers.push(pickupMarker);
    if (destMarker) markers.push(destMarker);
    if (driverMarker) markers.push(driverMarker);
    if (routeLine) overlays.push(routeLine);
    if (window.driverRouteLine) overlays.push(window.driverRouteLine);

    // 只有在强制重新适配或者是第一次显示司机时才调整视野
    if (forceRefit && markers.length > 0) {
      const allElements = markers.concat(overlays);
      map.setFitView(allElements, false, [60, 60, 60, 60]);
      console.log(
        "✅ 共享地图视野已调整，显示",
        markers.length,
        "个标记和",
        overlays.length,
        "条路线"
      );
    } else {
      console.log("📍 司机位置已更新，保持当前地图视野");
    }
  } catch (error) {
    console.error("❌ 更新共享地图视图失败:", error);
  }
};

// 开始追踪司机位置（已移除轮询逻辑，司机位置通过WebSocket推送）
const startDriverTracking = () => {
  console.log("🔍 开始追踪司机位置（通过WebSocket接收位置更新）");
  // 司机位置现在通过WebSocket实时推送，无需轮询
};

// 已删除requestDriverLocation函数 - 司机位置通过WebSocket推送，无需主动请求

// 更新司机位置
const updateDriverLocation = (data) => {
  console.log("📍 更新司机位置:", data);

  if (data.driverId === driverInfo.value?.id) {
    // 先清理所有旧的司机路径（修复司机位置更新后旧路径没有清除的问题）
    clearAllDriverRoutes();
    
    // 通过store更新司机位置信息，不要直接修改computed属性
    if (driverInfo.value) {
      const updatedDriver = {
        ...driverInfo.value,
        latitude: data.latitude,
        longitude: data.longitude,
      };
      orderStore.setDriverInfo(updatedDriver);
      console.log("✅ 司机位置已通过store更新");
    }

    const isFirstTime = !driverMarker;

    if (driverMarker) {
      // 司机标记已存在，只更新位置，不调整地图视野
      driverMarker.setPosition([data.longitude, data.latitude]);
      driverMarker.setAnimation("AMAP_ANIMATION_BOUNCE");
      setTimeout(() => {
        if (driverMarker) {
          driverMarker.setAnimation("AMAP_ANIMATION_NONE");
        }
      }, 1000);

      // 如果行程进行中，乘客和司机共享位置
      if (orderStatus.value === "IN_PROGRESS") {
        currentPosition.value = {
          lng: data.longitude,
          lat: data.latitude,
        };
        // 重新规划从当前位置到目的地的路线
        showRoute();
      } else {
        // 重新规划司机到上车点的路线
        updateDriverRoute(data.latitude, data.longitude);
      }

      // 不强制调整地图视野
      updateSharedMapView(false);
    } else {
      // 第一次显示司机，需要调整地图视野
      showDriverOnMap(data.latitude, data.longitude);
      updateSharedMapView(true);
    }
  }
};

// 处理订单状态变化
const handleStatusChange = (data) => {
  console.log("📋 订单状态变化:", data);
  console.log("🔍 当前订单ID:", currentOrder.value?.id);
  console.log("🔍 推送订单ID:", data.orderId);

  if (
    data.orderId === currentOrder.value?.id ||
    data.orderId === currentOrder.value?.orderNumber
  ) {
    orderStore.updateOrderStatus(data.status);

    switch (data.status) {
      case "PICKUP":
        ElMessage.success("司机已到达上车点，请准备上车");
        // 司机到达上车点，继续显示司机到上车点的路线
        break;
      case "IN_PROGRESS":
        ElMessage.success("行程已开始，请系好安全带");
        // 行程开始，清除司机到上车点的路线，司机现在前往目的地
        if (window.driverRouteLine) {
          map.remove(window.driverRouteLine);
          window.driverRouteLine = null;
        }

        // 行程开始后，乘客和司机共享位置
        // 移除上车点标记，因为已经上车了
        if (pickupMarker) {
          map.remove(pickupMarker);
          pickupMarker = null;
        }

        // 显示从当前位置（司机位置）到目的地的路线
        if (
          driverInfo.value &&
          driverInfo.value.latitude &&
          driverInfo.value.longitude
        ) {
          currentPosition.value = {
            lng: driverInfo.value.longitude,
            lat: driverInfo.value.latitude,
          };
          // 重新规划从当前位置到目的地的路线
          showRoute();
        }
        break;
      case "COMPLETED":
        ElMessage.success("行程已完成，感谢您的使用");
        resetOrderState();
        break;
      case "CANCELLED":
        ElMessage.warning("订单已取消");
        resetOrderState();
        break;
    }

    updateSharedMapView(false);
  }
};

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
    pickupMarker.on("dragend", async (e) => {
      if (currentOrder.value) {
        console.log("⚠️ 订单已发起，不允许修改上车点");
        pickupMarker.setPosition([currentPosition.value.lng, currentPosition.value.lat]);
        return;
      }
      
      const newPos = e.lnglat;
      console.log("📍 上车点被拖拽到新位置:", newPos.lng, newPos.lat);
      
      currentPosition.value = { lng: newPos.lng, lat: newPos.lat };
      
      try {
        const address = await reverseGeocode(newPos.lng, newPos.lat);
        pickupAddress.value = address;
        pickupMarker.setTitle(`上车点: ${address}`);
        
        if (destination.value) {
          showRoute();
        }
      } catch (error) {
        console.error("逆地理编码失败:", error);
      }
    });
  }
  
  // 恢复目的地标记
  if (destination.value && destination.value.location && !destMarker) {
    console.log('🎯 恢复目的地标记');
    
    let destLng, destLat;
    if (typeof destination.value.location === "string") {
      [destLng, destLat] = destination.value.location.split(",").map(Number);
    } else if (destination.value.location.lng && destination.value.location.lat) {
      destLng = destination.value.location.lng;
      destLat = destination.value.location.lat;
    }
    
    if (destLng && destLat) {
      destMarker = new window.AMap.Marker({
        position: [destLng, destLat],
        map,
        icon: new window.AMap.Icon({
          image: 'https://webapi.amap.com/theme/v1.3/markers/n/end.png',
          size: new window.AMap.Size(25, 34),
          imageSize: new window.AMap.Size(25, 34)
        }),
        title: destination.value.name || '目的地'
      });
    }
  } else if (currentOrder.value && currentOrder.value.destinationLatitude && currentOrder.value.destinationLongitude && !destMarker) {
    // 如果destination.value不存在，但是currentOrder中有目的地信息，使用订单中的目的地
    console.log('🎯 从订单信息恢复目的地标记');
    destMarker = new window.AMap.Marker({
      position: [currentOrder.value.destinationLongitude, currentOrder.value.destinationLatitude],
      map,
      icon: new window.AMap.Icon({
        image: 'https://webapi.amap.com/theme/v1.3/markers/n/end.png',
        size: new window.AMap.Size(25, 34),
        imageSize: new window.AMap.Size(25, 34)
      }),
      title: currentOrder.value.destinationAddress || '目的地'
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

  // 重置路线初始化标记
  window.routeInitialized = false;
  canOrder.value = true;
  isCalling.value = false;

  // 恢复上车点标记的可拖拽状态
  updatePickupMarkerDraggable();

  // 重新检查未支付订单
  orderStore.checkUnpaidOrders();
  
  console.log('✅ 订单状态重置完成');
};



// 恢复订单相关的地图元素
const restoreOrderMapElements = () => {
  console.log('🔄 恢复订单相关的地图元素...');
  
  // 检查地图是否已初始化
  if (!map) {
    console.log('⚠️ 地图未初始化，延迟恢复地图元素');
    setTimeout(restoreOrderMapElements, 1000);
    return;
  }
  
  // 检查是否有当前订单
  if (!currentOrder.value) {
    console.log('✅ 没有当前订单，无需恢复地图元素');
    return;
  }
  
  // 🔑 关键修复：首先设置目的地信息，然后恢复标记
  if (currentOrder.value.destinationLatitude && currentOrder.value.destinationLongitude) {
    console.log('🎯 设置目的地信息');
    destination.value = {
      name: currentOrder.value.destinationAddress,
      location: {
        lng: currentOrder.value.destinationLongitude,
        lat: currentOrder.value.destinationLatitude
      }
    };
    destinationKeyword.value = currentOrder.value.destinationAddress;
  }
  
  // 然后恢复地图标记
  restoreMapMarkers();
  
  console.log('📋 当前订单状态:', orderStatus.value);
  console.log('🚗 司机信息:', driverInfo.value);
  
  // 如果有司机信息，在地图上显示司机位置
  if (driverInfo.value && driverInfo.value.latitude && driverInfo.value.longitude && 
      (orderStatus.value === 'ASSIGNED' || orderStatus.value === 'PICKUP' || orderStatus.value === 'IN_PROGRESS')) {
    console.log('🚗 恢复司机位置标记:', driverInfo.value.latitude, driverInfo.value.longitude);
    
    try {
      showDriverOnMap(driverInfo.value.latitude, driverInfo.value.longitude);
      
      // 开始追踪司机位置
      startDriverTracking();
    } catch (error) {
      console.error('❌ 恢复司机位置失败:', error);
    }
  }
  
  // 重新规划路线（目的地信息已在上面设置）
  if (destination.value) {
    console.log('🗺️ 恢复路线规划');
    setTimeout(() => {
      try {
        showRoute();
      } catch (error) {
        console.error('❌ 恢复路线规划失败:', error);
      }
    }, 1500);
  }
  
  // 根据订单状态调整地图视图
  setTimeout(() => {
    try {
      if (orderStatus.value === 'IN_PROGRESS' && driverInfo.value) {
        // 行程中，显示当前位置到目的地的路线
        console.log('🛣️ 行程进行中，调整地图视图');
        updateSharedMapView(true);
      } else if ((orderStatus.value === 'ASSIGNED' || orderStatus.value === 'PICKUP') && driverInfo.value && 
                 driverInfo.value.latitude && driverInfo.value.longitude) {
        // 司机前往上车点，显示司机到上车点的路线
        console.log('🚕 司机前往上车点，显示相关路线');
        updateDriverRoute(driverInfo.value.latitude, driverInfo.value.longitude);
      }
    } catch (error) {
      console.error('❌ 调整地图视图失败:', error);
    }
  }, 2000);
};

// 跳转到我的行程页面
const goToMyTrips = () => {
  console.log("🚀 跳转到我的行程页面");
  // 使用Vue Router跳转
  if (window.location.pathname.includes("passenger-app.html")) {
    // 如果是在独立页面中，直接跳转
    window.location.href = "/frontend/src/views/MyTrips.vue";
  } else {
    // 如果是在Vue应用中，使用路由跳转
    import("@/router")
      .then(({ default: router }) => {
        router.push("/dashboard/my-trips");
      })
      .catch(() => {
        // 如果路由不可用，使用直接跳转
        window.location.href = "#/dashboard/my-trips";
      });
  }
};

// 构建完整的头像URL
const buildAvatarUrl = (avatarPath) => {
  if (!avatarPath) return null;
  if (avatarPath.startsWith('http')) return avatarPath;
  // 添加服务器前缀
  return `http://localhost:8080${avatarPath}`;
};

// 获取默认头像
const getDefaultAvatar = (name) => {
  // 根据姓名生成默认头像
  const colors = ['#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEAA7', '#DDA0DD', '#98D8C8'];
  const colorIndex = name ? name.charCodeAt(0) % colors.length : 0;
  const backgroundColor = colors[colorIndex];
  const firstChar = name ? name.charAt(0).toUpperCase() : '司';
  
  // 创建SVG头像
  const svg = `
    <svg width="40" height="40" xmlns="http://www.w3.org/2000/svg">
      <circle cx="20" cy="20" r="20" fill="${backgroundColor}"/>
      <text x="20" y="26" text-anchor="middle" fill="white" font-size="16" font-weight="bold">${firstChar}</text>
    </svg>
  `;
  
  return `data:image/svg+xml;base64,${btoa(unescape(encodeURIComponent(svg)))}`;
};

// 处理头像加载错误
const handleAvatarError = (event) => {
  const img = event.target;
  const name = driverInfo.value?.name || '司机';
  img.src = getDefaultAvatar(name);
};

// 获取车辆类型文本
const getVehicleTypeText = (type) => {
  const typeMap = {
    'ECONOMY': '经济型',
    'COMFORT': '舒适型',
    'PREMIUM': '高级型',
    'LUXURY': '豪华型'
  };
  return typeMap[type] || '经济型';
};

// 拨打司机电话
const callDriver = () => {
  if (driverInfo.value?.phone) {
    // 在移动设备上直接拨打电话
    if (/Android|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent)) {
      window.location.href = `tel:${driverInfo.value.phone}`;
    } else {
      // 在桌面设备上显示电话号码
      ElMessage.info(`司机电话：${driverInfo.value.phone}`);
      // 复制到剪贴板
      if (navigator.clipboard) {
        navigator.clipboard.writeText(driverInfo.value.phone).then(() => {
          ElMessage.success('电话号码已复制到剪贴板');
        });
      }
    }
  }
};

// 切换司机详情面板
const toggleDriverDetails = () => {
  showDriverDetails.value = !showDriverDetails.value;
};
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
}

.search-container {
  display: flex;
  align-items: center;
  gap: 15px;
  background: #f8f9fa;
  border-radius: 12px;
  padding: 15px;
}

.location-item {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.location-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 16px;
}

.location-icon.pickup {
  background: #28a745;
}

.location-icon.destination {
  background: #dc3545;
}

.location-info {
  flex: 1;
}

.location-label {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.location-text {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.destination-input {
  border: none;
  background: transparent;
}

.destination-input :deep(.el-input__wrapper) {
  box-shadow: none;
  background: transparent;
}

.location-divider {
  width: 2px;
  height: 40px;
  background: #ddd;
  margin: 0 10px;
}

.map-container {
  width: 100vw;
  height: calc(100vh - 120px);
  margin-top: 120px;
  background: #f0f0f0;
  position: relative;
  z-index: 1;
}

.bottom-panel {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: white;
  padding: 20px;
  border-radius: 20px 20px 0 0;
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.1);
  z-index: 100;
}

.booking-type-selector {
  margin-bottom: 15px;
  text-align: center;
}

.booking-type-selector .el-radio-group {
  width: 100%;
}

.booking-type-selector .el-radio-button {
  flex: 1;
}

.scheduled-time-selector {
  margin-bottom: 15px;
  text-align: center;
}

.scheduled-time-selector .el-date-editor {
  width: 100%;
}

.route-info {
  margin-bottom: 20px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 12px;
}

.route-details {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.distance {
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.duration {
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
  padding: 15px;
  border: 2px solid #e9ecef;
  border-radius: 12px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  background: white;
}

.car-type-item:hover {
  border-color: #409eff;
  background: #f0f8ff;
}

.car-type-item.active {
  border-color: #409eff;
  background: #409eff;
  color: white;
}

.car-icon {
  font-size: 24px;
  margin-bottom: 8px;
}

.car-name {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 4px;
}

.car-price {
  font-size: 16px;
  font-weight: bold;
  color: #28a745;
}

.car-type-item.active .car-price {
  color: white;
}

.car-type-item.disabled {
  opacity: 0.5;
  cursor: not-allowed;
  pointer-events: none;
}

.destination-input:disabled {
  background-color: #f5f5f5;
  cursor: not-allowed;
}

.call-car-btn {
  width: 100%;
  height: 50px;
  font-size: 16px;
  font-weight: bold;
  border-radius: 25px;
}

.order-panel {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: white;
  padding: 20px;
  border-radius: 20px 20px 0 0;
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.1);
  z-index: 100;
}

.order-status {
  text-align: center;
}

.status-text {
  font-size: 18px;
  font-weight: bold;
  color: #333;
  margin-bottom: 15px;
}

.driver-info {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 15px;
  text-align: left;
}

.driver-name {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin-bottom: 5px;
}

.driver-phone {
  font-size: 14px;
  color: #666;
  margin-bottom: 5px;
}

.vehicle-info {
  font-size: 14px;
  color: #666;
}

/* 紧凑的司机信息条 */
.driver-info-bar {
  position: absolute;
  top: 80px;
  left: 10px;
  right: 10px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  padding: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  cursor: pointer;
  transition: all 0.3s ease;
  z-index: 1000;
}

.driver-info-bar:hover {
  background: rgba(255, 255, 255, 1);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
}

.driver-bar-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.driver-avatar-small img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 2px solid #fff;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
  object-fit: cover;
}

.driver-basic-info {
  flex: 1;
}

.driver-name-small {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 2px;
}

.vehicle-brief {
  font-size: 13px;
  color: #7f8c8d;
}

.driver-rating-small {
  display: flex;
  align-items: center;
  gap: 4px;
}

.rating-score-small {
  font-size: 14px;
  font-weight: 600;
  color: #2c3e50;
}

.star-small {
  color: #ffd700;
  font-size: 14px;
}

.expand-icon {
  color: #7f8c8d;
  transition: transform 0.3s ease;
}

/* 司机详细信息面板 */
.driver-details-panel {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: white;
  border-radius: 20px 20px 0 0;
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.15);
  max-height: 70vh;
  overflow-y: auto;
  z-index: 2000;
  animation: slideUp 0.3s ease-out;
}

@keyframes slideUp {
  from {
    transform: translateY(100%);
  }
  to {
    transform: translateY(0);
  }
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 20px 10px;
  border-bottom: 1px solid #f0f0f0;
}

.panel-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #2c3e50;
}

.panel-content {
  padding: 20px;
}

.driver-profile {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.driver-avatar-large img {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  border: 3px solid #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  object-fit: cover;
}

.driver-info {
  flex: 1;
}

.driver-name-large {
  font-size: 20px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 6px;
}

.driver-rating-large {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.rating-stars {
  display: flex;
  gap: 2px;
}

.star {
  color: #ddd;
  font-size: 16px;
  transition: color 0.2s ease;
}

.star.filled {
  color: #ffd700;
}

.rating-score {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.driver-phone {
  font-size: 14px;
  color: #7f8c8d;
  font-weight: 500;
}

.vehicle-info-section {
  margin-bottom: 24px;
}

.vehicle-info-section h4 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
}

.vehicle-details-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-item .label {
  font-size: 12px;
  color: #7f8c8d;
  font-weight: 500;
}

.detail-item .value {
  font-size: 14px;
  color: #2c3e50;
  font-weight: 600;
}

.plate-number-compact {
  font-family: 'Courier New', monospace;
  background: #f8f9fa;
  padding: 4px 8px;
  border-radius: 6px;
  border: 1px solid #e9ecef;
  letter-spacing: 1px;
  display: inline-block;
}

.action-buttons {
  margin-top: 20px;
}

.cancel-order-section {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #eee;
}

/* 时间选择器样式 */
.time-picker-container {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  background: white;
}

.time-display {
  flex: 1;
  font-size: 14px;
}

.time-display .placeholder {
  color: #c0c4cc;
}

.time-picker-btn {
  padding: 8px 16px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.time-picker-btn:hover {
  background: #337ecc;
}

.time-picker-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.time-picker-modal {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 400px;
  max-height: 80vh;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
}

.time-picker-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #eee;
}

.time-picker-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #999;
  cursor: pointer;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-btn:hover {
  color: #666;
}

.time-picker-wheels {
  display: flex;
  padding: 20px;
  gap: 20px;
}

.wheel-container {
  flex: 1;
  text-align: center;
}

.wheel-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 10px;
  font-weight: 500;
}

.wheel {
  height: 200px;
  overflow-y: auto;
  border: 1px solid #eee;
  border-radius: 8px;
  background: #fafafa;
}

.wheel-item {
  padding: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  border-bottom: 1px solid #f0f0f0;
}

.wheel-item:hover {
  background: #f0f8ff;
}

.wheel-item.active {
  background: #409eff;
  color: white;
  font-weight: bold;
}

.wheel-item:last-child {
  border-bottom: none;
}

.time-picker-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 20px;
  border-top: 1px solid #eee;
}

.cancel-btn, .confirm-btn {
  padding: 10px 20px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.cancel-btn {
  background: #f5f5f5;
  color: #666;
}

.cancel-btn:hover {
  background: #e8e8e8;
}

.confirm-btn {
  background: #409eff;
  color: white;
}

.confirm-btn:hover {
  background: #337ecc;
}

@media (max-width: 768px) {
  .header {
    padding: 15px;
  }

  .search-container {
    flex-direction: column;
    gap: 10px;
  }

  .location-divider {
    width: 40px;
    height: 2px;
    margin: 10px 0;
  }

  .car-types {
    flex-direction: column;
    gap: 10px;
  }

  .route-details {
    flex-direction: column;
    gap: 10px;
  }

  .time-picker-modal {
    width: 95%;
    margin: 20px;
  }

  .time-picker-wheels {
    gap: 10px;
    padding: 15px;
  }

  .wheel {
    height: 150px;
  }
}
</style>
