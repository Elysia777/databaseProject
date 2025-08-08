<template>
  <div class="driver-earnings">
    <div class="page-header">
      <h2>收入统计</h2>
      <div class="date-selector">
        <div class="quick-months">
          <el-button 
            v-for="month in quickMonths" 
            :key="month.value"
            :type="selectedDate === month.value ? 'primary' : 'default'"
            size="small"
            @click="selectQuickMonth(month.value)"
          >
            {{ month.label }}
          </el-button>
        </div>
        <div class="date-picker-group">
          <el-date-picker
            v-model="selectedDate"
            type="month"
            placeholder="选择月份"
            format="YYYY年MM月"
            value-format="YYYY-MM"
            @change="loadEarningsData"
            style="width: 160px;"
          />
          <el-button type="primary" @click="loadEarningsData" :loading="loading">
            <el-icon><Refresh /></el-icon>
            查询
          </el-button>
        </div>
      </div>
    </div>

    <!-- 收入概览卡片 -->
    <div class="earnings-overview">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card class="earnings-card" :class="{ 'no-data': earningsData.totalOrders === 0 }">
            <div class="card-content">
              <div class="card-icon total">
                <el-icon><Money /></el-icon>
              </div>
              <div class="card-info">
                <div class="card-value">¥{{ earningsData.totalEarnings.toFixed(2) }}</div>
                <div class="card-label">{{ formatMonthDisplay(selectedDate) }}总收入</div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :span="6">
          <el-card class="earnings-card" :class="{ 'no-data': earningsData.totalOrders === 0 }">
            <div class="card-content">
              <div class="card-icon orders">
                <el-icon><Document /></el-icon>
              </div>
              <div class="card-info">
                <div class="card-value">{{ earningsData.totalOrders }}</div>
                <div class="card-label">{{ formatMonthDisplay(selectedDate) }}完成订单</div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :span="6">
          <el-card class="earnings-card" :class="{ 'no-data': earningsData.totalOrders === 0 }">
            <div class="card-content">
              <div class="card-icon average">
                <el-icon><TrendCharts /></el-icon>
              </div>
              <div class="card-info">
                <div class="card-value">¥{{ earningsData.averageEarnings.toFixed(2) }}</div>
                <div class="card-label">{{ formatMonthDisplay(selectedDate) }}平均单价</div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :span="6">
          <el-card class="earnings-card" :class="{ 'no-data': earningsData.totalOrders === 0 }">
            <div class="card-content">
              <div class="card-icon distance">
                <el-icon><Location /></el-icon>
              </div>
              <div class="card-info">
                <div class="card-value">{{ earningsData.totalDistance.toFixed(1) }}km</div>
                <div class="card-label">{{ formatMonthDisplay(selectedDate) }}总里程</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 无数据提示 -->
    <div v-if="!loading && earningsData.totalOrders === 0" class="no-data-tip">
      <el-empty 
        :image-size="120"
        description="暂无收入记录"
      >
        <template #description>
          <p>{{ formatMonthDisplay(selectedDate) }}暂无完成的订单</p>
          <p>选择其他月份查看收入统计</p>
        </template>
      </el-empty>
    </div>

    <!-- 订单列表 -->
    <el-card v-if="!loading && earningsData.totalOrders > 0" class="orders-card">
      <template #header>
        <div class="card-header">
          <span>{{ formatMonthDisplay(selectedDate) }}收入明细</span>
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
                {{ row.pickupAddress }}
              </div>
              <div class="route-item">
                <el-icon class="destination-icon"><Location /></el-icon>
                {{ row.destinationAddress }}
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
            <el-tag v-else type="primary" size="small">
              即时单
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
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// 响应式数据
const loading = ref(false)
const selectedDate = ref(new Date().toISOString().slice(0, 7)) // 当前月份
const currentPage = ref(1)
const pageSize = ref(20)
const totalOrders = ref(0)
const statusFilter = ref('')

// 收入数据
const earningsData = reactive({
  totalEarnings: 0,
  totalOrders: 0,
  averageEarnings: 0,
  totalDistance: 0
})

const ordersList = ref([])

// 计算属性
const driverId = computed(() => userStore.user?.driverId || userStore.user?.id)

// 快速月份选择
const quickMonths = computed(() => {
  const months = []
  const now = new Date()
  
  // 当前月
  months.push({
    label: '本月',
    value: now.toISOString().slice(0, 7)
  })
  
  // 上个月
  const lastMonth = new Date(now.getFullYear(), now.getMonth() - 1, 1)
  months.push({
    label: '上月',
    value: lastMonth.toISOString().slice(0, 7)
  })
  
  // 前3个月
  for (let i = 2; i <= 4; i++) {
    const month = new Date(now.getFullYear(), now.getMonth() - i, 1)
    months.push({
      label: `${month.getMonth() + 1}月`,
      value: month.toISOString().slice(0, 7)
    })
  }
  
  return months
})

