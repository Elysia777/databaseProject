<template>
  <div class="driver-orders">
    <div class="page-header">
      <h2>我的订单</h2>
      <div class="header-actions">
        <el-select v-model="statusFilter" placeholder="筛选状态" clearable @change="loadOrders">
          <el-option label="全部" value="" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已取消" value="CANCELLED" />
          <el-option label="进行中" value="IN_PROGRESS" />
          <el-option label="已接单" value="ASSIGNED" />
          <el-option label="已到达" value="PICKUP" />
        </el-select>
        <el-button @click="loadOrders" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
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
        <el-card v-for="order in orders" :key="order.id" class="order-card">
          <div class="order-header">
            <div class="order-info">
              <span class="order-number">#{{ order.orderNumber }}</span>
              <el-tag :type="getStatusTagType(order.status)" size="small">
                {{ getStatusText(order.status) }}
              </el-tag>
              <el-tag v-if="order.orderType === 'RESERVATION'" type="warning" size="small">
                预约单
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
                <span class="address">{{ order.pickupAddress }}</span>
              </div>
              <div class="route-line"></div>
              <div class="location-item">
                <el-icon class="location-icon destination"><Location /></el-icon>
                <span class="address">{{ order.destinationAddress }}</span>
              </div>
            </div>

            <div class="order-details">
              <div class="detail-row">
                <span class="label">距离:</span>
                <span class="value">{{ order.estimatedDistance || '--' }} 公里</span>
              </div>
              <div class="detail-row">
                <span class="label">时长:</span>
                <span class="value">{{ order.estimatedDuration || '--' }} 分钟</span>
              </div>
              <div class="detail-row">
                <span class="label">费用:</span>
                <span class="value price">¥{{ order.actualFare || order.estimatedFare || '--' }}</span>
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
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Loading, LocationFilled, Location } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// 响应式数据
const orders = ref([])
const loading = ref(false)
const statusFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const totalOrders = ref(0)

// 计算属性
const driverId = computed(() => userStore.user?.driverId || userStore.user?.id)

// 加载订单列表
const loadOrders = async () => {
  if (!driverId.value) {
    ElMessage.error('无法获取司机信息')
    return
  }

  loading.value = true
  try {
    const params = new URLSearchParams({
      page: currentPage.value.toString(),
      size: pageSize.value.toString()
    })
    
    if (statusFilter.value) {
      params.append('status', statusFilter.value)
    }

    const response = await fetch(`/api/drivers/${driverId.value}/orders?${params}`, {
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })

    const result = await response.json()
    
    if (result.code === 200) {
      orders.value = result.data || []
      // 注意：这里假设后端返回总数，如果没有可以设置为当前页数据长度
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
}

.order-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
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

/* 响应式设计 */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }

  .header-actions {
    justify-content: space-between;
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