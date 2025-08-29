<template>
  <div class="order-management">
    <!-- 搜索和筛选 -->
    <el-card class="filter-card">
      <div class="filter-row">
        <div class="filter-left">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索订单号或用户"
            style="width: 300px; margin-right: 10px;"
            clearable
            @input="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          
          <el-select
            v-model="statusFilter"
            placeholder="订单状态"
            style="width: 120px; margin-right: 10px;"
            @change="handleFilter"
          >
            <el-option label="全部" value="" />
            <el-option label="待接单" value="PENDING" />
            <el-option label="进行中" value="IN_PROGRESS" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已退款" value="COMPLETED_REFUNDED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
          
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 240px;"
            @change="handleFilter"
          />
        </div>
        
        <div class="filter-right">
          <el-button type="primary" @click="refreshData">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
          <el-button type="success" @click="exportData">
            <el-icon><Download /></el-icon>
            导出
          </el-button>
        </div>
      </div>
    </el-card>
    
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="4">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-number">{{ orderStats.total }}</div>
            <div class="stats-label">总订单数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-number">{{ orderStats.completed }}</div>
            <div class="stats-label">已完成</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-number">{{ orderStats.refunded }}</div>
            <div class="stats-label">已退款</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-number">{{ orderStats.inProgress }}</div>
            <div class="stats-label">进行中</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-number">¥{{ orderStats.totalRevenue }}</div>
            <div class="stats-label">总收入</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-number">¥{{ orderStats.actualRevenue }}</div>
            <div class="stats-label">实际收入</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 订单列表 -->
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>订单列表 ({{ filteredOrders.length }})</span>
        </div>
      </template>
      
      <el-table
        :data="paginatedOrders"
        style="width: 100%"
        v-loading="loading"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="订单号" width="100" />
        <el-table-column prop="passengerName" label="乘客" width="120" />
        <el-table-column prop="driverName" label="司机" width="120" />
        <el-table-column prop="startAddress" label="起点" width="200" show-overflow-tooltip />
        <el-table-column prop="endAddress" label="终点" width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getOrderStatusColor(scope.row.status, scope.row.paymentStatus || scope.row.payment_status)">
              {{ getOrderStatusText(scope.row.status, scope.row.paymentStatus || scope.row.payment_status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalFare" label="费用" width="100">
          <template #default="scope">
            ¥{{ scope.row.totalFare || 0 }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="viewOrder(scope.row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="filteredOrders.length"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
    
    <!-- 订单详情对话框 -->
    <el-dialog
      v-model="orderDetailVisible"
      title="订单详情"
      width="800px"
    >
      <div v-if="selectedOrder" class="order-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ selectedOrder.id }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="getOrderStatusColor(selectedOrder.status, selectedOrder.paymentStatus || selectedOrder.payment_status)">
              {{ getOrderStatusText(selectedOrder.status, selectedOrder.paymentStatus || selectedOrder.payment_status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="乘客">{{ selectedOrder.passengerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="司机">{{ selectedOrder.driverName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="起点" :span="2">{{ selectedOrder.startAddress || '-' }}</el-descriptions-item>
          <el-descriptions-item label="终点" :span="2">{{ selectedOrder.endAddress || '-' }}</el-descriptions-item>
          <el-descriptions-item label="预估距离">{{ getDistance(selectedOrder, 'estimated') }}</el-descriptions-item>
          <el-descriptions-item label="实际距离">{{ getDistance(selectedOrder, 'actual') }}</el-descriptions-item>
          <el-descriptions-item label="预估时长">{{ getDuration(selectedOrder, 'estimated') }}</el-descriptions-item>
          <el-descriptions-item label="实际时长">{{ getDuration(selectedOrder, 'actual') }}</el-descriptions-item>
          <el-descriptions-item label="预估费用">¥{{ getFare(selectedOrder, 'estimated') }}</el-descriptions-item>
          <el-descriptions-item label="实际费用">¥{{ getFare(selectedOrder, 'actual') }}</el-descriptions-item>
          <el-descriptions-item label="总费用">¥{{ selectedOrder.totalFare || getFare(selectedOrder, 'actual') || getFare(selectedOrder, 'estimated') || 0 }}</el-descriptions-item>
          <el-descriptions-item label="支付状态">
            <el-tag :type="getPaymentStatusColor(selectedOrder.paymentStatus || selectedOrder.payment_status)">
              {{ getPaymentStatusText(selectedOrder.paymentStatus || selectedOrder.payment_status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="支付方式">{{ getPaymentMethodText(selectedOrder.paymentMethod || selectedOrder.payment_method) }}</el-descriptions-item>
          <el-descriptions-item label="订单类型">{{ getOrderTypeText(selectedOrder.orderType || selectedOrder.order_type) }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ selectedOrder.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="接单时间">{{ getPickupTime(selectedOrder) }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ getCompletionTime(selectedOrder) }}</el-descriptions-item>
          <el-descriptions-item label="取消原因" v-if="selectedOrder.status === 'CANCELLED'" :span="2">{{ selectedOrder.cancelReason || selectedOrder.cancel_reason || '-' }}</el-descriptions-item>
          <el-descriptions-item label="退款信息" v-if="selectedOrder.status === 'COMPLETED' && (selectedOrder.paymentStatus === 'REFUNDED' || selectedOrder.payment_status === 'REFUNDED')" :span="2">
            <el-tag type="warning">此订单已完成但已退款，不计入实际收入</el-tag>
          </el-descriptions-item>
        </el-descriptions>
        
        <div v-if="selectedOrder.remarks" style="margin-top: 20px;">
          <h4>备注信息</h4>
          <p>{{ selectedOrder.remarks }}</p>
        </div>
      </div>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="orderDetailVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// 响应式数据
const loading = ref(false)
const orders = ref([])
const selectedOrders = ref([])
const selectedOrder = ref(null)
const orderDetailVisible = ref(false)

// 筛选条件
const searchKeyword = ref('')
const statusFilter = ref('')
const dateRange = ref([])

// 分页
const currentPage = ref(1)
const pageSize = ref(20)

// 订单统计
const orderStats = reactive({
  total: 0,
  completed: 0,
  refunded: 0,
  inProgress: 0,
  totalRevenue: 0,
  actualRevenue: 0
})

// 计算属性
const filteredOrders = computed(() => {
  let filtered = orders.value
  
  // 搜索筛选
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    filtered = filtered.filter(order =>
      order.id.toString().includes(keyword) ||
      (order.passengerName && order.passengerName.toLowerCase().includes(keyword)) ||
      (order.driverName && order.driverName.toLowerCase().includes(keyword))
    )
  }
  
  // 状态筛选
  if (statusFilter.value) {
    if (statusFilter.value === 'COMPLETED_REFUNDED') {
      // 已退款状态：订单状态为已完成，支付状态为已退款
      filtered = filtered.filter(order => 
        order.status === 'COMPLETED' && 
        (order.paymentStatus === 'REFUNDED' || order.payment_status === 'REFUNDED')
      )
    } else if (statusFilter.value === 'COMPLETED') {
      // 已完成状态：订单状态为已完成，支付状态不为已退款
      filtered = filtered.filter(order => 
        order.status === 'COMPLETED' && 
        (order.paymentStatus !== 'REFUNDED' && order.payment_status !== 'REFUNDED')
      )
    } else {
      filtered = filtered.filter(order => order.status === statusFilter.value)
    }
  }
  
  // 日期筛选
  if (dateRange.value && dateRange.value.length === 2) {
    const [startDate, endDate] = dateRange.value
    filtered = filtered.filter(order => {
      const orderDate = new Date(order.createdAt)
      return orderDate >= startDate && orderDate <= endDate
    })
  }
  
  return filtered
})

const paginatedOrders = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredOrders.value.slice(start, end)
})

// 方法
const loadOrders = async () => {
  loading.value = true
  try {
    console.log('正在请求订单数据（包含用户姓名）...')
    const response = await fetch('/api/orders/with-names')
    console.log('响应状态:', response.status)
    
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`)
    }
    
    const data = await response.json()
    console.log('订单数据响应:', data)
    
    // 兼容两种响应格式：{success: true} 或 {code: 200}
    const isSuccess = data.success === true || data.code === 200;
    console.log('订单API判断是否成功:', isSuccess, 'data.success:', data.success, 'data.code:', data.code);
    
    if (isSuccess) {
      // 处理订单数据，现在数据中已经包含了用户姓名
      console.log('原始订单数据（前2个）:', data.data.slice(0, 2))
      
      orders.value = data.data.map(order => {
        // 费用优先级：actualFare > totalFare > estimatedFare（支持两种字段格式）
        const displayFare = order.actualFare || order.actual_fare || 
                           order.totalFare || order.total_fare || 
                           order.estimatedFare || order.estimated_fare || 0
        
        const processedOrder = {
          ...order,
          // 字段名适配（支持驼峰和下划线两种格式）
          startAddress: order.pickupAddress || order.pickup_address || '-',
          endAddress: order.destinationAddress || order.destination_address || '-',
          passengerName: order.passengerName || order.passenger_name || `未知乘客(ID:${order.passengerId || order.passenger_id})`,
          driverName: order.driverName || order.driver_name || (order.driverId || order.driver_id ? `未知司机(ID:${order.driverId || order.driver_id})` : '-'),
          // 修复费用显示（支持两种格式）
          totalFare: displayFare,
          estimatedFare: order.estimatedFare || order.estimated_fare || 0,
          actualFare: order.actualFare || order.actual_fare || 0,
          // 时间字段处理（支持两种格式）
          createdAt: (order.createdAt || order.created_at) ? new Date(order.createdAt || order.created_at).toLocaleString() : '-',
          completedAt: (order.completedAt || order.completed_at) ? new Date(order.completedAt || order.completed_at).toLocaleString() : '-'
        }
        
        // 调试前几个订单的处理结果
        if (order.id >= 385) {
          console.log(`订单 ${order.id} 处理结果:`, {
            passengerId: order.passengerId,
            passengerName: processedOrder.passengerName,
            driverId: order.driverId,
            driverName: processedOrder.driverName,
            originalFares: { totalFare: order.totalFare, actualFare: order.actualFare, estimatedFare: order.estimatedFare },
            displayFare: displayFare
          })
        }
        
        return processedOrder
      })
      
      // 更新统计数据
      updateOrderStats()
      ElMessage.success(`成功加载 ${orders.value.length} 个订单`)
    } else {
      ElMessage.error(data.message || '加载订单数据失败')
    }
  } catch (error) {
    console.error('加载订单失败:', error)
    ElMessage.error(`网络错误: ${error.message}`)
    
    // 如果是404错误，提供更具体的提示
    if (error.message.includes('404')) {
      ElMessage.warning('后端服务可能未启动，请检查后端服务是否运行在8080端口')
    }
  } finally {
    loading.value = false
  }
}

const updateOrderStats = () => {
  orderStats.total = orders.value.length
  
  // 统计已完成订单（包括退款和未退款）
  const completedOrders = orders.value.filter(o => o.status === 'COMPLETED')
  orderStats.completed = completedOrders.length
  
  // 统计退款订单
  orderStats.refunded = completedOrders.filter(o => 
    (o.paymentStatus === 'REFUNDED' || o.payment_status === 'REFUNDED')
  ).length
  
  orderStats.inProgress = orders.value.filter(o => o.status === 'IN_PROGRESS').length
  
  // 总收入（包括退款订单）
  orderStats.totalRevenue = completedOrders.reduce((sum, order) => sum + (order.totalFare || 0), 0)
  
  // 实际收入（排除退款订单）
  orderStats.actualRevenue = completedOrders
    .filter(o => (o.paymentStatus !== 'REFUNDED' && o.payment_status !== 'REFUNDED'))
    .reduce((sum, order) => sum + (order.totalFare || 0), 0)
}

const refreshData = () => {
  loadOrders()
  ElMessage.success('数据已刷新')
}

const exportData = () => {
  // 这里实现数据导出功能
  ElMessage.info('导出功能开发中...')
}

const handleSearch = () => {
  currentPage.value = 1
}

const handleFilter = () => {
  currentPage.value = 1
}

const handleSelectionChange = (selection) => {
  selectedOrders.value = selection
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
}

const handleCurrentChange = (page) => {
  currentPage.value = page
}

const getOrderStatusColor = (status, paymentStatus) => {
  // 如果订单已完成但已退款，显示退款标签颜色
  if (status === 'COMPLETED' && (paymentStatus === 'REFUNDED' || paymentStatus === 'REFUNDED')) {
    return 'warning'
  }
  
  const colors = {
    'PENDING': 'warning',
    'IN_PROGRESS': 'primary',
    'COMPLETED': 'success',
    'CANCELLED': 'danger'
  }
  return colors[status] || ''
}

const getOrderStatusText = (status, paymentStatus) => {
  // 如果订单已完成但已退款，显示退款状态
  if (status === 'COMPLETED' && (paymentStatus === 'REFUNDED' || paymentStatus === 'REFUNDED')) {
    return '已退款'
  }
  
  const texts = {
    'PENDING': '待接单',
    'IN_PROGRESS': '进行中',
    'COMPLETED': '已完成',
    'CANCELLED': '已取消'
  }
  return texts[status] || status
}

const viewOrder = (order) => {
  selectedOrder.value = order
  orderDetailVisible.value = true
}

// 辅助方法
const getDistance = (order, type) => {
  if (type === 'estimated') {
    const distance = order.estimatedDistance || order.estimated_distance
    return distance ? `${distance} km` : '-'
  } else {
    const distance = order.actualDistance || order.actual_distance
    return distance ? `${distance} km` : '-'
  }
}

const getDuration = (order, type) => {
  if (type === 'estimated') {
    const duration = order.estimatedDuration || order.estimated_duration
    return duration ? `${duration} 分钟` : '-'
  } else {
    const duration = order.actualDuration || order.actual_duration
    return duration ? `${duration} 分钟` : '-'
  }
}

const getFare = (order, type) => {
  if (type === 'estimated') {
    return order.estimatedFare || order.estimated_fare || 0
  } else {
    return order.actualFare || order.actual_fare || 0
  }
}

const getPaymentStatusColor = (status) => {
  const colors = {
    'UNPAID': 'warning',
    'PAID': 'success',
    'REFUNDED': 'info'
  }
  return colors[status] || ''
}

const getPaymentStatusText = (status) => {
  const texts = {
    'UNPAID': '未支付',
    'PAID': '已支付',
    'REFUNDED': '已退款'
  }
  return texts[status] || status || '-'
}

const getPaymentMethodText = (method) => {
  const methods = {
    'CASH': '现金',
    'WECHAT': '微信支付',
    'ALIPAY': '支付宝',
    'CREDIT_CARD': '信用卡'
  }
  return methods[method] || method || '-'
}

const getOrderTypeText = (type) => {
  const types = {
    'REAL_TIME': '实时订单',
    'RESERVATION': '预约订单'
  }
  return types[type] || type || '-'
}

const getPickupTime = (order) => {
  const pickupTime = order.pickupTime || order.pickup_time
  return pickupTime ? new Date(pickupTime).toLocaleString() : '-'
}

const getCompletionTime = (order) => {
  // 尝试多种可能的字段名
  const completionTime = order.completionTime || order.completion_time || 
                        order.completedAt || order.completed_at
  return completionTime ? new Date(completionTime).toLocaleString() : '-'
}

onMounted(() => {
  loadOrders()
})
</script>

<style scoped>
.order-management {
  padding: 20px;
}

.filter-card {
  margin-bottom: 20px;
}

.filter-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-left {
  display: flex;
  align-items: center;
}

.stats-row {
  margin-bottom: 20px;
}

.stats-card {
  height: 80px;
  cursor: default;
}

.stats-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.stats-number {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin-bottom: 5px;
}

.stats-label {
  font-size: 12px;
  color: #666;
}

.table-card {
  min-height: 600px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination-container {
  margin-top: 20px;
  text-align: right;
}

.order-detail {
  padding: 20px 0;
}
</style>