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
            disabled: currentOrder !== null,
          }"
          @click="currentOrder ? null : selectCarType('economy')"
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
        :disabled="!canOrder || currentOrder !== null"
        @click="handleCallCar"
        size="large"
      >
        {{ callCarText }}
      </el-button>
    </div>

    <!-- 订单状态面板 -->
    <div v-if="currentOrder" class="order-panel">
      <div class="order-status">
        <div class="status-text">{{ getStatusText() }}</div>
        <div v-if="driverInfo && (orderStatus === 'ASSIGNED' || orderStatus === 'PICKUP')" class="driver-info">
          <div class="driver-name">司机：{{ driverInfo.name }}</div>
          <div class="driver-phone">{{ driverInfo.phone }}</div>
          <div class="vehicle-info">{{ driverInfo.vehicleInfo }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, onUnmounted } from "vue";
import { ElMessage } from "element-plus";
import { Location } from "@element-plus/icons-vue";
import { useUserStore } from "@/stores/user";
import {
  mapConfig,
  getMapApiUrl,
  getRestApiUrl,
  getSecurityConfig,
} from "@/config/map";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";

const userStore = useUserStore();

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

// 订单状态相关
const currentOrder = ref(null);
const driverInfo = ref(null);
const orderStatus = ref("");

// WebSocket相关
let stompClient = null;

// 司机位置相关变量
let driverMarker = null;
let driverTrackingTimer = null;

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
  if (stompClient) {
    stompClient.deactivate();
  }
  stopDriverTracking();
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
  if (currentOrder.value) return "订单进行中";
  if (isCalling.value) return "正在叫车...";
  if (!canOrder.value) return "请选择目的地";
  return "立即叫车";
});

