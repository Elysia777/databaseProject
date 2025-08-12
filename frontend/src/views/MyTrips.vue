<template>
  <div class="my-trips">
    <!-- 顶部标题 -->
    <div class="header">
      <h1>我的行程</h1>
      <div class="stats">
        <span>总计 {{ orders.length }} 个订单</span>
        <span v-if="unpaidCount > 0" class="unpaid-count">{{ unpaidCount }} 个待支付</span>
      </div>
    </div>

    <!-- 当前进行中的订单 -->
    <div v-if="orderStore.currentOrder" class="current-order-section">
      <h3>🚗 当前订单</h3>
      <div class="current-order-card">
        <div class="status-badge current" :class="getStatusClass(orderStore.orderStatus)">
          {{ getStatusText(orderStore.orderStatus) }}
        </div>
        
        <div class="order-header">
          <div class="order-number">订单号: {{ orderStore.currentOrder.orderNumber }}</div>
          <div class="order-time">{{ formatTime(orderStore.currentOrder.createdAt) }}</div>
        </div>

        <div class="trip-info">
          <div class="location-item">
            <el-icon class="pickup-icon"><Location /></el-icon>
            <div class="location-text">
              <div class="label">上车点</div>
              <div class="address">{{ orderStore.currentOrder.pickupAddress }}</div>
            </div>
          </div>
          
          <div class="location-divider"></div>
          
          <div class="location-item">
            <el-icon class="destination-icon"><Position /></el-icon>
            <div class="location-text">
              <div class="label">目的地</div>
              <div class="address">{{ orderStore.currentOrder.destinationAddress }}</div>
            </div>
          </div>
        </div>

        <div class="fare-info">
          <div class="fare-item">
            <span class="label">预估费用:</span>
            <span class="amount">¥{{ orderStore.currentOrder.estimatedFare }}</span>
          </div>
        </div>

        <div v-if="orderStore.driverInfo" class="driver-info">
          <h4>司机信息</h4>
          <div class="driver-details">
            <span>司机：{{ orderStore.driverInfo.name }}</span>
            <span>电话：{{ orderStore.driverInfo.phone }}</span>
            <span>车辆：{{ orderStore.driverInfo.vehicleInfo }}</span>
          </div>
        </div>

        <div class="current-order-actions">
          <el-button type="primary" @click="goToMap">返回地图</el-button>
          <el-button v-if="orderStore.canCancelOrder" type="danger" plain @click="cancelCurrentOrder">取消订单</el-button>
        </div>
      </div>
    </div>

    <!-- 筛选条件 -->
    <div class="filter-section">
      <el-card class="filter-card">
        <div class="filter-header">
          <h3>📋 {{ orderStore.currentOrder ? '历史订单' : '我的订单' }}</h3>
          <el-button 
            type="text" 
            @click="toggleFilters"
            class="toggle-filters"
          >
            <el-icon><Filter /></el-icon>
            {{ showFilters ? '收起筛选' : '展开筛选' }}
          </el-button>
        </div>
        
        <div v-show="showFilters" class="filters-content">
          <el-form :model="filters" inline class="filter-form">
            <el-form-item label="订单状态">
              <el-select 
                v-model="filters.status" 
                placeholder="选择状态" 
                clearable
                style="width: 140px"
                @change="applyFilters"
              >
                <el-option label="全部" value="" />
                <el-option label="预约中" value="SCHEDULED" />
                <el-option label="等待接单" value="PENDING" />
                <el-option label="司机已接单" value="ASSIGNED" />
                <el-option label="司机已到达" value="PICKUP" />
                <el-option label="行程中" value="IN_PROGRESS" />
                <el-option label="已完成" value="COMPLETED" />
                <el-option label="已取消" value="CANCELLED" />
              </el-select>
            </el-form-item>
            
            <el-form-item label="支付状态">
              <el-select 
                v-model="filters.paymentStatus" 
                placeholder="支付状态" 
                clearable
                style="width: 120px"
                @change="applyFilters"
              >
                <el-option label="全部" value="" />
                <el-option label="已支付" value="PAID" />
                <el-option label="未支付" value="UNPAID" />
                <el-option label="已退款" value="REFUNDED" />
              </el-select>
            </el-form-item>
            
            <el-form-item label="时间范围">
              <el-date-picker
                v-model="filters.dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                style="width: 240px"
                @change="applyFilters"
              />
            </el-form-item>
            
            <el-form-item label="关键词">
              <el-input
                v-model="filters.keyword"
                placeholder="搜索订单号、地址"
                clearable
                style="width: 200px"
                @input="debounceSearch"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            
            <el-form-item>
              <el-button type="primary" @click="applyFilters">
                <el-icon><Search /></el-icon>
                搜索
              </el-button>
              <el-button @click="resetFilters">
                <el-icon><Refresh /></el-icon>
                重置
              </el-button>
            </el-form-item>
          </el-form>
          
          <!-- 快速筛选标签 -->
          <div class="quick-filters">
            <span class="quick-filter-label">快速筛选：</span>
            <el-tag 
              v-for="quickFilter in quickFilters" 
              :key="quickFilter.key"
              :type="activeQuickFilter === quickFilter.key ? 'primary' : 'info'"
              :effect="activeQuickFilter === quickFilter.key ? 'dark' : 'plain'"
              @click="applyQuickFilter(quickFilter.key)"
              class="quick-filter-tag"
            >
              {{ quickFilter.label }}
            </el-tag>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 历史订单列表 -->
    <div class="orders-container">
      <div class="orders-header">
        <div class="orders-count">
          共找到 {{ filteredOrders.length }} 条订单
          <span v-if="filters.status || filters.paymentStatus || filters.dateRange || filters.keyword" class="filter-tip">
            (已筛选)
          </span>
        </div>
        <div class="sort-options">
          <el-select 
            v-model="sortBy" 
            placeholder="排序方式" 
            style="width: 140px"
            @change="applySorting"
          >
            <el-option label="时间倒序" value="time_desc" />
            <el-option label="时间正序" value="time_asc" />
            <el-option label="费用高到低" value="fare_desc" />
            <el-option label="费用低到高" value="fare_asc" />
          </el-select>
        </div>
      </div>
      
      <div v-if="loading" class="loading">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <div v-else-if="orders.length === 0 && !orderStore.currentOrder" class="empty">
        <el-icon><DocumentRemove /></el-icon>
        <p>暂无行程记录</p>
      </div>

      <div v-else class="orders-list">
        <div 
          v-for="order in paginatedOrders" 
          :key="order.id" 
          class="order-item"
          :class="{ 'unpaid': isUnpaid(order) }"
        >
          <!-- 订单状态标签 -->
          <div class="status-badge" :class="getStatusClass(order.status)">
            {{ getStatusText(order.status) }}
          </div>

          <!-- 订单基本信息 -->
          <div class="order-header">
            <div class="order-number">订单号: {{ order.orderNumber }}</div>
            <div class="order-time">{{ formatTime(order.createdAt) }}</div>
          </div>

          <!-- 行程信息 -->
          <div class="trip-info">
            <div class="location-item">
              <el-icon class="pickup-icon"><Location /></el-icon>
              <div class="location-text">
                <div class="label">上车点</div>
                <div class="address">{{ order.pickupAddress }}</div>
              </div>
            </div>
            
            <div class="location-divider"></div>
            
            <div class="location-item">
              <el-icon class="destination-icon"><Position /></el-icon>
              <div class="location-text">
                <div class="label">目的地</div>
                <div class="address">{{ order.destinationAddress }}</div>
              </div>
            </div>
          </div>

          <!-- 费用信息 -->
          <div class="fare-info">
            <div class="fare-item">
              <span class="label">预估费用:</span>
              <span class="amount">¥{{ order.estimatedFare }}</span>
            </div>
            <div v-if="order.actualFare" class="fare-item">
              <span class="label">实际费用:</span>
              <span class="amount actual">¥{{ order.actualFare }}</span>
            </div>
          </div>

          <!-- 支付状态和操作 -->
          <div class="payment-section">
            <div v-if="order.paymentStatus === 'PAID'" class="payment-status paid">
              <el-icon><SuccessFilled /></el-icon>
              <span>已支付 ({{ getPaymentMethodText(order.paymentMethod) }})</span>
            </div>
            
            <div v-else-if="isUnpaid(order)" class="payment-actions">
              <div class="unpaid-notice">
                <el-icon><WarningFilled /></el-icon>
                <span>待支付</span>
              </div>
              <el-button 
                type="primary" 
                @click="showPaymentDialog(order)"
                :loading="paymentLoading"
                size="small"
              >
                立即支付
              </el-button>
            </div>
            
            <div v-else class="payment-status other">
              <span>{{ getPaymentStatusText(order.paymentStatus) }}</span>
            </div>
          </div>

          <!-- 司机信息 (如果有) -->
          <div v-if="order.driverId" class="driver-info">
            <el-icon><User /></el-icon>
            <span>司机ID: {{ order.driverId }}</span>
          </div>

          <!-- 评价功能 -->
          <div v-if="order.status === 'COMPLETED'" class="review-section">
            <div v-if="order.reviewStatus === 'reviewed'" class="review-status">
              <el-icon><StarFilled /></el-icon>
              <span>已评价</span>
              <el-button type="text" @click="viewReview(order)" size="small">查看评价</el-button>
            </div>
            <div v-else class="review-actions">
              <el-button 
                type="primary" 
                @click="showReviewDialog(order)"
                size="small"
                plain
              >
                <el-icon><Star /></el-icon>
                评价此次行程
              </el-button>
            </div>
          </div>
        </div>
        
        <!-- 分页 -->
        <div v-if="filteredOrders.length > pageSize" class="pagination-container">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[5, 10, 20, 50]"
            :total="filteredOrders.length"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </div>

    <!-- 评价对话框 -->
    <ReviewDialog
      v-model="reviewDialogVisible"
      :order-info="selectedOrderForReview"
      :driver-info="selectedDriverInfo"
      @review-submitted="handleReviewSubmitted"
    />

    <!-- 支付对话框 -->
    <el-dialog
      v-model="paymentDialogVisible"
      title="选择支付方式"
      width="400px"
      :close-on-click-modal="false"
    >
      <div v-if="selectedOrder" class="payment-dialog">
        <div class="order-summary">
          <h3>订单信息</h3>
          <div class="summary-item">
            <span>订单号:</span>
            <span>{{ selectedOrder.orderNumber }}</span>
          </div>
          <div class="summary-item">
            <span>行程:</span>
            <span>{{ selectedOrder.pickupAddress }} → {{ selectedOrder.destinationAddress }}</span>
          </div>
          <div class="summary-item total">
            <span>应付金额:</span>
            <span class="amount">¥{{ selectedOrder.actualFare || selectedOrder.estimatedFare }}</span>
          </div>
        </div>

        <div class="payment-methods">
          <h3>支付方式</h3>
          <el-radio-group v-model="selectedPaymentMethod">
            <el-radio label="WECHAT" class="payment-option">
              <div class="payment-method">
                <img class="custom-icon" src="/avatars/WeChat.jpg" alt="Logo" >
                <span>微信支付</span>
              </div>
            </el-radio>
            <el-radio label="ALIPAY" class="payment-option">
              <div class="payment-method">
                <img class="custom-icon" src="/avatars/AliPay.jpg" alt="Logo" >
                <span>支付宝支付</span>
              </div>
            </el-radio>
            <el-radio label="CREDIT_CARD" class="payment-option">
              <div class="payment-method">
                <img class="custom-icon" src="/avatars/Visa.jpg" alt="Logo" >
                <span>银行卡支付</span>
              </div>
            </el-radio>
          </el-radio-group>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="paymentDialogVisible = false">取消</el-button>
          <el-button 
            type="primary" 
            @click="confirmPayment"
            :loading="paymentLoading"
            :disabled="!selectedPaymentMethod"
          >
            确认支付
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Location, 
  Position, 
  Loading, 
  DocumentRemove, 
  SuccessFilled, 
  WarningFilled, 
  User,
  Star,
  StarFilled,
  Filter,
  Search,
  Refresh
} from '@element-plus/icons-vue'
import ReviewDialog from '@/components/ReviewDialog.vue'
import { useUserStore } from '@/stores/user'
import { useOrderStore } from '@/stores/order'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const orderStore = useOrderStore()
const router = useRouter()