// 加载收入数据
const loadEarningsData = async () => {
  if (!driverId.value) {
    ElMessage.error('无法获取司机信息')
    return
  }
  
  loading.value = true
  
  // 重置数据
  resetEarningsData()
  
  try {
    // 先获取收入统计数据
    const hasEarningsData = await loadEarningsStatistics()
    
    // 如果有收入数据，再获取订单列表
    if (hasEarningsData) {
      await loadOrdersList()
    } else {
      // 没有收入数据时，清空订单列表
      ordersList.value = []
      totalOrders.value = 0
      ElMessage.info(`${formatMonthDisplay(selectedDate.value)} 暂无收入记录`)
    }
  } catch (error) {
    console.error('加载收入数据失败:', error)
    ElMessage.error('网络错误，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 重置收入数据
const resetEarningsData = () => {
  Object.assign(earningsData, {
    totalEarnings: 0,
    totalOrders: 0,
    averageEarnings: 0,
    totalDistance: 0
  })
}

// 加载收入统计数据
const loadEarningsStatistics = async () => {
  const params = new URLSearchParams({
    month: selectedDate.value,
    page: '1',
    size: '100'
  })
  
  try {
    console.log(`正在加载 ${selectedDate.value} 月的收入统计...`)
    
    const response = await fetch(`/api/drivers/${driverId.value}/earnings?${params}`, {
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    
    const result = await response.json()
    console.log('收入统计API响应:', result)
    
    if (result.code === 200 && result.data && result.data.summary) {
      const summary = result.data.summary
      
      // 检查是否有实际的收入数据
      const hasData = summary.totalOrders > 0 || summary.totalEarnings > 0
      
      if (hasData) {
        Object.assign(earningsData, {
          totalEarnings: summary.totalEarnings || 0,
          totalOrders: summary.totalOrders || 0,
          averageEarnings: summary.averageEarnings || 0,
          totalDistance: summary.totalDistance || 0
        })
        console.log(`${selectedDate.value} 月收入统计:`, earningsData)
        return true
      } else {
        console.log(`${selectedDate.value} 月暂无收入记录`)
        return false
      }
    } else {
      console.log(`${selectedDate.value} 月暂无收入数据`)
      return false
    }
  } catch (error) {
    console.error('加载收入统计失败:', error)
    throw error
  }
}

// 加载订单列表
const loadOrdersList = async () => {
  const params = new URLSearchParams({
    page: currentPage.value.toString(),
    size: pageSize.value.toString(),
    status: 'COMPLETED' // 只显示已完成的订单
  })
  
  if (selectedDate.value) {
    const startDate = `${selectedDate.value}-01`
    const endDate = new Date(selectedDate.value + '-01')
    endDate.setMonth(endDate.getMonth() + 1)
    endDate.setDate(0)
    params.append('startDate', startDate)
    params.append('endDate', endDate.toISOString().slice(0, 10))
  }
  
  console.log(`正在加载 ${selectedDate.value} 月的订单列表...`)
  
  const response = await fetch(`/api/drivers/${driverId.value}/orders?${params}`, {
    headers: {
      'Authorization': `Bearer ${userStore.token}`
    }
  })
  
  const result = await response.json()
  console.log('订单列表API响应:', result)
  
  if (result.code === 200) {
    const orders = result.data || []
    ordersList.value = orders
    console.log(`加载了 ${orders.length} 条订单记录`)
    
    // 如果没有统计数据，从订单计算
    if (earningsData.totalEarnings === 0 && orders.length > 0) {
      console.log('从订单数据计算收入统计...')
      calculateEarningsData(orders)
    }
    
    // 设置总数
    totalOrders.value = orders.length >= pageSize.value ? 
      (currentPage.value * pageSize.value + 1) : 
      ((currentPage.value - 1) * pageSize.value + orders.length)
  } else {
    console.error('订单列表API调用失败:', result.message)
    ElMessage.error(result.message || '加载订单数据失败')
  }
}

// 计算收入统计数据
const calculateEarningsData = (orders) => {
  const completedOrders = orders.filter(order => order.status === 'COMPLETED')
  console.log(`计算收入统计 - 已完成订单数: ${completedOrders.length}`)
  
  const totalEarnings = completedOrders.reduce((sum, order) => {
    const fare = parseFloat(order.actualFare) || parseFloat(order.totalFare) || parseFloat(order.estimatedFare) || 0
    return sum + fare
  }, 0)
  
  const totalDistance = completedOrders.reduce((sum, order) => {
    const distance = parseFloat(order.actualDistance) || parseFloat(order.estimatedDistance) || 0
    return sum + distance
  }, 0)
  
  const averageEarnings = completedOrders.length > 0 ? totalEarnings / completedOrders.length : 0
  
  console.log('计算结果:', {
    totalEarnings: totalEarnings.toFixed(2),
    totalOrders: completedOrders.length,
    averageEarnings: averageEarnings.toFixed(2),
    totalDistance: totalDistance.toFixed(1)
  })
  
  Object.assign(earningsData, {
    totalEarnings,
    totalOrders: completedOrders.length,
    averageEarnings,
    totalDistance
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

// 获取状态标签类型
const getStatusTagType = (status) => {
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
const getStatusText = (status) => {
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
  return new Date(dateTime).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 格式化月份显示
const formatMonthDisplay = (monthValue) => {
  if (!monthValue) return ''
  const [year, month] = monthValue.split('-')
  return `${year}年${month}月`
}

// 快速选择月份
const selectQuickMonth = (monthValue) => {
  selectedDate.value = monthValue
  loadEarningsData()
}

// 组件挂载时加载数据
onMounted(() => {
  console.log('司机收入统计页面已加载')
  console.log('司机ID:', driverId.value)
  console.log('默认查询月份:', selectedDate.value)
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
  flex-direction: column;
  gap: 10px;
  align-items: flex-end;
}

.quick-months {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.date-picker-group {
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

.earnings-card.no-data {
  opacity: 0.6;
}

.no-data-tip {
  margin: 40px 0;
  text-align: center;
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