// 获取订单状态文本
const getStatusText = () => {
  switch (orderStatus.value) {
    case "PENDING":
      return "正在为您寻找司机...";
    case "ASSIGNED":
      return "司机已接单，正在前往上车点";
    case "PICKUP":
      return "司机已到达上车点，请准备上车";
    case "IN_PROGRESS":
      return "行程进行中，前往目的地";
    case "COMPLETED":
      return "行程已完成";
    case "CANCELLED":
      return "订单已取消";
    default:
      return "未知状态";
  }
};
// 初始化地图
onMounted(() => {
  console.log("开始初始化地图...");

  setTimeout(() => {
    if (window.AMap) {
      console.log("高德地图已加载，直接初始化");
      initMap();
    } else {
      console.log("开始加载高德地图API...");

      window._AMapSecurityConfig = getSecurityConfig();

      const script = document.createElement("script");
      script.src = getMapApiUrl();
      script.onload = () => {
        console.log("高德地图API加载成功");
        setTimeout(initMap, 200);
      };
      script.onerror = (error) => {
        console.error("高德地图API加载失败:", error);
        ElMessage.error("地图加载失败，请检查网络连接");
      };
      document.head.appendChild(script);
    }
  }, 500);
});

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
              size: new window.AMap.Size(32, 32),
              image: "https://webapi.amap.com/theme/v1.3/markers/n/mark_b.png",
            }),
            title: "拖拽调整上车位置",
          });

          // 添加拖拽事件监听器
          pickupMarker.on("dragend", (e) => {
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

            const newPosition = e.lnglat;
            console.log(
              "🚩 上车点被拖拽到新位置:",
              newPosition.lng,
              newPosition.lat
            );

            // 更新当前位置
            currentPosition.value = {
              lng: newPosition.lng,
              lat: newPosition.lat,
            };

            // 获取新位置的地址
            getAddressFromLocation(newPosition.lng, newPosition.lat);

            // 如果已有目的地，重新规划路线
            if (destination.value) {
              showRoute();
            }
          });

          map.setCenter([lng, lat]);
          console.log("定位成功，当前位置:", lng, lat);

          getAddressFromLocation(lng, lat);
          initAutocomplete();
        } else {
          console.error("定位失败:", status);
          ElMessage.error("定位失败，请检查浏览器权限");
          pickupAddress.value = "定位失败";
          initAutocomplete();
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

    const response = await fetch(
      getRestApiUrl("geocode/regeo", {
        location: `${lng},${lat}`,
        extensions: "all",
      })
    );
    const data = await response.json();

    console.log("地址解析结果:", data);

    if (data.status === "1" && data.regeocode) {
      pickupAddress.value = data.regeocode.formatted_address;
      console.log("获取到地址:", data.regeocode.formatted_address);
    } else {
      console.log("地址获取失败，使用备用方案");
      pickupAddress.value = `位置 (${lat.toFixed(6)}, ${lng.toFixed(6)})`;
    }
  } catch (error) {
    console.error("地址获取异常:", error);
    pickupAddress.value = `位置 (${lat.toFixed(6)}, ${lng.toFixed(6)})`;
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

// 停止追踪司机位置
const stopDriverTracking = () => {
  if (driverTrackingTimer) {
    clearInterval(driverTrackingTimer);
    driverTrackingTimer = null;
  }
  console.log("⏹️ 已停止追踪司机位置");
};

// 更新上车点标记的可拖拽状态
const updatePickupMarkerDraggable = () => {
  if (pickupMarker) {
    const isDraggable = !currentOrder.value;
    pickupMarker.setDraggable(isDraggable);
    pickupMarker.setCursor(isDraggable ? "move" : "default");
    pickupMarker.setTitle(isDraggable ? "拖拽调整上车位置" : "上车点");
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

            map.setFitView(
              [pickupMarker, destMarker, routeLine],
              false,
              [50, 50, 50, 50]
            );
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

  map.setFitView(
    [pickupMarker, destMarker, routeLine],
    false,
    [50, 50, 50, 50]
  );
};

// 叫车
const handleCallCar = async () => {
  if (!canOrder.value) return;

  if (currentOrder.value) {
    ElMessage.warning("您已有进行中的订单，请等待完成后再下单");
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

    const orderData = {
      passengerId: userStore.user.passengerId,
      pickupAddress: pickupAddress.value,
      pickupLatitude: currentPosition.value.lat,
      pickupLongitude: currentPosition.value.lng,
      destinationAddress: destination.value.name,
      destinationLatitude: destLat,
      destinationLongitude: destLng,
      orderType: "REAL_TIME",
      carType: selectedCarType.value,
      estimatedFare: getPrice(selectedCarType.value),
    };

    console.log("发送订单数据:", orderData);

    const response = await fetch("/api/orders/create", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${userStore.token}`,
      },
      body: JSON.stringify(orderData),
    });

    const result = await response.json();

    if (response.ok && result.code === 200) {
      currentOrder.value = {
        id: result.data,
        orderNumber: result.data,
        pickupAddress: pickupAddress.value,
        destinationAddress: destination.value.name,
        pickupLatitude: currentPosition.value.lat,
        pickupLongitude: currentPosition.value.lng,
        destinationLatitude: destLat,
        destinationLongitude: destLng,
        estimatedFare: getPrice(selectedCarType.value),
        carType: selectedCarType.value,
        status: "PENDING",
      };

      orderStatus.value = "PENDING";

      ElMessage.success(
        `已为您呼叫${carTypes[selectedCarType.value].name}，正在为您寻找司机...`
      );

      connectWebSocket(result.data);

      // 更新UI状态，禁用相关操作
      canOrder.value = false;
      isCalling.value = false;
      updatePickupMarkerDraggable();
    } else {
      ElMessage.error("下单失败: " + (result.message || "未知错误"));
      isCalling.value = false;
    }
  } catch (error) {
    console.error("下单错误:", error);
    ElMessage.error("叫车失败，请重试");
    isCalling.value = false;
  }
};

// WebSocket连接
const connectWebSocket = (orderId) => {
  try {
    console.log("开始建立乘客WebSocket连接...");

    const socket = new SockJS("/ws");
    stompClient = new Client({
      webSocketFactory: () => socket,
      debug: (str) => {
        console.log("乘客STOMP Debug:", str);
      },
    });

    stompClient.onConnect = () => {
      console.log("✅ 乘客WebSocket连接成功");

      const passengerId = (userStore.user.passengerId || userStore.user.id).toString();
      console.log("乘客ID:", passengerId);

      stompClient.subscribe(`/user/${passengerId}/queue/orders`, (message) => {
        console.log("🚗 乘客收到订单更新:", message.body);
        console.log("📨 消息头信息:", message.headers);
        try {
          const data = JSON.parse(message.body);
          console.log("📋 解析后的数据:", data);
          handleOrderUpdate(data);
        } catch (error) {
          console.error("❌ 解析订单更新数据失败:", error);
          console.error("❌ 原始消息:", message.body);
        }
      });

      stompClient.publish({
        destination: "/app/passenger/connect",
        body: JSON.stringify({
          passengerId: passengerId,
          orderId: orderId,
          timestamp: Date.now(),
        }),
      });

      console.log("✅ 乘客WebSocket订阅完成");
    };

    stompClient.onStompError = (frame) => {
      console.error("❌ 乘客WebSocket连接失败:", frame);
    };

    stompClient.onWebSocketError = (error) => {
      console.error("❌ 乘客WebSocket错误:", error);
    };

    stompClient.onDisconnect = () => {
      console.log("⚠️ 乘客WebSocket连接断开");
    };

    stompClient.activate();
  } catch (error) {
    console.error("❌ 乘客WebSocket连接错误:", error);
  }
};

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
    currentOrder.value = {
      ...currentOrder.value,
      ...data.order,
      status: "ASSIGNED",
    };
  }

  if (data.driver) {
    driverInfo.value = {
      id: data.driver.id || data.driver.driverId,
      name: data.driver.name || data.driver.driverName || "司机",
      phone: data.driver.phone || data.driver.phoneNumber,
      avatar: data.driver.avatar,
      rating: data.driver.rating || 5.0,
      vehicleInfo: data.driver.vehicleInfo || data.driver.carModel || "车牌号",
      latitude: data.driver.latitude,
      longitude: data.driver.longitude,
    };
  }

  orderStatus.value = "ASSIGNED";

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

// 开始追踪司机位置
const startDriverTracking = () => {
  console.log("🔍 开始追踪司机位置");

  if (driverTrackingTimer) {
    clearInterval(driverTrackingTimer);
  }

  driverTrackingTimer = setInterval(() => {
    if (currentOrder.value && driverInfo.value) {
      requestDriverLocation();
    }
  }, 5000);
};

// 请求司机位置更新
const requestDriverLocation = async () => {
  try {
    const response = await fetch(
      `/api/drivers/${driverInfo.value.id}/location`,
      {
        headers: {
          Authorization: `Bearer ${userStore.token}`,
        },
      }
    );

    if (response.ok) {
      const locationData = await response.json();
      if (locationData.code === 200 && locationData.data) {
        updateDriverLocation({
          driverId: driverInfo.value.id,
          latitude: locationData.data.latitude,
          longitude: locationData.data.longitude,
        });
      }
    }
  } catch (error) {
    console.error("获取司机位置失败:", error);
  }
};

// 更新司机位置
const updateDriverLocation = (data) => {
  console.log("📍 更新司机位置:", data);

  if (data.driverId === driverInfo.value?.id) {
    if (driverInfo.value) {
      driverInfo.value.latitude = data.latitude;
      driverInfo.value.longitude = data.longitude;
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
        // 更新司机到上车点的路线
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

  if (data.orderId === currentOrder.value?.id || data.orderId === currentOrder.value?.orderNumber) {
    currentOrder.value.status = data.status;
    orderStatus.value = data.status;

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

// 重置订单状态
const resetOrderState = () => {
  console.log("🔄 重置订单状态");

  currentOrder.value = null;
  driverInfo.value = null;
  orderStatus.value = "";

  stopDriverTracking();

  if (driverMarker) {
    map.remove(driverMarker);
    driverMarker = null;
  }

  if (window.driverRouteLine) {
    map.remove(window.driverRouteLine);
    window.driverRouteLine = null;
  }

  if (routeLine) {
    routeLine.setOptions({
      strokeColor: "#409EFF",
      strokeWeight: 6,
      strokeOpacity: 0.8,
    });
  }

  if (stompClient) {
    stompClient.deactivate();
    stompClient = null;
  }

  // 重置路线初始化标记
  window.routeInitialized = false;

  canOrder.value = true;
  isCalling.value = false;

  // 恢复上车点标记的可拖拽状态
  updatePickupMarkerDraggable();
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
}
</style>
