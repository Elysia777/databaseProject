import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useUserStore } from './user'
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

export const useOrderStore = defineStore('order', () => {
  // WebSocket连接
  let stompClient = null

  // 状态
  const currentOrder = ref(null)
  const driverInfo = ref(null)
  const orderStatus = ref('')
  const hasUnpaidOrders = ref(false)

  // 计算属性
  const hasActiveOrder = computed(() => !!currentOrder.value)
  const canOrder = computed(() => !hasActiveOrder.value && !hasUnpaidOrders.value)
  
  // 判断是否可以取消订单
  const canCancelOrder = computed(() => {
    return currentOrder.value && 
           (orderStatus.value === 'PENDING' || 
            orderStatus.value === 'ASSIGNED' || 
            orderStatus.value === 'PICKUP')
  })

  // 获取订单状态文本
  const getStatusText = () => {
    switch (orderStatus.value) {
      case 'PENDING':
        return '正在为您寻找司机...'
      case 'ASSIGNED':
        return '司机已接单，正在前往上车点'
      case 'PICKUP':
        return '司机已到达上车点，请准备上车'
      case 'IN_PROGRESS':
        return '行程进行中，前往目的地'
      case 'COMPLETED':
        return '行程已完成'
      case 'CANCELLED':
        return '订单已取消'
      default:
        return '未知状态'
    }
  }

  // 方法
  const setCurrentOrder = (order) => {
    currentOrder.value = order
    if (order) {
      orderStatus.value = order.status || 'PENDING'
      
      // 获取当前用户ID
      const userStore = useUserStore()
      const currentUserId = userStore.user?.passengerId || userStore.user?.id
      
      // 保存到localStorage以便页面刷新后恢复
      localStorage.setItem('currentOrder', JSON.stringify(order))
      localStorage.setItem('orderStatus', orderStatus.value)
      localStorage.setItem('orderUserId', currentUserId?.toString() || '') // 保存用户ID用于验证
    } else {
      localStorage.removeItem('currentOrder')
      localStorage.removeItem('orderStatus')
      localStorage.removeItem('driverInfo')
      localStorage.removeItem('orderUserId')
    }
  }

  const setDriverInfo = (driver) => {
    driverInfo.value = driver
    if (driver) {
      localStorage.setItem('driverInfo', JSON.stringify(driver))
    } else {
      localStorage.removeItem('driverInfo')
    }
  }

  const updateOrderStatus = (status) => {
    orderStatus.value = status
    if (currentOrder.value) {
      currentOrder.value.status = status
      localStorage.setItem('currentOrder', JSON.stringify(currentOrder.value))
      localStorage.setItem('orderStatus', status)
    }
  }

  const setHasUnpaidOrders = (hasUnpaid) => {
    hasUnpaidOrders.value = hasUnpaid
  }

  // 从localStorage恢复订单状态
  const restoreOrderState = () => {
    try {
      const userStore = useUserStore()
      const currentUserId = userStore.user?.passengerId || userStore.user?.id
      
      if (!currentUserId) {
        console.log('⚠️ 无法获取当前用户ID，清除localStorage中的订单状态')
        clearOrderState()
        return
      }

      const savedOrder = localStorage.getItem('currentOrder')
      const savedStatus = localStorage.getItem('orderStatus')
      const savedDriver = localStorage.getItem('driverInfo')
      const savedUserId = localStorage.getItem('orderUserId') // 保存订单所属用户ID

      // 检查localStorage中的订单是否属于当前用户
      if (savedUserId && savedUserId !== currentUserId.toString()) {
        console.log('⚠️ localStorage中的订单不属于当前用户，清除状态')
        console.log('保存的用户ID:', savedUserId, '当前用户ID:', currentUserId)
        clearOrderState()
        return
      }

      if (savedOrder) {
        const orderData = JSON.parse(savedOrder)
        // 再次验证订单的passengerId是否匹配
        if (orderData.passengerId && orderData.passengerId !== currentUserId) {
          console.log('⚠️ 订单数据中的乘客ID不匹配，清除状态')
          console.log('订单乘客ID:', orderData.passengerId, '当前用户ID:', currentUserId)
          clearOrderState()
          return
        }
        
        currentOrder.value = orderData
        console.log('🔄 恢复订单状态:', currentOrder.value)
      }

      if (savedStatus) {
        orderStatus.value = savedStatus
        console.log('🔄 恢复订单状态:', orderStatus.value)
      }

      if (savedDriver) {
        driverInfo.value = JSON.parse(savedDriver)
        console.log('🔄 恢复司机信息:', driverInfo.value)
      }

      // 如果有进行中的订单，重新建立WebSocket连接
      if (currentOrder.value && orderStatus.value && 
          ['PENDING', 'ASSIGNED', 'PICKUP', 'IN_PROGRESS'].includes(orderStatus.value)) {
        console.log('🔌 检测到进行中的订单，重新建立WebSocket连接')
        console.log('订单ID:', currentOrder.value.id, '状态:', orderStatus.value)
        
        // 延迟建立连接，确保用户信息已完全加载
        setTimeout(() => {
          connectWebSocket(currentOrder.value.id)
        }, 1000)
      }
    } catch (error) {
      console.error('恢复订单状态失败:', error)
      clearOrderState()
    }
  }

  // 清除订单状态
  const clearOrderState = () => {
    currentOrder.value = null
    driverInfo.value = null
    orderStatus.value = ''
    localStorage.removeItem('currentOrder')
    localStorage.removeItem('orderStatus')
    localStorage.removeItem('driverInfo')
    localStorage.removeItem('orderUserId')
    console.log('🔄 订单状态已清除')
  }

  // 检查未支付订单
  const checkUnpaidOrders = async () => {
    try {
      const userStore = useUserStore()
      const passengerId = userStore.user?.passengerId || userStore.user?.id
      
      if (!passengerId) {
        console.log('⚠️ 无法获取乘客ID，跳过未支付订单检查')
        return
      }

      console.log('🔍 检查未支付订单...')
      
      const response = await fetch(`/api/orders/unpaid?passengerId=${passengerId}`, {
        headers: {
          'Authorization': `Bearer ${userStore.token}`
        }
      })
      
      if (response.ok) {
        const result = await response.json()
        console.log('📋 未支付订单检查结果:', result)
        
        if (result.code === 200 && result.data && result.data.length > 0) {
          hasUnpaidOrders.value = true
          console.log('⚠️ 发现未支付订单:', result.data.length, '个')
        } else {
          hasUnpaidOrders.value = false
          console.log('✅ 没有未支付订单')
        }
      } else {
        console.error('❌ 检查未支付订单失败:', response.status)
        hasUnpaidOrders.value = false
      }
    } catch (error) {
      console.error('❌ 检查未支付订单异常:', error)
      hasUnpaidOrders.value = false
    }
  }

  // 获取当前进行中的订单
  const getCurrentOrder = async () => {
    try {
      const userStore = useUserStore()
      const passengerId = userStore.user?.passengerId || userStore.user?.id
      
      console.log('🔍 检查当前进行中的订单...')
      console.log('👤 当前用户信息:', userStore.user)
      console.log('🆔 使用的乘客ID:', passengerId)
      
      if (!passengerId) {
        console.log('⚠️ 无法获取乘客ID，跳过当前订单检查')
        return null
      }

      const apiUrl = `/api/orders/passenger/${passengerId}/current`
      console.log('📞 调用API:', apiUrl)
      
      const response = await fetch(apiUrl, {
        headers: {
          'Authorization': `Bearer ${userStore.token}`
        }
      })
      
      console.log('📡 API响应状态:', response.status)
      
      if (response.ok) {
        const result = await response.json()
        console.log('📋 当前订单检查结果:', result)
        
        if (result.code === 200 && result.data) {
          console.log('✅ 发现进行中的订单:', result.data)
          console.log('📊 订单状态:', result.data.status)
          console.log('🆔 订单乘客ID:', result.data.passengerId)
          setCurrentOrder(result.data)
          return result.data
        } else {
          console.log('✅ 没有进行中的订单')
          return null
        }
      } else {
        console.error('❌ 检查当前订单失败:', response.status)
        const errorText = await response.text()
        console.error('❌ 错误详情:', errorText)
        return null
      }
    } catch (error) {
      console.error('❌ 检查当前订单异常:', error)
      return null
    }
  }

  // WebSocket连接管理
  const connectWebSocket = (orderId) => {
    try {
      console.log('🔌 建立全局WebSocket连接...')
      
      // 如果已有连接，先断开
      if (stompClient) {
        stompClient.deactivate()
      }

      const userStore = useUserStore()
      const socket = new SockJS('/ws')
      stompClient = new Client({
        webSocketFactory: () => socket,
        debug: (str) => {
          console.log('🔌 STOMP Debug:', str)
        },
      })

      stompClient.onConnect = () => {
        console.log('✅ 全局WebSocket连接成功')

        // 确保用户信息存在
        if (!userStore.user) {
          console.error('❌ 用户信息不存在，无法建立WebSocket订阅')
          return
        }

        const passengerId = (userStore.user.passengerId || userStore.user.id)
        if (!passengerId) {
          console.error('❌ 用户ID不存在，无法建立WebSocket订阅')
          return
        }

        const passengerIdStr = passengerId.toString()
        console.log('🆔 乘客ID:', passengerIdStr)

        stompClient.subscribe(`/user/${passengerIdStr}/queue/orders`, (message) => {
          console.log('📨 收到订单更新:', message.body)
          try {
            const data = JSON.parse(message.body)
            console.log('📋 解析后的数据:', data)
            handleOrderUpdate(data)
          } catch (error) {
            console.error('❌ 解析订单更新数据失败:', error)
          }
        })

        stompClient.publish({
          destination: '/app/passenger/connect',
          body: JSON.stringify({
            passengerId: passengerIdStr,
            orderId: orderId,
            timestamp: Date.now(),
          }),
        })

        console.log('✅ 全局WebSocket订阅完成')
      }

      stompClient.onStompError = (frame) => {
        console.error('❌ 全局WebSocket连接失败:', frame)
      }

      stompClient.onWebSocketError = (error) => {
        console.error('❌ 全局WebSocket错误:', error)
      }

      stompClient.onDisconnect = () => {
        console.log('⚠️ 全局WebSocket连接断开')
      }

      stompClient.activate()
    } catch (error) {
      console.error('❌ 全局WebSocket连接错误:', error)
    }
  }

  const disconnectWebSocket = () => {
    if (stompClient) {
      console.log('🔌 断开全局WebSocket连接')
      stompClient.deactivate()
      stompClient = null
    }
  }

  // 处理订单更新消息
  const handleOrderUpdate = (data) => {
    console.log('🔔 全局处理订单更新:', data)
    console.log('📋 消息类型:', data.type)

    switch (data.type) {
      case 'ORDER_ASSIGNED':
        console.log('🚗 处理司机接单消息')
        handleOrderAssigned(data)
        break
      case 'DRIVER_LOCATION':
        console.log('📍 处理司机位置更新')
        handleDriverLocationUpdate(data)
        break
      case 'ORDER_STATUS_CHANGE':
        console.log('📊 处理订单状态变化')
        handleOrderStatusChange(data)
        break
      default:
        console.log('❓ 未知消息类型:', data.type)
    }

    // 通知地图组件处理消息（如果存在）
    if (window.handleMapOrderUpdate && typeof window.handleMapOrderUpdate === 'function') {
      console.log('🗺️ 通知地图组件处理消息')
      window.handleMapOrderUpdate(data)
    }
  }

  const handleOrderAssigned = (data) => {
    if (data.order) {
      const updatedOrder = {
        ...currentOrder.value,
        ...data.order,
        status: 'ASSIGNED',
      }
      setCurrentOrder(updatedOrder)
    }

    if (data.driver) {
      setDriverInfo(data.driver)
    }

    updateOrderStatus('ASSIGNED')
    
    // 通知UI更新
    if (window.ElMessage) {
      window.ElMessage.success('司机已接单，正在前往接您')
    }
  }

  const handleDriverLocationUpdate = (data) => {
    if (driverInfo.value && data.driverId === driverInfo.value.id) {
      const updatedDriver = {
        ...driverInfo.value,
        latitude: data.latitude,
        longitude: data.longitude,
      }
      setDriverInfo(updatedDriver)
    }
  }

  const handleOrderStatusChange = (data) => {
    if (data.orderId === currentOrder.value?.id || data.orderId === currentOrder.value?.orderNumber) {
      updateOrderStatus(data.status)
      
      // 根据状态显示消息
      if (window.ElMessage) {
        switch (data.status) {
          case 'PICKUP':
            window.ElMessage.success('司机已到达上车点，请准备上车')
            break
          case 'IN_PROGRESS':
            window.ElMessage.success('行程已开始，请系好安全带')
            break
          case 'COMPLETED':
            window.ElMessage.success('行程已完成，请前往我的行程页面完成支付')
            break
          case 'CANCELLED':
            window.ElMessage.warning('订单已取消')
            clearOrderState()
            break
        }
      }
    }
  }

  // 初始化订单状态（页面加载时调用）
  const initOrderState = async () => {
    console.log('🚀 初始化订单状态...')
    
    // 确保用户信息已加载
    const userStore = useUserStore()
    if (!userStore.user) {
      console.log('⚠️ 用户信息未加载，等待用户信息初始化...')
      try {
        await userStore.initUserInfo()
      } catch (error) {
        console.error('❌ 用户信息初始化失败:', error)
        clearOrderState()
        return
      }
    }
    
    // 先从localStorage恢复状态（现在用户信息已确保加载）
    restoreOrderState()
    
    // 然后从服务器获取最新状态
    const [, serverOrder] = await Promise.all([
      checkUnpaidOrders(),
      getCurrentOrder()
    ])
    
    // 如果服务器没有返回订单，但localStorage中有订单，说明localStorage数据过期，需要清除
    if (!serverOrder && currentOrder.value) {
      console.log('⚠️ 服务器没有进行中的订单，但localStorage中有订单数据，清除过期数据')
      clearOrderState()
    }
    
    // 如果有进行中的订单，建立WebSocket连接
    if (currentOrder.value && (orderStatus.value === 'PENDING' || orderStatus.value === 'ASSIGNED' || orderStatus.value === 'PICKUP' || orderStatus.value === 'IN_PROGRESS')) {
      console.log('🔄 发现进行中的订单，建立WebSocket连接')
      connectWebSocket(currentOrder.value.id)
    }
  }

  return {
    // 状态
    currentOrder,
    driverInfo,
    orderStatus,
    hasUnpaidOrders,
    
    // 计算属性
    hasActiveOrder,
    canOrder,
    canCancelOrder,
    
    // 方法
    getStatusText,
    setCurrentOrder,
    setDriverInfo,
    updateOrderStatus,
    setHasUnpaidOrders,
    restoreOrderState,
    clearOrderState,
    checkUnpaidOrders,
    getCurrentOrder,
    initOrderState
  }
})