// 响应式数据
const orders = ref([])
const loading = ref(false)
const paymentLoading = ref(false)
const paymentDialogVisible = ref(false)
const selectedOrder = ref(null)
const selectedPaymentMethod = ref('')

// 评价相关
const reviewDialogVisible = ref(false)
const selectedOrderForReview = ref(null)
const selectedDriverInfo = ref(null)

// 筛选和分页相关
const showFilters = ref(false)
const filters = ref({
  status: '',
  paymentStatus: '',
  dateRange: null,
  keyword: ''
})
const activeQuickFilter = ref('')
const sortBy = ref('time_desc')
const currentPage = ref(1)
const pageSize = ref(10)
const searchTimeout = ref(null)

// 快速筛选选项
const quickFilters = [
  { key: '', label: '全部订单' },
  { key: 'unpaid', label: '待支付' },
  { key: 'completed', label: '已完成' },
  { key: 'cancelled', label: '已取消' },
  { key: 'today', label: '今日订单' },
  { key: 'this_week', label: '本周订单' }
]

// 计算属性
const unpaidCount = computed(() => {
  return orders.value.filter(order => isUnpaid(order)).length
})

// 筛选后的订单
const filteredOrders = computed(() => {
  let result = [...orders.value]
  
  // 状态筛选
  if (filters.value.status) {
    result = result.filter(order => order.status === filters.value.status)
  }
  
  // 支付状态筛选
  if (filters.value.paymentStatus) {
    if (filters.value.paymentStatus === 'UNPAID') {
      result = result.filter(order => isUnpaid(order))
    } else {
      result = result.filter(order => order.paymentStatus === filters.value.paymentStatus)
    }
  }
  
  // 时间范围筛选
  if (filters.value.dateRange && filters.value.dateRange.length === 2) {
    const [startDate, endDate] = filters.value.dateRange
    result = result.filter(order => {
      const orderDate = new Date(order.createdAt).toISOString().split('T')[0]
      return orderDate >= startDate && orderDate <= endDate
    })
  }
  
  // 关键词搜索
  if (filters.value.keyword && filters.value.keyword.trim()) {
    const keyword = filters.value.keyword.trim().toLowerCase()
    result = result.filter(order => 
      order.orderNumber?.toLowerCase().includes(keyword) ||
      order.pickupAddress?.toLowerCase().includes(keyword) ||
      order.destinationAddress?.toLowerCase().includes(keyword)
    )
  }
  
  // 排序
  result.sort((a, b) => {
    switch (sortBy.value) {
      case 'time_asc':
        return new Date(a.createdAt) - new Date(b.createdAt)
      case 'time_desc':
        return new Date(b.createdAt) - new Date(a.createdAt)
      case 'fare_asc':
        return (a.actualFare || a.estimatedFare || 0) - (b.actualFare || b.estimatedFare || 0)
      case 'fare_desc':
        return (b.actualFare || b.estimatedFare || 0) - (a.actualFare || a.estimatedFare || 0)
      default:
        return new Date(b.createdAt) - new Date(a.createdAt)
    }
  })
  
  return result
})

