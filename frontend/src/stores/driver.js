import { defineStore } from "pinia";
import { ref, computed } from "vue";
import { useUserStore } from "./user";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";

export const useDriverStore = defineStore("driver", () => {
  // WebSocket连接
  let stompClient = null;

  // 司机状态
  const isOnline = ref(false);
  const currentPosition = ref({ lng: 0, lat: 0 });
  const todayEarnings = ref(0);
  const completedOrders = ref(0);

  // 订单相关状态
  const pendingOrders = ref([]); // 待处理订单队列
  const currentOrder = ref(null); // 当前正在执行的订单
  const navigationInfo = ref(null); // 导航信息

  // 计算属性
  const hasActiveOrder = computed(() => !!currentOrder.value);
  const canAcceptNewOrders = computed(
    () => isOnline.value && !hasActiveOrder.value
  );

  // 判断司机是否可以取消订单
  const canDriverCancelOrder = computed(() => {
    return (
      currentOrder.value &&
      (currentOrder.value.status === "ASSIGNED" ||
        currentOrder.value.status === "PICKUP")
    );
  });

  // 获取订单状态文本
  const getOrderStatusText = (status) => {
    switch (status) {
      case "ASSIGNED":
        return "前往上车点";
      case "PICKUP":
        return "已到达，等待乘客上车";
      case "IN_PROGRESS":
        return "行程进行中";
      case "COMPLETED":
        return "订单已完成";
      case "CANCELLED":
        return "订单已取消";
      default:
        return "未知状态";
    }
  };

  // 保存司机状态到localStorage
  const saveDriverState = () => {
    try {
      const userStore = useUserStore();
      const driverId = userStore.user?.driverId || userStore.user?.id;

      if (driverId) {
        const driverState = {
          driverId: driverId,
          isOnline: isOnline.value,
          currentPosition: currentPosition.value,
          todayEarnings: todayEarnings.value,
          completedOrders: completedOrders.value,
          currentOrder: currentOrder.value,
          navigationInfo: navigationInfo.value,
          timestamp: Date.now(),
        };

        localStorage.setItem("driverState", JSON.stringify(driverState));
        localStorage.setItem("driverUserId", driverId.toString());
        console.log("💾 司机状态已保存到localStorage");
      }
    } catch (error) {
      console.error("❌ 保存司机状态失败:", error);
    }
  };

  // 从localStorage恢复司机状态
  const restoreDriverState = async () => {
    try {
      console.log("🔄 开始恢复司机状态...");

      const userStore = useUserStore();
      const currentDriverId = userStore.user?.driverId || userStore.user?.id;

      if (!currentDriverId) {
        console.log("⚠️ 无法获取当前司机ID，清除localStorage中的司机状态");
        clearDriverState();
        return;
      }

      const savedState = localStorage.getItem("driverState");
      const savedUserId = localStorage.getItem("driverUserId");

      // 检查localStorage中的状态是否属于当前司机
      if (savedUserId && savedUserId !== currentDriverId.toString()) {
        console.log("⚠️ localStorage中的状态不属于当前司机，清除状态");
        console.log(
          "保存的司机ID:",
          savedUserId,
          "当前司机ID:",
          currentDriverId
        );
        clearDriverState();
        return;
      }

      // 先从localStorage恢复基本状态
      if (savedState) {
        const driverState = JSON.parse(savedState);

        // 验证状态的有效性（不超过24小时）
        const stateAge = Date.now() - (driverState.timestamp || 0);
        const maxAge = 24 * 60 * 60 * 1000; // 24小时

        if (stateAge > maxAge) {
          console.log("⚠️ 司机状态已过期，清除状态");
          clearDriverState();
          return;
        }

        // 再次验证司机ID是否匹配
        if (driverState.driverId && driverState.driverId !== currentDriverId) {
          console.log("⚠️ 状态数据中的司机ID不匹配，清除状态");
          clearDriverState();
          return;
        }

        // 恢复基本状态
        isOnline.value = driverState.isOnline || false;
        currentPosition.value = driverState.currentPosition || {
          lng: 121.749849,
          lat: 39.044237,
        };
        todayEarnings.value = driverState.todayEarnings || 0;
        completedOrders.value = driverState.completedOrders || 0;
        currentOrder.value = driverState.currentOrder || null;
        navigationInfo.value = driverState.navigationInfo || null;

        console.log("🔄 从localStorage恢复基本状态");
        console.log("在线状态:", isOnline.value);
        console.log("当前订单:", currentOrder.value);
      } else {
        console.log("📱 没有保存的司机状态，使用默认状态");
        clearDriverState();
      }
    } catch (error) {
      console.error("❌ 恢复司机状态失败:", error);
      clearDriverState();
    }
  };

  // 清除司机状态
  const clearDriverState = () => {
    isOnline.value = false;
    currentPosition.value = { lng: 0, lat: 0 };
    todayEarnings.value = 0;
    completedOrders.value = 0;
    currentOrder.value = null;
    navigationInfo.value = null;
    pendingOrders.value = [];

    localStorage.removeItem("driverState");
    localStorage.removeItem("driverUserId");

    console.log("🔄 司机状态已清除");
  };

  // 检查当前进行中的订单（从后端获取）
  const getCurrentOrder = async () => {
    try {
      const userStore = useUserStore();
      const currentDriverId = userStore.user?.driverId || userStore.user?.id;

      console.log("🔍 检查司机当前进行中的订单...");
      console.log("👤 当前司机信息:", userStore.user);
      console.log("🆔 使用的司机ID:", currentDriverId);

      if (!currentDriverId) {
        console.log("⚠️ 无法获取司机ID，跳过当前订单检查");
        return null;
      }

      const apiUrl = `/api/drivers/${currentDriverId}/current-order`;
      console.log("📞 调用API:", apiUrl);

      const response = await fetch(apiUrl, {
        headers: {
          Authorization: `Bearer ${userStore.token}`,
        },
      });

      console.log("📡 API响应状态:", response.status);

      if (response.ok) {
        const result = await response.json();
        console.log("📋 当前订单检查结果:", result);

        if (result.code === 200 && result.data) {
          console.log("✅ 发现进行中的订单:", result.data);
          console.log("📊 订单状态:", result.data.status);
          console.log("🆔 订单司机ID:", result.data.driverId);

          // 设置当前订单
          currentOrder.value = result.data;
          saveDriverState();

          return result.data;
        } else {
          console.log("✅ 没有进行中的订单");
          return null;
        }
      } else {
        console.error("❌ 检查当前订单失败:", response.status);
        const errorText = await response.text();
        console.error("❌ 错误详情:", errorText);
        return null;
      }
    } catch (error) {
      console.error("❌ 检查当前订单异常:", error);
      return null;
    }
  };

  // 检查司机后端状态
  const checkDriverBackendStatus = async () => {
    try {
      const userStore = useUserStore();
      const currentDriverId = userStore.user?.driverId || userStore.user?.id;

      if (!currentDriverId) {
        console.log("⚠️ 无法获取司机ID，跳过后端状态检查");
        return null;
      }

      console.log("🔍 检查司机后端状态...");

      const response = await fetch(
        `/api/drivers/${currentDriverId}/status-detail`,
        {
          headers: {
            Authorization: `Bearer ${userStore.token}`,
          },
        }
      );

      if (response.ok) {
        const result = await response.json();
        console.log("📋 司机后端状态:", result.data);
        return result.data;
      } else {
        console.error("❌ 检查司机后端状态失败:", response.status);
        return null;
      }
    } catch (error) {
      console.error("❌ 检查司机后端状态异常:", error);
      return null;
    }
  };

  // 清除订单相关状态（保留收入统计）
  const clearOrderState = () => {
    currentOrder.value = null;
    navigationInfo.value = null;
    pendingOrders.value = [];
    saveDriverState();
    console.log("🔄 订单状态已清除");
  };

  // 只清理待处理订单，保留当前订单
  const clearPendingOrders = () => {
    pendingOrders.value = [];
    saveDriverState();
    console.log("🔄 待处理订单已清除，当前订单保留");
  };

  // 初始化司机状态（页面加载时调用）
  const initDriverState = async () => {
    console.log("🚀 初始化司机状态...");

    // 确保用户信息已加载
    const userStore = useUserStore();
    if (!userStore.user) {
      console.log("⚠️ 用户信息未加载，等待用户信息初始化...");
      try {
        await userStore.initUserInfo();
      } catch (error) {
        console.error("❌ 用户信息初始化失败:", error);
        clearDriverState();
        return;
      }
    }

    // 先从localStorage恢复状态
    await restoreDriverState();

    // 然后从服务器获取最新状态
    const [backendStatus, serverOrder] = await Promise.all([
      checkDriverBackendStatus(),
      getCurrentOrder(),
    ]);

    console.log("🔍 状态对比:");
    console.log(
      "- localStorage订单:",
      currentOrder.value
        ? `#${currentOrder.value.orderNumber || currentOrder.value.id}`
        : "无"
    );
    console.log(
      "- 服务器订单:",
      serverOrder ? `#${serverOrder.orderNumber || serverOrder.id}` : "无"
    );
    console.log("- 后端在线状态:", backendStatus?.isOnlineAndFree || false);

    // 如果服务器有订单但localStorage没有，从服务器恢复
    if (serverOrder && !currentOrder.value) {
      console.log("🔄 服务器有订单但本地没有，从服务器恢复状态");
      currentOrder.value = serverOrder;
      isOnline.value = true; // 有进行中订单说明司机应该在线
      saveDriverState();
    }
    // 如果localStorage有订单但服务器没有，清除本地状态
    else if (currentOrder.value && !serverOrder) {
      console.log("⚠️ 本地有订单但服务器没有，清除过期的本地状态");
      currentOrder.value = null;
      navigationInfo.value = null;
      saveDriverState();
    }
    // 如果两边都有订单，检查是否一致
    else if (currentOrder.value && serverOrder) {
      const localOrderId = currentOrder.value.id || currentOrder.value.orderId;
      const serverOrderId = serverOrder.id || serverOrder.orderId;

      if (localOrderId !== serverOrderId) {
        console.log("⚠️ 本地订单与服务器订单不一致，使用服务器数据");
        currentOrder.value = serverOrder;
        saveDriverState();
      } else {
        console.log("✅ 本地订单与服务器订单一致");
        // 更新订单状态，以防有变化
        if (currentOrder.value.status !== serverOrder.status) {
          console.log(
            `📊 订单状态更新: ${currentOrder.value.status} -> ${serverOrder.status}`
          );
          currentOrder.value.status = serverOrder.status;
          saveDriverState();
        }
      }
    }

    // 确保WebSocket连接建立（如果有进行中的订单或司机在线）
    if (currentOrder.value || isOnline.value) {
      console.log("🔌 确保WebSocket连接建立...");
      setTimeout(() => {
        connectWebSocket();
      }, 1500);
    }

    console.log("✅ 司机状态初始化完成");
    console.log("- 在线状态:", isOnline.value);
    console.log(
      "- 当前订单:",
      currentOrder.value
        ? `#${currentOrder.value.orderNumber || currentOrder.value.id} (${currentOrder.value.status})`
        : "无"
    );
  };

  // 设置司机在线状态
  const setOnlineStatus = (online) => {
    isOnline.value = online;
    saveDriverState();
    console.log("📡 司机在线状态更新:", online ? "在线" : "离线");
  };

  // 更新司机当前位置
  const updateCurrentPosition = (position) => {
    currentPosition.value = position;
    saveDriverState();
    console.log("📍 司机位置已更新:", position);
  };

  // 更新司机位置
  const updateDriverPosition = (position) => {
    currentPosition.value = position;
    saveDriverState();
  };

  // 设置当前订单
  const setCurrentOrder = (order) => {
    currentOrder.value = order;
    saveDriverState();
    console.log("📋 当前订单更新:", order);
  };

  // 更新订单状态
  const updateOrderStatus = (status) => {
    if (currentOrder.value) {
      currentOrder.value.status = status;
      saveDriverState();
      console.log("📊 订单状态更新:", status);
    }
  };

  // 添加待处理订单
  const addPendingOrder = (order) => {
    const existingIndex = pendingOrders.value.findIndex(
      (o) => o.orderId === order.orderId
    );
    if (existingIndex === -1) {
      pendingOrders.value.push(order);
      console.log("📨 新增待处理订单:", order.orderNumber);
    }
  };

  // 移除待处理订单
  const removePendingOrder = (orderId) => {
    const index = pendingOrders.value.findIndex((o) => o.orderId === orderId);
    if (index !== -1) {
      pendingOrders.value.splice(index, 1);
      console.log("🗑️ 移除待处理订单:", orderId);
    }
  };

  // 更新今日收入
  const updateTodayEarnings = (earnings) => {
    todayEarnings.value = earnings;
    saveDriverState();
  };

  // 更新完成订单数
  const updateCompletedOrders = (count) => {
    completedOrders.value = count;
    saveDriverState();
  };

  // WebSocket连接管理
  const connectWebSocket = () => {
    try {
      console.log("🔌 建立司机WebSocket连接...");

      // 如果已有连接，强制断开
      if (stompClient && stompClient.connected) {
        console.log("🔌 强制断开现有WebSocket连接");
        stompClient.deactivate();
        stompClient = null;
      }

      const userStore = useUserStore();
      const socket = new SockJS("/ws");
      stompClient = new Client({
        webSocketFactory: () => socket,
        debug: (str) => {
          console.log("🔌 司机STOMP Debug:", str);
        },
        reconnectDelay: 5000, // 5秒后重连
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
      });

      stompClient.onConnect = () => {
        console.log("✅ 司机WebSocket连接成功");

        // 确保用户信息存在
        if (!userStore.user) {
          console.error("❌ 用户信息不存在，无法建立WebSocket订阅");
          return;
        }

        const driverId = userStore.user.driverId || userStore.user.id;
        if (!driverId) {
          console.error("❌ 司机ID不存在，无法建立WebSocket订阅");
          return;
        }

        const driverIdStr = driverId.toString();
        console.log("🆔 司机ID:", driverIdStr);

        // 发送连接请求，包含时间戳确保唯一性
        stompClient.publish({
          destination: "/app/driver/connect",
          body: JSON.stringify({
            driverId: driverIdStr,
            timestamp: Date.now(),
            sessionType: "DRIVER_SESSION",
            userAgent: navigator.userAgent,
          }),
        });

        // 订阅司机专用队列 - 订单队列
        const orderSubscription = stompClient.subscribe(
          `/user/${driverIdStr}/queue/orders`,
          (message) => {
            console.log("📨 司机收到订单更新:", message.body);
            try {
              const data = JSON.parse(message.body);
              console.log("📋 解析后的数据:", data);
              handleDriverOrderUpdate(data);
            } catch (error) {
              console.error("❌ 解析订单更新数据失败:", error);
            }
          }
        );

        // 订阅司机专用队列 - 通知队列
        const notificationSubscription = stompClient.subscribe(
          `/user/${driverIdStr}/queue/notifications`,
          (message) => {
            console.log("🔔 司机收到通知:", message.body);
            try {
              const data = JSON.parse(message.body);
              console.log("📋 解析后的通知数据:", data);
              handleDriverOrderUpdate(data); // 使用同一个处理函数
            } catch (error) {
              console.error("❌ 解析通知数据失败:", error);
            }
          }
        );

        // 订阅司机专用队列 - 连接确认队列
        const connectionSubscription = stompClient.subscribe(
          `/user/${driverIdStr}/queue/connection`,
          (message) => {
            console.log("🔗 司机收到连接确认:", message.body);
            try {
              const data = JSON.parse(message.body);
              if (data.status === "connected") {
                console.log("✅ WebSocket连接已确认，会话ID:", data.sessionId);
              }
            } catch (error) {
              console.error("❌ 解析连接确认数据失败:", error);
            }
          }
        );

        // 订阅司机广播主题（备用通道）
        const topicSubscription = stompClient.subscribe(
          `/topic/driver/${driverIdStr}`,
          (message) => {
            console.log("📢 司机收到广播消息:", message.body);
            try {
              const data = JSON.parse(message.body);
              console.log("📋 解析后的广播数据:", data);
              handleDriverOrderUpdate(data);
            } catch (error) {
              console.error("❌ 解析广播数据失败:", error);
            }
          }
        );

        console.log("✅ 司机WebSocket订阅完成");
        console.log("- 订单队列:", orderSubscription.id);
        console.log("- 通知队列:", notificationSubscription.id);
        console.log("- 连接队列:", connectionSubscription.id);
        console.log("- 广播主题:", topicSubscription.id);

        // 将连接状态暴露到全局，方便调试
        window.driverStompClient = stompClient;
      };

      stompClient.onStompError = (frame) => {
        console.error("❌ 司机WebSocket连接失败:", frame);
      };

      stompClient.onWebSocketError = (error) => {
        console.error("❌ 司机WebSocket错误:", error);
      };

      stompClient.onDisconnect = () => {
        console.log("⚠️ 司机WebSocket连接断开");
        
        // 如果有进行中的订单或司机在线，尝试重连
        if (currentOrder.value || isOnline.value) {
          console.log("🔄 检测到司机在线或有进行中订单，3秒后尝试重连...");
          setTimeout(() => {
            if (!stompClient || !stompClient.connected) {
              connectWebSocket();
            }
          }, 3000);
        }
      };

      stompClient.activate();
    } catch (error) {
      console.error("❌ 司机WebSocket连接错误:", error);
    }
  };

  const disconnectWebSocket = () => {
    if (stompClient) {
      console.log("🔌 断开司机WebSocket连接");
      stompClient.deactivate();
      stompClient = null;
    }
  };

  // 处理司机订单更新消息
  const handleDriverOrderUpdate = (data) => {
    console.log("🔔 司机处理订单更新:", data);
    console.log("📋 消息类型:", data.type);
    console.log("📋 消息优先级:", data.priority || "NORMAL");

    switch (data.type) {
      case "NEW_ORDER":
        console.log("📨 收到新订单");
        console.log("📋 订单数据:", data);

        // WebSocket消息直接包含订单字段，不是嵌套在order对象中
        const orderData = {
          orderId: data.orderId,
          orderNumber: data.orderNumber,
          orderType: data.orderType, // 添加订单类型
          pickupAddress: data.pickupAddress,
          destinationAddress: data.destinationAddress,
          pickupLatitude: data.pickupLatitude,
          pickupLongitude: data.pickupLongitude,
          destinationLatitude: data.destinationLatitude,
          destinationLongitude: data.destinationLongitude,
          passengerId: data.passengerId,
          distance: data.distance,
          estimatedFare: data.estimatedFare,
          scheduledTime: data.scheduledTime, // 添加预约时间
          timestamp: data.timestamp,
          countdown: 30, // 添加倒计时初始值
          processing: false // 添加处理状态
        };

        console.log("📦 处理后的订单数据:", orderData);
        addPendingOrder(orderData);
        break;
        
      case "ORDER_CANCELLED":
        console.log("❌ 订单被取消");
        console.log("📋 取消的订单ID:", data.orderId);
        console.log("📋 取消原因:", data.reason);
        console.log("📋 当前订单:", currentOrder.value);
        
        // 处理订单取消
        if (data.orderId) {
          const orderIdStr = data.orderId.toString();
          
          // 从待处理订单中移除
          removePendingOrder(orderIdStr);
          
          // 如果是当前正在执行的订单被取消
          if (currentOrder.value) {
            const currentOrderId = (currentOrder.value.id || currentOrder.value.orderId || currentOrder.value.orderNumber).toString();
            
            if (currentOrderId === orderIdStr) {
              console.log("⚠️ 当前执行的订单被取消，清除订单状态");
              setCurrentOrder(null);
              clearOrderState();
              
              // 显示取消通知
              if (window.ElMessage) {
                window.ElMessage.warning(`订单已被取消：${data.reason || '乘客取消'}`);
              }
              
              // 通知地图组件清除路线
              if (window.handleDriverMapUpdate && typeof window.handleDriverMapUpdate === "function") {
                window.handleDriverMapUpdate({
                  type: "CLEAR_ROUTE",
                  reason: "ORDER_CANCELLED"
                });
              }
            }
          }
          
          console.log("✅ 订单取消处理完成");
        }
        break;
        
      case "ORDER_STATUS_CHANGE":
        console.log("📊 订单状态变化");
        if (
          data.orderId &&
          currentOrder.value &&
          currentOrder.value.id === data.orderId
        ) {
          updateOrderStatus(data.status);
        }
        break;
        
      case "ORDER_ASSIGNED":
        console.log("✅ 订单分配确认");
        // 订单分配确认，通常在接单后收到
        break;
        
      case "DRIVER_LOCATION":
        console.log("📍 司机位置更新");
        // 司机位置更新，可以忽略
        break;
        
      default:
        console.log("❓ 未知消息类型:", data.type);
        console.log("📋 完整消息数据:", data);
    }

    // 通知地图组件处理消息（如果存在）
    if (
      window.handleDriverMapUpdate &&
      typeof window.handleDriverMapUpdate === "function"
    ) {
      console.log("🗺️ 通知司机地图组件处理消息");
      window.handleDriverMapUpdate(data);
    }
  };

  return {
    // 状态
    isOnline,
    currentPosition,
    todayEarnings,
    completedOrders,
    pendingOrders,
    currentOrder,
    navigationInfo,

    // 计算属性
    hasActiveOrder,
    canAcceptNewOrders,
    canDriverCancelOrder,

    // 方法
    getOrderStatusText,
    saveDriverState,
    restoreDriverState,
    clearDriverState,
    clearOrderState,
    clearPendingOrders,
    initDriverState,
    getCurrentOrder,
    checkDriverBackendStatus,
    setOnlineStatus,
    updateCurrentPosition,
    updateDriverPosition,
    setCurrentOrder,
    updateOrderStatus,
    addPendingOrder,
    removePendingOrder,
    updateTodayEarnings,
    updateCompletedOrders,
    connectWebSocket,
    disconnectWebSocket,
    handleDriverOrderUpdate,
  };
});
