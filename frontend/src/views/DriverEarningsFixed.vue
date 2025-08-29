<template>
  <div class="driver-earnings">
    <div class="page-header">
      <h2>收入统计</h2>
      <div class="date-selector">
        <el-date-picker
          v-model="selectedDate"
          type="month"
          placeholder="选择月份"
          format="YYYY年MM月"
          value-format="YYYY-MM"
          @change="loadEarningsData"
        />
        <el-button type="primary" @click="loadEarningsData" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 收入概览卡片 -->
    <div class="earnings-overview">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card class="earnings-card">
            <div class="card-content">
              <div class="card-icon total">
                <el-icon><Money /></el-icon>
              </div>
              <div class="card-info">
                <div class="card-value">¥{{ earningsData.totalEarnings.toFixed(2) }}</div>
                <div class="card-label">总收入</div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :span="6">
          <el-card class="earnings-card">
            <div class="card-content">
              <div class="card-icon orders">
                <el-icon><Document /></el-icon>
              </div>
              <div class="card-info">
                <div class="card-value">{{ earningsData.totalOrders }}</div>
                <div class="card-label">完成订单</div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :span="6">
          <el-card class="earnings-card">
            <div class="card-content">
              <div class="card-icon average">
                <el-icon><TrendCharts /></el-icon>
              </div>
              <div class="card-info">
                <div class="card-value">¥{{ earningsData.averageEarnings.toFixed(2) }}</div>
                <div class="card-label">平均单价</div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :span="6">
          <el-card class="earnings-card">
            <div class="card-content">
              <div class="card-icon distance">
                <el-icon><Location /></el-icon>
              </div>
              <div class="card-info">
                <div class="card-value">{{ earningsData.totalDistance.toFixed(1) }}km</div>
                <div class="card-label">总里程</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 订单列表 -->
    <el-card class="orders-card">
      <template #header>
        <div class="card-header">
          <span>{{ selectedDate }} 月收入明细</span>
          <div class="header-actions">
            <span class="total-summary">
              共 {{ earningsData.totalOrders }} 单，总收入 ¥{{ earningsData.totalEarnings.toFixed(2) }}
            </span>
          </div>
        </div>
      </template>
      
      <el-table 
        :data="ordersList" 
        v-loading="loading"
        stripe
        style="width: 100%"
        empty-text="暂无订单记录"
      >
        <el-table-column prop="orderNumber" label="订单号" width="150">
          <template #default="{ row }">
            #{{ row.orderNumber || row.id }}
          </template>
        </el-table-column>
        
        <el-table-column prop="createdAt" label="完成时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.completionTime || row.createdAt) }}
          </template>
        </el-table-column>
        
        <el-table-column label="路线" min-width="200">
          <template #default="{ row }">
            <div class="route-info">
              <div class="route-item">
                <el-icon class="pickup-icon"><LocationFilled /></el-icon>
                {{ row.pickupAddress || '上车地点' }}
              </div>
              <div class="route-item">
                <el-icon class="destination-icon"><Location /></el-icon>
                {{ row.destinationAddress || '目的地' }}
              </div>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="estimatedDistance" label="距离(km)" width="100" align="center">
          <template #default="{ row }">
            {{ (row.estimatedDistance || 0).toFixed(1) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="actualFare" label="收入" width="120" align="center">
          <template #default="{ row }">
            <span class="earnings-amount">¥{{ (row.actualFare || row.estimatedFare || 0).toFixed(2) }}</span>
          </template>
        </el-table-column>
        
        <el-table-column label="订单类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.orderType === 'RESERVATION'" type="warning" size="small">
              预约单
            </el-tag>
            <el-tag v-else-if="row.orderType === 'IMMEDIATE'" type="primary" size="small">
              即时单
            </el-tag>
            <el-tag v-else type="info" size="small">
              普通单
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="totalOrders"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handlePageSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Money, Document, TrendCharts, Location, LocationFilled } from '@element-plus/icons-vue'

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
const loading = ref(false)
const selectedDate = ref(new Date().toISOString().slice(0, 7)) // 当前月份
const currentPage = ref(1)
const pageSize = ref(20)
const totalOrders = ref(0)

// 收入数据
const earningsData = reactive({
  totalEarnings: 0,
  totalOrders: 0,
  averageEarnings: 0,
  totalDistance: 0
})

const ordersList = ref([])

// 计算属性
const driverId = computed(() => {
  const id = mockUserStore.user?.driverId || mockUserStore.user?.id || 1
  console.log('当前用户信息:', mockUserStore.user)
  console.log('使用的driverId:', id)
  return id
})

// 加载收入数据
const loadEarningsData = async () => {
  loading.value = true
  try {
    const token = localStorage.getItem('token')
    if (!token) {
      ElMessage.warning('请先登录')
      // 添加模拟数据用于展示
      Object.assign(earningsData, {
        totalEarnings: 1256.80,
        totalOrders: 28,
        averageEarnings: 44.89,
        totalDistance: 342.6
      })
      
      ordersList.value = [
        {
          id: 1,
          orderNumber: 'ORD20250805001',
          status: 'COMPLETED',
          orderType: 'IMMEDIATE',
          pickupAddress: '北京市朝阳区三里屯SOHO',
          destinationAddress: '北京市海淀区中关村软件园',
          estimatedDistance: 12.5,
          estimatedDuration: 35,
          actualFare: 45.80,
          createdAt: '2025-08-05T10:30:00',
          completionTime: '2025-08-05T11:05:00'
        },
        {
          id: 2,
          orderNumber: 'ORD20250805002',
          status: 'COMPLETED',
          orderType: 'RESERVATION',
          pickupAddress: '北京市东城区王府井大街',
          destinationAddress: '北京首都国际机场T3航站楼',
          estimatedDistance: 28.3,
          estimatedDuration: 55,
          actualFare: 89.50,
          createdAt: '2025-08-05T14:15:00',
          completionTime: '2025-08-05T15:10:00'
        }
      ]
      totalOrders.value = 2
      return
    }

    // 先获取收入统计数据
    await loadEarningsStatistics()
    
    // 再获取订单列表
    await loadOrdersList()
  } catch (error) {
    console.error('加载收入数据失败:', error)
    ElMessage.error('网络错误，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 加载收入统计数据
const loadEarningsStatistics = async () => {
  const params = new URLSearchParams({
    month: selectedDate.value
  })
  
  try {
    const response = await fetch(`/api/drivers/${driverId.value}/earnings?${params}`, {
      headers: {
        'Authorization': `Bearer ${mockUserStore.token}`
      }
    })
    
    const result = await response.json()
    
    if (result.code === 200 && result.data && result.data.summary) {
      Object.assign(earningsData, {
        totalEarnings: result.data.summary.totalEarnings || 0,
        totalOrders: result.data.summary.totalOrders || 0,
        averageEarnings: result.data.summary.averageEarnings || 0,
        totalDistance: result.data.summary.totalDistance || 0
      })
    }
  } catch (error) {
    console.error('加载收入统计失败:', error)
    // 如果统计接口失败，从订单列表计算
  }
}

// 加载订单列表
const loadOrdersList = async () => {
  const params = new URLSearchParams({
    page: currentPage.value.toString(),
    size: pageSize.value.toString(),
    status: 'COMPLETED', // 只显示已完成且未退款的订单
    paymentStatus: 'NOT_REFUNDED' // 明确指定不包含退款订单
  })
  
  if (selectedDate.value) {
    const startDate = `${selectedDate.value}-01`
    const endDate = new Date(selectedDate.value + '-01')
    endDate.setMonth(endDate.getMonth() + 1)
    endDate.setDate(0)
    params.append('startDate', startDate)
    params.append('endDate', endDate.toISOString().slice(0, 10))
  }
  
  const response = await fetch(`/api/drivers/${driverId.value}/orders?${params}`, {
    headers: {
      'Authorization': `Bearer ${mockUserStore.token}`
    }
  })
  
  const result = await response.json()
  
  if (result.code === 200) {
    const orders = result.data || []
    ordersList.value = orders
    
    // 如果没有统计数据，从订单计算
    if (earningsData.totalEarnings === 0 && orders.length > 0) {
      calculateEarningsData(orders)
    }
    
    // 设置总数
    totalOrders.value = orders.length >= pageSize.value ? 
      (currentPage.value * pageSize.value + 1) : 
      ((currentPage.value - 1) * pageSize.value + orders.length)
  } else {
    ElMessage.error(result.message || '加载订单数据失败')
  }
}

// 计算收入统计数据
const calculateEarningsData = (orders) => {
  // 只统计已完成且未退款的订单
  const validOrders = orders.filter(order => 
    order.status === 'COMPLETED' && order.paymentStatus !== 'REFUNDED'
  )
  
  // 统计退款订单数量（用于显示）
  const refundedOrders = orders.filter(order => 
    order.status === 'COMPLETED' && order.paymentStatus === 'REFUNDED'
  )
  
  const totalEarnings = validOrders.reduce((sum, order) => {
    return sum + (parseFloat(order.actualFare) || parseFloat(order.estimatedFare) || 0)
  }, 0)
  
  const totalDistance = validOrders.reduce((sum, order) => {
    return sum + (parseFloat(order.estimatedDistance) || 0)
  }, 0)
  
  const averageEarnings = validOrders.length > 0 ? totalEarnings / validOrders.length : 0
  
  console.log('订单统计:', {
    totalOrders: orders.length,
    validOrders: validOrders.length,
    refundedOrders: refundedOrders.length,
    totalEarnings,
    totalDistance
  })
  
  Object.assign(earningsData, {
    totalEarnings,
    totalOrders: validOrders.length,
    averageEarnings,
    totalDistance,
    refundedOrders: refundedOrders.length
  })
}

// 处理分页变化
const handlePageChange = (page) => {
  currentPage.value = page
  loadEarningsData()
}

const handlePageSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadEarningsData()
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

// 组件挂载时加载数据
onMounted(() => {
  loadEarningsData()
})
</script>

<style scoped>
.driver-earnings {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  color: #333;
}

.date-selector {
  display: flex;
  gap: 10px;
  align-items: center;
}

.earnings-overview {
  margin-bottom: 20px;
}

.earnings-card {
  height: 120px;
}

.card-content {
  display: flex;
  align-items: center;
  height: 100%;
}

.card-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
  font-size: 24px;
  color: white;
}

.card-icon.total {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.card-icon.orders {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.card-icon.average {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.card-icon.distance {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.card-info {
  flex: 1;
}

.card-value {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin-bottom: 5px;
}

.card-label {
  font-size: 14px;
  color: #666;
}

.orders-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.total-summary {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.route-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.route-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.pickup-icon {
  color: #67c23a;
}

.destination-icon {
  color: #e6a23c;
}

.earnings-amount {
  color: #67C23A;
  font-weight: bold;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

@media (max-width: 768px) {
  .earnings-overview .el-col {
    margin-bottom: 10px;
  }
  
  .page-header {
    flex-direction: column;
    gap: 15px;
    align-items: stretch;
  }
  
  .date-selector {
    justify-content: center;
  }
}
</style>