// 分页后的订单
const paginatedOrders = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredOrders.value.slice(start, end)
})

// 页面加载时获取订单历史
onMounted(async () => {
  // 初始化订单状态
  await orderStore.initOrderState()
  // 加载订单历史
  loadOrderHistory()
})

// 加载订单历史
const loadOrderHistory = async () => {
  loading.value = true
  try {
    const passengerId = userStore.user.passengerId || userStore.user.id
    const response = await fetch(`/api/orders/passenger/${passengerId}/history`, {
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    
    const result = await response.json()
    
    if (response.ok && result.code === 200) {
      orders.value = result.data || []
      console.log('✅ 加载到', orders.value.length, '个历史订单')
      
      // 为已完成的订单检查评价状态
      await checkReviewStatus()
    } else {
      ElMessage.error('加载订单历史失败: ' + (result.message || '未知错误'))
    }
  } catch (error) {
    console.error('加载订单历史错误:', error)
    ElMessage.error('加载失败，请重试')
  } finally {
    loading.value = false
  }
}

// 检查订单评价状态
const checkReviewStatus = async () => {
  const completedOrders = orders.value.filter(order => order.status === 'COMPLETED')
  
  for (const order of completedOrders) {
    try {
      const response = await fetch(`/api/reviews/order/${order.id}/exists`)
      if (response.ok) {
        const result = await response.json()
        if (result.code === 200) {
          order.reviewStatus = result.data ? 'reviewed' : 'not_reviewed'
        }
      }
    } catch (error) {
      console.error('检查评价状态失败:', error)
      order.reviewStatus = 'not_reviewed'
    }
  }
}

// 判断订单是否未支付
const isUnpaid = (order) => {
  return order.status === 'COMPLETED' && order.paymentStatus !== 'PAID'
}

// 获取状态样式类
const getStatusClass = (status) => {
  const statusMap = {
    'SCHEDULED': 'scheduled',
    'PENDING': 'pending',
    'ASSIGNED': 'assigned', 
    'PICKUP': 'pickup',
    'IN_PROGRESS': 'in-progress',
    'COMPLETED': 'completed',
    'CANCELLED': 'cancelled'
  }
  return statusMap[status] || 'default'
}

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    'SCHEDULED': '预约中',
    'PENDING': '等待接单',
    'ASSIGNED': '司机已接单',
    'PICKUP': '司机已到达',
    'IN_PROGRESS': '行程中',
    'COMPLETED': '已完成',
    'CANCELLED': '已取消'
  }
  return statusMap[status] || '未知状态'
}

