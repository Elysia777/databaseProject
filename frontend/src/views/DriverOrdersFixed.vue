<template>
  <div class="driver-orders">
    <div class="page-header">
      <h2>我的订单</h2>
      <div class="header-actions">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索订单号或地址"
          style="width: 200px;"
          clearable
          @change="handleSearch">
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="statusFilter" placeholder="筛选状态" clearable @change="loadOrders" style="width: 150px;">
          <el-option label="全部" value="" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已退款" value="COMPLETED_REFUNDED" />
          <el-option label="已取消" value="CANCELLED" />
          <el-option label="进行中" value="IN_PROGRESS" />
          <el-option label="已接单" value="ASSIGNED" />
          <el-option label="已到达" value="PICKUP" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          @change="loadOrders"
          style="width: 240px;">
        </el-date-picker>
        <el-button @click="loadOrders" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button @click="showAdvancedSearch = true" type="primary" plain>
          <el-icon><Filter /></el-icon>
          高级筛选
        </el-button>
      </div>
    </div>

         <!-- 统计信息栏 -->
     <div class="stats-bar" v-if="!loading && orders.length > 0">
       <div class="stat-item">
         <span class="stat-label">本页订单:</span>
         <span class="stat-value">{{ orders.length }}</span>
       </div>
       <div class="stat-item">
         <span class="stat-label">已完成:</span>
         <span class="stat-value">{{ completedCount }}</span>
       </div>
       <div class="stat-item">
         <span class="stat-label">已退款:</span>
         <span class="stat-value">{{ refundedCount }}</span>
       </div>
       <div class="stat-item">
         <span class="stat-label">本页收入:</span>
         <span class="stat-value">¥{{ totalEarnings.toFixed(2) }}</span>
       </div>
       <div class="stat-item">
         <span class="stat-label">平均收入:</span>
         <span class="stat-value">¥{{ averageEarnings.toFixed(2) }}</span>
       </div>
     </div>

    <div class="orders-container">
      <el-card v-if="loading" class="loading-card">
        <div class="loading-content">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>加载中...</span>
        </div>
      </el-card>

      <div v-else-if="orders.length === 0" class="empty-state">
        <el-empty description="暂无订单记录" />
      </div>

      <div v-else class="orders-list">
        <el-card v-for="order in orders" :key="order.id" class="order-card" @click="showOrderDetails(order)">
          <div class="order-header">
            <div class="order-info">
              <span class="order-number">#{{ order.orderNumber || order.id }}</span>
                             <el-tag :type="getStatusTagType(order.status, order.paymentStatus)" size="small">
                 {{ getStatusText(order.status, order.paymentStatus) }}
               </el-tag>
              <el-tag v-if="order.orderType === 'RESERVATION'" type="warning" size="small">
                预约单
              </el-tag>
              <el-tag v-else-if="order.orderType === 'REAL_TIME'" type="primary" size="small">
                实时单
              </el-tag>
            </div>
            <div class="order-time">
              {{ formatDateTime(order.createdAt) }}
            </div>
          </div>

          <div class="order-content">
            <div class="route-info">
              <div class="location-item">
                <el-icon class="location-icon pickup"><LocationFilled /></el-icon>
                <span class="address">{{ order.pickupAddress || '上车地点' }}</span>
              </div>
              <div class="route-line"></div>
              <div class="location-item">
                <el-icon class="location-icon destination"><Location /></el-icon>
                <span class="address">{{ order.destinationAddress || '目的地' }}</span>
              </div>
            </div>

            <div class="order-details">
              <div class="detail-row">
                <span class="label">距离:</span>
                <span class="value">{{ (order.estimatedDistance || 0).toFixed(1) }} 公里</span>
              </div>
              <div class="detail-row">
                <span class="label">时长:</span>
                <span class="value">{{ order.estimatedDuration || '--' }} 分钟</span>
              </div>
              <div class="detail-row">
                <span class="label">费用:</span>
                <span class="value price">¥{{ (order.actualFare || order.estimatedFare || 0).toFixed(2) }}</span>
              </div>
              <div v-if="order.scheduledTime" class="detail-row">
                <span class="label">预约时间:</span>
                <span class="value">{{ formatDateTime(order.scheduledTime) }}</span>
              </div>
              <div v-if="order.completionTime" class="detail-row">
                <span class="label">完成时间:</span>
                <span class="value">{{ formatDateTime(order.completionTime) }}</span>
              </div>
              <div v-if="order.cancelReason" class="detail-row">
                <span class="label">取消原因:</span>
                <span class="value cancel-reason">{{ order.cancelReason }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </div>

      <div v-if="orders.length > 0" class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="totalOrders"
          layout="prev, pager, next, jumper"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 高级搜索对话框 -->
    <el-dialog v-model="showAdvancedSearch" title="高级筛选" width="600px">
      <el-form :model="advancedFilters" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="订单类型">
              <el-select v-model="advancedFilters.orderType" placeholder="选择类型" clearable>
                <el-option label="全部" value="" />
                <el-option label="实时订单" value="REAL_TIME" />
                <el-option label="预约订单" value="RESERVATION" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="费用范围">
              <el-input-number v-model="advancedFilters.minFare" :min="0" :precision="2" placeholder="最低费用" style="width: 200px;" />
              <span style="margin: 0 8px;">-</span>
              <el-input-number v-model="advancedFilters.maxFare" :min="0" :precision="2" placeholder="最高费用" style="width: 200px;" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="起点地址">
          <el-input v-model="advancedFilters.pickupAddress" placeholder="输入起点地址关键词" />
        </el-form-item>
        <el-form-item label="终点地址">
          <el-input v-model="advancedFilters.destinationAddress" placeholder="输入终点地址关键词" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="resetAdvancedFilters">重置</el-button>
          <el-button @click="showAdvancedSearch = false">取消</el-button>
          <el-button type="primary" @click="applyAdvancedFilters">应用筛选</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 订单详情对话框 -->
    <el-dialog v-model="showOrderDetail" title="订单详情" width="700px">
      <div v-if="selectedOrder" class="order-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ selectedOrder.orderNumber || selectedOrder.id }}</el-descriptions-item>
                     <el-descriptions-item label="订单状态">
             <el-tag :type="getStatusTagType(selectedOrder.status, selectedOrder.paymentStatus)">
               {{ getStatusText(selectedOrder.status, selectedOrder.paymentStatus) }}
             </el-tag>
           </el-descriptions-item>
          <el-descriptions-item label="订单类型">
            <el-tag v-if="selectedOrder.orderType === 'RESERVATION'" type="warning">预约单</el-tag>
            <el-tag v-else-if="selectedOrder.orderType === 'REAL_TIME'" type="primary">实时单</el-tag>
            <el-tag v-else type="info">未知类型</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(selectedOrder.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="起点地址" :span="2">{{ selectedOrder.pickupAddress || '未知' }}</el-descriptions-item>
          <el-descriptions-item label="终点地址" :span="2">{{ selectedOrder.destinationAddress || '未知' }}</el-descriptions-item>
          <el-descriptions-item label="预估距离">{{ (selectedOrder.estimatedDistance || 0).toFixed(1) }} 公里</el-descriptions-item>
          <el-descriptions-item label="预估时长">{{ selectedOrder.estimatedDuration || '--' }} 分钟</el-descriptions-item>
          <el-descriptions-item label="预估费用">¥{{ (selectedOrder.estimatedFare || 0).toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="实际费用">¥{{ (selectedOrder.actualFare || selectedOrder.estimatedFare || 0).toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item v-if="selectedOrder.scheduledTime" label="预约时间" :span="2">
            {{ formatDateTime(selectedOrder.scheduledTime) }}
          </el-descriptions-item>
          <el-descriptions-item v-if="selectedOrder.pickupTime" label="接客时间">
            {{ formatDateTime(selectedOrder.pickupTime) }}
          </el-descriptions-item>
          <el-descriptions-item v-if="selectedOrder.completionTime" label="完成时间">
            {{ formatDateTime(selectedOrder.completionTime) }}
          </el-descriptions-item>
          <el-descriptions-item v-if="selectedOrder.cancelReason" label="取消原因" :span="2">
            <span class="cancel-reason">{{ selectedOrder.cancelReason }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Loading, LocationFilled, Location, Search, Filter } from '@element-plus/icons-vue'

// 从localStorage获取用户信息
const getUserInfo = () => {
  try {
    const userInfo = localStorage.getItem('user')
    if (userInfo) {
      return JSON.parse(userInfo)
    }
  } catch (error) {
    console.error('解析用户信息失败:', error)
  }
  return null
}

const mockUserStore = {
  user: getUserInfo() || { id: 1, driverId: 1 },
  token: localStorage.getItem('token') || ''
}

// 响应式数据
const orders = ref([])
const loading = ref(false)
const statusFilter = ref('')
const searchKeyword = ref('')
const dateRange = ref(null)
const currentPage = ref(1)
const pageSize = ref(20)
const totalOrders = ref(0)

// 对话框状态
const showAdvancedSearch = ref(false)
const showOrderDetail = ref(false)
const selectedOrder = ref(null)

// 高级筛选
const advancedFilters = ref({
  orderType: '',
  minFare: null,
  maxFare: null,
  pickupAddress: '',
  destinationAddress: ''
})

// 计算属性
const driverId = computed(() => {
  const id = mockUserStore.user?.driverId || mockUserStore.user?.id || 1
  console.log('当前用户信息:', mockUserStore.user)
  console.log('使用的driverId:', id)
  return id
})

// 统计信息
const completedCount = computed(() => {
  return orders.value.filter(order => 
    order.status === 'COMPLETED' && order.paymentStatus !== 'REFUNDED'
  ).length
})

const totalEarnings = computed(() => {
  return orders.value
    .filter(order => order.status === 'COMPLETED' && order.paymentStatus !== 'REFUNDED')
    .reduce((sum, order) => sum + (parseFloat(order.actualFare || order.estimatedFare || 0)), 0)
})

// 添加退款订单统计
const refundedCount = computed(() => {
  return orders.value.filter(order => 
    order.status === 'COMPLETED' && order.paymentStatus === 'REFUNDED'
  ).length
})

const averageEarnings = computed(() => {
  const completed = completedCount.value
  return completed > 0 ? totalEarnings.value / completed : 0
})

// 加载订单列表
const loadOrders = async () => {
  loading.value = true
  try {
    const params = new URLSearchParams({
      page: currentPage.value.toString(),
      size: pageSize.value.toString()
    })
    
         if (statusFilter.value) {
       if (statusFilter.value === 'COMPLETED_REFUNDED') {
         // 已退款状态：订单状态为已完成，支付状态为已退款
         params.append('status', 'COMPLETED')
         params.append('paymentStatus', 'REFUNDED')
       } else if (statusFilter.value === 'COMPLETED') {
         // 已完成状态：订单状态为已完成，支付状态不为已退款
         params.append('status', 'COMPLETED')
         params.append('paymentStatus', 'NOT_REFUNDED')
       } else {
         params.append('status', statusFilter.value)
       }
     }

    // 添加日期范围筛选
    if (dateRange.value && dateRange.value.length === 2) {
      params.append('startDate', dateRange.value[0])
      params.append('endDate', dateRange.value[1])
    }

    const token = localStorage.getItem('token')
    if (!token) {
      ElMessage.warning('请先登录')
      // 生成更多模拟数据用于展示
      let mockOrders = [
        {
          id: 1,
          orderNumber: 'ORD20250813001',
          status: 'COMPLETED',
          orderType: 'IMMEDIATE',
          pickupAddress: '大连市中山区人民路100号',
          destinationAddress: '大连市沙河口区西安路商业街',
          estimatedDistance: 8.5,
          estimatedDuration: 25,
          actualFare: 32.50,
          createdAt: '2025-08-13T09:30:00',
          completionTime: '2025-08-13T09:55:00'
        },
        {
          id: 2,
          orderNumber: 'ORD20250813002',
          status: 'COMPLETED',
          orderType: 'RESERVATION',
          pickupAddress: '大连市甘井子区大连理工大学',
          destinationAddress: '大连周水子国际机场',
          estimatedDistance: 15.3,
          estimatedDuration: 35,
          actualFare: 58.80,
          scheduledTime: '2025-08-13T14:00:00',
          createdAt: '2025-08-13T12:15:00',
          completionTime: '2025-08-13T14:35:00'
        },
        {
          id: 3,
          orderNumber: 'ORD20250813003',
          status: 'CANCELLED',
          orderType: 'IMMEDIATE',
          pickupAddress: '大连市西岗区五四广场',
          destinationAddress: '大连市金州区开发区',
          estimatedDistance: 22.1,
          estimatedDuration: 45,
          estimatedFare: 78.00,
          createdAt: '2025-08-13T16:20:00',
          cancelReason: '乘客取消'
        },
        {
          id: 4,
          orderNumber: 'ORD20250812001',
          status: 'COMPLETED',
          orderType: 'IMMEDIATE',
          pickupAddress: '大连市中山区友好广场',
          destinationAddress: '大连火车站',
          estimatedDistance: 6.2,
          estimatedDuration: 18,
          actualFare: 24.50,
          createdAt: '2025-08-12T20:10:00',
          completionTime: '2025-08-12T20:28:00'
        },
        {
          id: 5,
          orderNumber: 'ORD20250812002',
          status: 'COMPLETED',
          orderType: 'RESERVATION',
          pickupAddress: '大连市沙河口区星海广场',
          destinationAddress: '大连市旅顺口区',
          estimatedDistance: 35.8,
          estimatedDuration: 65,
          actualFare: 125.60,
          scheduledTime: '2025-08-12T08:00:00',
          createdAt: '2025-08-11T18:30:00',
          completionTime: '2025-08-12T09:05:00'
        }
      ]

      // 应用筛选
      if (statusFilter.value) {
        mockOrders = mockOrders.filter(order => order.status === statusFilter.value)
      }

      if (searchKeyword.value) {
        const keyword = searchKeyword.value.toLowerCase()
        mockOrders = mockOrders.filter(order => 
          (order.orderNumber && order.orderNumber.toLowerCase().includes(keyword)) ||
          (order.pickupAddress && order.pickupAddress.toLowerCase().includes(keyword)) ||
          (order.destinationAddress && order.destinationAddress.toLowerCase().includes(keyword))
        )
      }

      orders.value = mockOrders
      totalOrders.value = mockOrders.length
      return
    }

    // 添加高级筛选参数
    if (searchKeyword.value) {
      params.append('keyword', searchKeyword.value)
    }
    
         const filters = advancedFilters.value
     if (filters.orderType) {
       params.append('orderType', filters.orderType)
     }
     if (filters.minFare != null && filters.minFare > 0) {
       params.append('minFare', filters.minFare.toString())
     }
     if (filters.maxFare != null && filters.maxFare > 0) {
       params.append('maxFare', filters.maxFare.toString())
     }
     if (filters.pickupAddress) {
       params.append('pickupKeyword', filters.pickupAddress)
     }
     if (filters.destinationAddress) {
       params.append('destinationKeyword', filters.destinationAddress)
     }

    const response = await fetch(`/api/drivers/${driverId.value}/orders?${params}`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })

    const result = await response.json()
    
    if (result.code === 200) {
      orders.value = result.data || []
      totalOrders.value = orders.value.length >= pageSize.value ? 
        (currentPage.value * pageSize.value + 1) : 
        ((currentPage.value - 1) * pageSize.value + orders.value.length)
    } else {
      ElMessage.error(result.message || '加载订单失败')
    }
  } catch (error) {
    console.error('加载订单失败:', error)
    ElMessage.error('网络错误，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 处理分页变化
const handlePageChange = (page) => {
  currentPage.value = page
  loadOrders()
}

// 处理搜索
const handleSearch = () => {
  currentPage.value = 1
  loadOrders()
}

// 显示订单详情
const showOrderDetails = (order) => {
  selectedOrder.value = order
  showOrderDetail.value = true
}

// 应用高级筛选
const applyAdvancedFilters = () => {
  showAdvancedSearch.value = false
  currentPage.value = 1
  loadOrders()
  ElMessage.success('高级筛选已应用')
}

// 重置高级筛选
const resetAdvancedFilters = () => {
  advancedFilters.value = {
    orderType: '',
    minFare: null,
    maxFare: null,
    pickupAddress: '',
    destinationAddress: ''
  }
}

// 获取状态标签类型
const getStatusTagType = (status, paymentStatus) => {
  if (!status) return 'info'
  
  // 如果订单已完成但已退款，显示退款标签
  if (status === 'COMPLETED' && paymentStatus === 'REFUNDED') {
    return 'warning'
  }
  
  const statusMap = {
    'COMPLETED': 'success',
    'CANCELLED': 'danger',
    'IN_PROGRESS': 'primary',
    'ASSIGNED': 'warning',
    'PICKUP': 'info',
    'PENDING': 'warning',
    'SCHEDULED': 'info'
  }
  return statusMap[status] || 'info'
}

// 获取状态文本
const getStatusText = (status, paymentStatus) => {
  // 如果订单已完成但已退款，显示退款状态
  if (status === 'COMPLETED' && paymentStatus === 'REFUNDED') {
    return '已退款'
  }
  
  const statusMap = {
    'SCHEDULED': '已预约',
    'PENDING': '待接单',
    'ASSIGNED': '已接单',
    'PICKUP': '已到达',
    'IN_PROGRESS': '进行中',
    'COMPLETED': '已完成',
    'CANCELLED': '已取消'
  }
  return statusMap[status] || status
}

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return '--'
  try {
    return new Date(dateTime).toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch (error) {
    return '--'
  }
}

// 页面加载时获取订单
onMounted(() => {
  loadOrders()
})
</script>

<style scoped>
.driver-orders {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 0 4px;
}

.page-header h2 {
  margin: 0;
  color: #333;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.stats-bar {
  display: flex;
  gap: 24px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stat-label {
  color: #666;
  font-size: 14px;
}

.stat-value {
  color: #333;
  font-weight: 600;
  font-size: 16px;
}

.orders-container {
  min-height: 400px;
}

.loading-card {
  text-align: center;
  padding: 40px;
}

.loading-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #666;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.orders-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-card {
  transition: all 0.3s ease;
  cursor: pointer;
}

.order-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.order-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.order-number {
  font-weight: 600;
  color: #333;
  font-size: 16px;
}

.order-time {
  color: #666;
  font-size: 14px;
}

.order-content {
  display: flex;
  gap: 24px;
}

.route-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.location-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.location-icon {
  font-size: 16px;
}

.location-icon.pickup {
  color: #67c23a;
}

.location-icon.destination {
  color: #e6a23c;
}

.address {
  color: #333;
  font-size: 14px;
}

.route-line {
  width: 2px;
  height: 20px;
  background: linear-gradient(to bottom, #67c23a, #e6a23c);
  margin-left: 7px;
}

.order-details {
  flex: 0 0 200px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
}

.label {
  color: #666;
}

.value {
  color: #333;
  font-weight: 500;
}

.value.price {
  color: #f56c6c;
  font-weight: 600;
}

.value.cancel-reason {
  color: #f56c6c;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding: 20px 0;
}

.order-detail .cancel-reason {
  color: #f56c6c;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }

  .header-actions {
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .stats-bar {
    flex-direction: column;
    gap: 12px;
  }

  .order-content {
    flex-direction: column;
    gap: 16px;
  }

  .order-details {
    flex: none;
  }
}
</style>