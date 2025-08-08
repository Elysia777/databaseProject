<template>
  <div class="admin-dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-icon users">
              <el-icon><User /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-number">{{ stats.totalUsers }}</div>
              <div class="stats-label">总用户数</div>
              <div class="stats-change" :class="{ 'positive': stats.userGrowth >= 0, 'negative': stats.userGrowth < 0 }">
                较昨日 {{ stats.userGrowth >= 0 ? '+' : '' }}{{ stats.userGrowth }}%
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-icon drivers">
              <el-icon><Van /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-number">{{ stats.onlineDrivers }}</div>
              <div class="stats-label">在线司机</div>
              <div class="stats-change">当前在线</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-icon orders">
              <el-icon><Document /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-number">{{ stats.todayOrders }}</div>
              <div class="stats-label">今日订单</div>
              <div class="stats-change" :class="{ 'positive': stats.orderGrowth >= 0, 'negative': stats.orderGrowth < 0 }">
                较昨日 {{ stats.orderGrowth >= 0 ? '+' : '' }}{{ stats.orderGrowth }}%
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-icon revenue">
              <el-icon><Money /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-number">¥{{ stats.todayRevenue }}</div>
              <div class="stats-label">今日收入</div>
              <div class="stats-change" :class="{ 'positive': stats.revenueGrowth >= 0, 'negative': stats.revenueGrowth < 0 }">
                较昨日 {{ stats.revenueGrowth >= 0 ? '+' : '' }}{{ stats.revenueGrowth }}%
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 图表区域 -->
    <el-row :gutter="20" class="charts-row">
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>订单趋势（近7天）</span>
            </div>
          </template>
          <div class="chart-container">
            <v-chart :option="orderTrendOption" style="height: 300px" />
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>用户类型分布</span>
            </div>
          </template>
          <div class="chart-container">
            <v-chart :option="userTypeOption" style="height: 300px" />
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 用户管理快捷入口 -->
    <el-row :gutter="20" class="management-row">
      <el-col :span="8">
        <el-card class="management-card" @click="goToUserManagement">
          <div class="management-content">
            <el-icon class="management-icon"><User /></el-icon>
            <div class="management-info">
              <div class="management-title">用户管理</div>
              <div class="management-desc">管理所有用户账号</div>
            </div>
            <el-icon class="arrow-icon"><ArrowRight /></el-icon>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card class="management-card" @click="goToOrderManagement">
          <div class="management-content">
            <el-icon class="management-icon"><Document /></el-icon>
            <div class="management-info">
              <div class="management-title">订单管理</div>
              <div class="management-desc">查看和管理所有订单</div>
            </div>
            <el-icon class="arrow-icon"><ArrowRight /></el-icon>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card class="management-card" @click="goToReviewManagement">
          <div class="management-content">
            <el-icon class="management-icon"><Star /></el-icon>
            <div class="management-info">
              <div class="management-title">评价管理</div>
              <div class="management-desc">管理用户评价和反馈</div>
            </div>
            <el-icon class="arrow-icon"><ArrowRight /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 最近活动 -->
    <el-row :gutter="20" class="activity-row">
      <el-col :span="24">
        <el-card class="activity-card">
          <template #header>
            <div class="card-header">
              <span>最近活动</span>
              <el-button type="text" @click="refreshData">刷新</el-button>
            </div>
          </template>
          <el-table :data="recentActivities" style="width: 100%" v-loading="loading">
            <el-table-column prop="time" label="时间" width="180" />
            <el-table-column prop="type" label="类型" width="120">
              <template #default="scope">
                <el-tag :type="getActivityTypeColor(scope.row.type)">
                  {{ scope.row.type }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="描述" />
            <el-table-column prop="user" label="用户" width="120" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'

use([CanvasRenderer, LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent])

const router = useRouter()
const loading = ref(false)

// 统计数据
const stats = reactive({
  totalUsers: 0,
  onlineDrivers: 0,
  todayOrders: 0,
  todayRevenue: 0,
  userGrowth: 0,
  orderGrowth: 0,
  revenueGrowth: 0
})

// 最近活动数据
const recentActivities = ref([])

// 订单趋势图表配置
const orderTrendOption = ref({
  tooltip: {
    trigger: 'axis'
  },
  xAxis: {
    type: 'category',
    data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  },
  yAxis: {
    type: 'value'
  },
  series: [
    {
      name: '订单数',
      type: 'line',
      data: [120, 132, 101, 134, 90, 230, 210],
      smooth: true,
      itemStyle: { color: '#409eff' }
    }
  ]
})

// 用户类型分布图表配置
const userTypeOption = ref({
  tooltip: {
    trigger: 'item'
  },
  series: [
    {
      type: 'pie',
      radius: '50%',
      data: [
        { value: 0, name: '乘客' },
        { value: 0, name: '司机' },
        { value: 0, name: '管理员' }
      ]
    }
  ]
})

// 方法
const loadData = async () => {
  loading.value = true
  try {
    console.log('正在加载管理员概览数据...')
    
    // 加载用户统计
    try {
      const usersResponse = await fetch('/api/auth/users')
      console.log('用户API响应状态:', usersResponse.status)
      
      if (usersResponse.ok) {
        const usersData = await usersResponse.json()
        console.log('用户数据:', usersData)
        
        const isUsersSuccess = usersData.success === true || usersData.code === 200;
        if (isUsersSuccess) {
          const users = usersData.data
          stats.totalUsers = users.length
          stats.onlineDrivers = users.filter(u => u.userType === 'DRIVER' && u.status === 'ACTIVE').length
          
          // 更新用户类型分布图表
          const passengers = users.filter(u => u.userType === 'PASSENGER').length
          const drivers = users.filter(u => u.userType === 'DRIVER').length
          const admins = users.filter(u => u.userType === 'ADMIN').length
          
          userTypeOption.value.series[0].data = [
            { value: passengers, name: '乘客' },
            { value: drivers, name: '司机' },
            { value: admins, name: '管理员' }
          ]
        }
      } else {
        console.warn('用户API请求失败:', usersResponse.status)
      }
    } catch (error) {
      console.error('加载用户数据失败:', error)
    }
    
    // 加载订单统计
    let ordersData = null
    try {
      const ordersResponse = await fetch('/api/orders/with-names')
      console.log('订单API响应状态:', ordersResponse.status)
      
      if (ordersResponse.ok) {
        ordersData = await ordersResponse.json()
        console.log('订单数据:', ordersData)
        
        const isOrdersSuccess = ordersData.success === true || ordersData.code === 200;
        if (isOrdersSuccess) {
          const orders = ordersData.data
          const today = new Date().toISOString().split('T')[0]
          const todayOrders = orders.filter(o => {
            const orderDate = o.createdAt || o.created_at
            return orderDate && orderDate.startsWith(today)
          })
          stats.todayOrders = todayOrders.length
          
          // 计算今日收入，支持两种字段格式
          stats.todayRevenue = todayOrders.reduce((sum, order) => {
            const fare = order.actualFare || order.actual_fare || 
                        order.totalFare || order.total_fare || 
                        order.estimatedFare || order.estimated_fare || 0
            return sum + fare
          }, 0)
          
          // 更新订单趋势图表（使用最近7天的数据）
          const last7Days = []
          const orderCounts = []
          for (let i = 6; i >= 0; i--) {
            const date = new Date()
            date.setDate(date.getDate() - i)
            const dateStr = date.toISOString().split('T')[0]
            const dayOrders = orders.filter(o => {
              const orderDate = o.createdAt || o.created_at
              return orderDate && orderDate.startsWith(dateStr)
            })
            last7Days.push(date.toLocaleDateString('zh-CN', { weekday: 'short' }))
            orderCounts.push(dayOrders.length)
          }
          
          orderTrendOption.value.xAxis.data = last7Days
          orderTrendOption.value.series[0].data = orderCounts
        }
      } else {
        console.warn('订单API请求失败:', ordersResponse.status)
      }
    } catch (error) {
      console.error('加载订单数据失败:', error)
    }
    
    // 计算增长数据（基于真实数据）
    await calculateGrowthStats(ordersData?.data || [])
    
    // 加载最近活动
    loadRecentActivities(ordersData?.data || [])
    
    console.log('管理员概览数据加载完成:', stats)
    
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error(`加载数据失败: ${error.message}`)
    
    // 如果是网络错误，提供更具体的提示
    if (error.message.includes('fetch')) {
      ElMessage.warning('无法连接到后端服务，请检查后端服务是否运行在8080端口')
    }
  } finally {
    loading.value = false
  }
}

const calculateGrowthStats = async (orders = []) => {
  try {
    const today = new Date()
    const yesterday = new Date(today)
    yesterday.setDate(yesterday.getDate() - 1)
    
    const todayStr = today.toISOString().split('T')[0]
    const yesterdayStr = yesterday.toISOString().split('T')[0]
    
    // 计算今日和昨日订单数
    const todayOrdersCount = orders.filter(o => {
      const orderDate = o.createdAt || o.created_at
      return orderDate && orderDate.startsWith(todayStr)
    }).length
    
    const yesterdayOrdersCount = orders.filter(o => {
      const orderDate = o.createdAt || o.created_at
      return orderDate && orderDate.startsWith(yesterdayStr)
    }).length
    
    // 计算今日和昨日收入
    const todayRevenue = orders.filter(o => {
      const orderDate = o.createdAt || o.created_at
      return orderDate && orderDate.startsWith(todayStr)
    }).reduce((sum, order) => {
      const fare = order.actualFare || order.actual_fare || 
                  order.totalFare || order.total_fare || 
                  order.estimatedFare || order.estimated_fare || 0
      return sum + fare
    }, 0)
    
    const yesterdayRevenue = orders.filter(o => {
      const orderDate = o.createdAt || o.created_at
      return orderDate && orderDate.startsWith(yesterdayStr)
    }).reduce((sum, order) => {
      const fare = order.actualFare || order.actual_fare || 
                  order.totalFare || order.total_fare || 
                  order.estimatedFare || order.estimated_fare || 0
      return sum + fare
    }, 0)
    
    // 计算增长率
    stats.orderGrowth = yesterdayOrdersCount > 0 ? 
      Math.round(((todayOrdersCount - yesterdayOrdersCount) / yesterdayOrdersCount) * 100) : 
      (todayOrdersCount > 0 ? 100 : 0)
    
    stats.revenueGrowth = yesterdayRevenue > 0 ? 
      Math.round(((todayRevenue - yesterdayRevenue) / yesterdayRevenue) * 100) : 
      (todayRevenue > 0 ? 100 : 0)
    
    // 获取用户增长数据
    try {
      const usersResponse = await fetch('/api/auth/users')
      if (usersResponse.ok) {
        const usersData = await usersResponse.json()
        if (usersData.success === true || usersData.code === 200) {
          const users = usersData.data
          
          const todayUsers = users.filter(u => {
            const userDate = u.createdAt || u.created_at
            return userDate && userDate.startsWith(todayStr)
          }).length
          
          const yesterdayUsers = users.filter(u => {
            const userDate = u.createdAt || u.created_at
            return userDate && userDate.startsWith(yesterdayStr)
          }).length
          
          stats.userGrowth = yesterdayUsers > 0 ? 
            Math.round(((todayUsers - yesterdayUsers) / yesterdayUsers) * 100) : 
            (todayUsers > 0 ? 100 : 0)
        }
      }
    } catch (error) {
      console.error('获取用户增长数据失败:', error)
      stats.userGrowth = 0
    }
    
  } catch (error) {
    console.error('计算增长统计失败:', error)
    stats.userGrowth = 0
    stats.orderGrowth = 0
    stats.revenueGrowth = 0
  }
}

const loadRecentActivities = (orders = []) => {
  // 基于真实订单数据生成最近活动
  const activities = []
  
  // 获取最近的订单活动
  const recentOrders = orders
    .filter(o => o.createdAt || o.created_at)
    .sort((a, b) => {
      const dateA = new Date(a.createdAt || a.created_at)
      const dateB = new Date(b.createdAt || b.created_at)
      return dateB - dateA
    })
    .slice(0, 10)
  
  recentOrders.forEach(order => {
    const orderDate = new Date(order.createdAt || order.created_at)
    const passengerName = order.passengerName || order.passenger_name || '未知用户'
    const driverName = order.driverName || order.driver_name || '未知司机'
    
    activities.push({
      time: orderDate.toLocaleString(),
      type: getOrderStatusText(order.status),
      description: `订单 #${order.id} - 乘客: ${passengerName}${order.driverId ? `, 司机: ${driverName}` : ''}`,
      user: passengerName
    })
  })
  
  // 如果没有足够的订单数据，添加一些模拟数据
  if (activities.length < 4) {
    activities.push(
      {
        time: new Date().toLocaleString(),
        type: '系统启动',
        description: '管理系统正常运行中',
        user: '系统'
      }
    )
  }
  
  recentActivities.value = activities.slice(0, 8)
}

const getOrderStatusText = (status) => {
  const statusMap = {
    'PENDING': '待接单',
    'ASSIGNED': '已接单',
    'PICKUP': '司机到达',
    'IN_PROGRESS': '行程中',
    'COMPLETED': '订单完成',
    'CANCELLED': '订单取消'
  }
  return statusMap[status] || status
}

const getActivityTypeColor = (type) => {
  const colors = {
    '新用户注册': 'success',
    '订单完成': 'primary',
    '司机上线': 'warning',
    '用户评价': 'info'
  }
  return colors[type] || ''
}

const refreshData = () => {
  loadData()
  ElMessage.success('数据已刷新')
}

const goToUserManagement = () => {
  router.push('/dashboard/user-management')
}

const goToOrderManagement = () => {
  router.push('/dashboard/order-management')
}

const goToReviewManagement = () => {
  router.push('/dashboard/review-management')
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.admin-dashboard {
  padding: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.stats-card {
  height: 120px;
  cursor: default;
}

.stats-content {
  display: flex;
  align-items: center;
  height: 100%;
}

.stats-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20px;
  font-size: 24px;
  color: white;
}

.stats-icon.users {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stats-icon.drivers {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.stats-icon.orders {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stats-icon.revenue {
  background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%);
}

.stats-info {
  flex: 1;
}

.stats-number {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin-bottom: 5px;
}

.stats-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 2px;
}

.stats-change {
  font-size: 12px;
  color: #999;
}

.stats-change.positive {
  color: #67c23a;
}

.stats-change.negative {
  color: #f56c6c;
}

.charts-row {
  margin-bottom: 20px;
}

.chart-card {
  height: 400px;
}

.chart-container {
  height: 300px;
}

.management-row {
  margin-bottom: 20px;
}

.management-card {
  height: 100px;
  cursor: pointer;
  transition: all 0.3s;
}

.management-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.management-content {
  display: flex;
  align-items: center;
  height: 100%;
  padding: 0 10px;
}

.management-icon {
  font-size: 32px;
  color: #409eff;
  margin-right: 15px;
}

.management-info {
  flex: 1;
}

.management-title {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin-bottom: 5px;
}

.management-desc {
  font-size: 12px;
  color: #666;
}

.arrow-icon {
  font-size: 16px;
  color: #999;
}

.activity-row {
  margin-bottom: 20px;
}

.activity-card {
  min-height: 300px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>