// 返回地图页面
const goToMap = () => {
  router.push('/dashboard/passenger-map')
}

// 取消当前订单
const cancelCurrentOrder = async () => {
  try {
    await ElMessageBox.confirm('确定要取消当前订单吗？', '确认取消', {
      confirmButtonText: '确定取消',
      cancelButtonText: '继续等待',
      type: 'warning',
    })
    
    const response = await fetch(`/api/orders/${orderStore.currentOrder.id}/cancel`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    
    const result = await response.json()
    
    if (response.ok && result.code === 200) {
      ElMessage.success('订单已取消')
      orderStore.clearOrderState()
      // 刷新历史订单
      loadOrderHistory()
    } else {
      ElMessage.error('取消失败: ' + (result.message || '未知错误'))
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('取消失败，请重试')
    }
  }
}

// 获取支付方式文本
const getPaymentMethodText = (method) => {
  const methodMap = {
    'WECHAT': '微信支付',
    'ALIPAY': '支付宝',
    'CASH': '现金',
    'CREDIT_CARD': '信用卡'
  }
  return methodMap[method] || method
}

// 获取支付状态文本
const getPaymentStatusText = (status) => {
  const statusMap = {
    'UNPAID': '未支付',
    'PAID': '已支付',
    'REFUNDED': '已退款'
  }
  return statusMap[status] || status
}

// 格式化时间
const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 显示支付对话框
const showPaymentDialog = (order) => {
  selectedOrder.value = order
  selectedPaymentMethod.value = ''
  paymentDialogVisible.value = true
}

// 确认支付
const confirmPayment = async () => {
  if (!selectedOrder.value || !selectedPaymentMethod.value) {
    ElMessage.warning('请选择支付方式')
    return
  }
  
  paymentLoading.value = true
  
  try {
    const response = await fetch(`/api/orders/${selectedOrder.value.id}/pay?paymentMethod=${selectedPaymentMethod.value}`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    
    const result = await response.json()
    
    if (response.ok && result.code === 200) {
      ElMessage.success('支付成功！')
      
      // 更新本地订单状态
      const orderIndex = orders.value.findIndex(o => o.id === selectedOrder.value.id)
      if (orderIndex !== -1) {
        orders.value[orderIndex].paymentStatus = 'PAID'
        orders.value[orderIndex].paymentMethod = selectedPaymentMethod.value
      }
      
      // 更新全局未支付订单状态
      orderStore.checkUnpaidOrders()
      
      paymentDialogVisible.value = false
    } else {
      ElMessage.error('支付失败: ' + (result.message || '未知错误'))
    }
  } catch (error) {
    console.error('支付错误:', error)
    ElMessage.error('支付失败，请重试')
  } finally {
    paymentLoading.value = false
  }
}

// 显示评价对话框
const showReviewDialog = async (order) => {
  selectedOrderForReview.value = order
  
  // 获取司机信息
  try {
    const response = await fetch(`/api/drivers/${order.driverId}`, {
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    
    if (response.ok) {
      const result = await response.json()
      if (result.code === 200) {
        selectedDriverInfo.value = result.data
        
        // 获取司机车辆信息
        const vehicleResponse = await fetch(`/api/vehicles/driver/${order.driverId}/active`)
        if (vehicleResponse.ok) {
          const vehicleResult = await vehicleResponse.json()
          if (vehicleResult.code === 200 && vehicleResult.data) {
            selectedDriverInfo.value.plateNumber = vehicleResult.data.plateNumber
            selectedDriverInfo.value.carModel = `${vehicleResult.data.brand} ${vehicleResult.data.model}`
          }
        }
      }
    }
  } catch (error) {
    console.error('获取司机信息失败:', error)
  }
  
  reviewDialogVisible.value = true
}

// 查看评价
const viewReview = async (order) => {
  try {
    const response = await fetch(`/api/reviews/order/${order.id}`)
    if (response.ok) {
      const result = await response.json()
      if (result.code === 200 && result.data) {
        const review = result.data
        ElMessageBox.alert(
          `评分: ${review.rating}分\n评价: ${review.comment || '无文字评价'}`,
          '我的评价',
          {
            confirmButtonText: '确定'
          }
        )
      }
    }
  } catch (error) {
    console.error('获取评价失败:', error)
    ElMessage.error('获取评价失败')
  }
}

// 处理评价提交成功
const handleReviewSubmitted = (review) => {
  // 更新本地订单状态
  const orderIndex = orders.value.findIndex(o => o.id === review.orderId)
  if (orderIndex !== -1) {
    orders.value[orderIndex].reviewStatus = 'reviewed'
  }
  
  ElMessage.success('感谢您的评价！')
}

// 筛选和分页相关方法
const toggleFilters = () => {
  showFilters.value = !showFilters.value
}

const applyFilters = () => {
  currentPage.value = 1 // 重置到第一页
  activeQuickFilter.value = '' // 清除快速筛选状态
}

const resetFilters = () => {
  filters.value = {
    status: '',
    paymentStatus: '',
    dateRange: null,
    keyword: ''
  }
  activeQuickFilter.value = ''
  currentPage.value = 1
  sortBy.value = 'time_desc'
}

const applyQuickFilter = (filterKey) => {
  // 重置其他筛选条件
  filters.value = {
    status: '',
    paymentStatus: '',
    dateRange: null,
    keyword: ''
  }
  
  activeQuickFilter.value = filterKey
  currentPage.value = 1
  
  const today = new Date()
  const todayStr = today.toISOString().split('T')[0]
  
  switch (filterKey) {
    case 'unpaid':
      filters.value.paymentStatus = 'UNPAID'
      break
    case 'completed':
      filters.value.status = 'COMPLETED'
      break
    case 'cancelled':
      filters.value.status = 'CANCELLED'
      break
    case 'today':
      filters.value.dateRange = [todayStr, todayStr]
      break
    case 'this_week':
      const weekStart = new Date(today)
      weekStart.setDate(today.getDate() - today.getDay())
      const weekStartStr = weekStart.toISOString().split('T')[0]
      filters.value.dateRange = [weekStartStr, todayStr]
      break
  }
}

const applySorting = () => {
  // 排序逻辑在计算属性中处理
}

const debounceSearch = () => {
  if (searchTimeout.value) {
    clearTimeout(searchTimeout.value)
  }
  searchTimeout.value = setTimeout(() => {
    applyFilters()
  }, 500)
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
}

const handleCurrentChange = (page) => {
  currentPage.value = page
}
</script>

<style scoped>
.my-trips {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  background: #f5f5f5;
  min-height: 100vh;
}

.current-order-section {
  margin-bottom: 30px;
}

.current-order-section h3 {
  color: #333;
  margin-bottom: 15px;
  font-size: 18px;
}

.current-order-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border-left: 4px solid #409EFF;
  position: relative;
}

.status-badge.current {
  position: absolute;
  top: 15px;
  right: 20px;
  padding: 6px 16px;
  border-radius: 15px;
  font-size: 14px;
  font-weight: bold;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% { opacity: 1; }
  50% { opacity: 0.7; }
  100% { opacity: 1; }
}

.current-order-actions {
  margin-top: 20px;
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.driver-info {
  margin-top: 15px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 8px;
}

.driver-info h4 {
  margin: 0 0 10px 0;
  color: #333;
  font-size: 14px;
}

.driver-details {
  display: flex;
  flex-direction: column;
  gap: 5px;
  font-size: 14px;
  color: #666;
}

.header {
  background: white;
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.header h1 {
  margin: 0 0 10px 0;
  color: #333;
  font-size: 24px;
}

.stats {
  display: flex;
  gap: 20px;
  font-size: 14px;
  color: #666;
}

.unpaid-count {
  color: #f56c6c;
  font-weight: bold;
}

.orders-container {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.review-section {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #f0f0f0;
}

.review-status {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #67c23a;
  font-size: 14px;
}

.review-actions {
  display: flex;
  justify-content: flex-end;
}

.loading, .empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #999;
}

.loading .el-icon {
  font-size: 32px;
  margin-bottom: 10px;
}

.empty .el-icon {
  font-size: 48px;
  margin-bottom: 15px;
}

.orders-list {
  padding: 0;
}

.order-item {
  position: relative;
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
  transition: background-color 0.3s;
}

.order-item:hover {
  background: #fafafa;
}

.order-item:last-child {
  border-bottom: none;
}

.order-item.unpaid {
  border-left: 4px solid #f56c6c;
  background: #fef7f7;
}

.status-badge {
  position: absolute;
  top: 15px;
  right: 20px;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: bold;
}

.status-badge.scheduled {
  background: #e2e3ff;
  color: #5a67d8;
}

.status-badge.pending {
  background: #fff3cd;
  color: #856404;
}

.status-badge.assigned {
  background: #d1ecf1;
  color: #0c5460;
}

.status-badge.pickup {
  background: #d4edda;
  color: #155724;
}

.status-badge.in-progress {
  background: #cce5ff;
  color: #004085;
}

.status-badge.completed {
  background: #d4edda;
  color: #155724;
}

.status-badge.cancelled {
  background: #f8d7da;
  color: #721c24;
}

.order-header {
  margin-bottom: 15px;
  padding-right: 100px;
}

.order-number {
  font-weight: bold;
  color: #333;
  margin-bottom: 5px;
}

.order-time {
  font-size: 14px;
  color: #666;
}

.trip-info {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
  gap: 15px;
}

.location-item {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}

.pickup-icon {
  color: #28a745;
  font-size: 18px;
}

.destination-icon {
  color: #dc3545;
  font-size: 18px;
}

.location-text .label {
  font-size: 12px;
  color: #999;
  margin-bottom: 2px;
}

.location-text .address {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.location-divider {
  width: 2px;
  height: 30px;
  background: #ddd;
  margin: 0 10px;
}

.fare-info {
  display: flex;
  gap: 20px;
  margin-bottom: 15px;
}

.fare-item {
  display: flex;
  align-items: center;
  gap: 5px;
}

.fare-item .label {
  font-size: 14px;
  color: #666;
}

.fare-item .amount {
  font-weight: bold;
  color: #333;
}

.fare-item .amount.actual {
  color: #f56c6c;
}

.payment-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.payment-status {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 14px;
}

.payment-status.paid {
  color: #28a745;
}

.payment-status.paid .el-icon {
  color: #28a745;
}

.payment-actions {
  display: flex;
  align-items: center;
  gap: 15px;
}

.unpaid-notice {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #f56c6c;
  font-size: 14px;
}

.driver-info {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #999;
}

.payment-dialog .order-summary {
  margin-bottom: 20px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 8px;
}

.payment-dialog h3 {
  margin: 0 0 15px 0;
  color: #333;
  font-size: 16px;
}

.summary-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 14px;
}

.summary-item:last-child {
  margin-bottom: 0;
}

.summary-item.total {
  font-weight: bold;
  font-size: 16px;
  padding-top: 8px;
  border-top: 1px solid #ddd;
}

.summary-item.total .amount {
  color: #f56c6c;
}

.payment-methods .el-radio-group {
  width: 100%;
}

.payment-option {
  width: 100%;
  margin-bottom: 20px;
}

.payment-method {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 8px;
  transition: all 0.3s;
}

.payment-option.is-checked .payment-method {
  border-color: #409EFF;
  background: #f0f8ff;
}

.payment-method .icon {
  font-size: 20px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* 筛选相关样式 */
.filter-section {
  margin-bottom: 20px;
}

.filter-card {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.filter-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.filter-header h3 {
  margin: 0;
  color: #333;
  font-size: 18px;
}

.toggle-filters {
  color: #409EFF;
  font-size: 14px;
}

.filters-content {
  border-top: 1px solid #f0f0f0;
  padding-top: 15px;
}

.filter-form {
  margin-bottom: 15px;
}

.quick-filters {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.quick-filter-label {
  font-size: 14px;
  color: #666;
  margin-right: 8px;
}

.quick-filter-tag {
  cursor: pointer;
  transition: all 0.3s;
}

.quick-filter-tag:hover {
  transform: translateY(-1px);
}

.orders-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

.orders-count {
  font-size: 14px;
  color: #666;
}

.filter-tip {
  color: #409EFF;
  font-weight: bold;
}

.sort-options {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pagination-container {
  display: flex;
  justify-content: center;
  padding: 20px;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
}

@media (max-width: 768px) {
  .my-trips {
    padding: 10px;
  }
  
  .filter-form {
    flex-direction: column;
  }
  
  .filter-form .el-form-item {
    margin-right: 0;
    margin-bottom: 15px;
  }
  
  .orders-header {
    flex-direction: column;
    gap: 10px;
    align-items: flex-start;
  }
  
  .quick-filters {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .trip-info {
    flex-direction: column;
    gap: 10px;
  }
  
  .location-divider {
    width: 30px;
    height: 2px;
    margin: 10px 0;
  }
  
  .fare-info {
    flex-direction: column;
    gap: 5px;
  }
  
  .payment-section {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}
.custom-icon {
  width: 30px;  /* 调整宽度 */
  height: 30px; /* 调整高度 */
}
</style>