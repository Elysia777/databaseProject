<template>
  <div class="overview">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-icon orders">
              <el-icon><Document /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-number">{{ stats.totalOrders }}</div>
              <div class="stats-label">总订单数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-icon drivers">
              <el-icon><User /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-number">{{ stats.totalDrivers }}</div>
              <div class="stats-label">注册司机</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-icon passengers">
              <el-icon><Avatar /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-number">{{ stats.totalPassengers }}</div>
              <div class="stats-label">注册乘客</div>
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
              <div class="stats-number">¥{{ stats.totalRevenue }}</div>
              <div class="stats-label">总收入</div>
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
              <span>订单趋势</span>
            </div>
          </template>
          <div class="chart-container">
            <v-chart :option="orderChartOption" style="height: 300px" />
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>收入统计</span>
            </div>
          </template>
          <div class="chart-container">
            <v-chart :option="revenueChartOption" style="height: 300px" />
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 最近订单 -->
    <el-card class="recent-orders">
      <template #header>
        <div class="card-header">
          <span>{{ getRecentTitle() }}</span>
          <el-button type="primary" size="small" @click="$router.push('/dashboard/orders')">
            查看全部
          </el-button>
        </div>
      </template>
      
      <el-table :data="recentOrders" style="width: 100%">
        <el-table-column prop="orderNumber" label="订单号" width="180" />
        <el-table-column prop="passengerName" label="乘客" width="120" />
        <el-table-column prop="driverName" label="司机" width="120" />
        <el-table-column prop="pickupAddress" label="上车地址" />
        <el-table-column prop="totalFare" label="金额" width="100">
          <template #default="scope">
            ¥{{ scope.row.totalFare }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import dayjs from 'dayjs'

const userStore = useUserStore()

use([CanvasRenderer, LineChart, BarChart, GridComponent, TooltipComponent, LegendComponent])

// 统计数据
const stats = ref({
  totalOrders: 0,
  totalDrivers: 0,
  totalPassengers: 0,
  totalRevenue: 0
})

// 最近订单
const recentOrders = ref([])

// 订单趋势图表配置
const orderChartOption = ref({
  tooltip: {
    trigger: 'axis'
  },
  legend: {
    data: ['订单数', '完成率']
  },
  xAxis: {
    type: 'category',
    data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  },
  yAxis: [
    {
      type: 'value',
      name: '订单数'
    },
    {
      type: 'value',
      name: '完成率',
      axisLabel: {
        formatter: '{value}%'
      }
    }
  ],
  series: [
    {
      name: '订单数',
      type: 'line',
      data: [120, 132, 101, 134, 90, 230, 210]
    },
    {
      name: '完成率',
      type: 'line',
      yAxisIndex: 1,
      data: [95, 92, 88, 96, 89, 94, 91]
    }
  ]
})

// 收入统计图表配置
const revenueChartOption = ref({
  tooltip: {
    trigger: 'axis'
  },
  legend: {
    data: ['收入']
  },
  xAxis: {
    type: 'category',
    data: ['1月', '2月', '3月', '4月', '5月', '6月']
  },
  yAxis: {
    type: 'value',
    name: '收入(万元)'
  },
  series: [
    {
      name: '收入',
      type: 'bar',
      data: [12, 15, 18, 22, 25, 28]
    }
  ]
})

// 获取状态类型
const getStatusType = (status) => {
  const statusMap = {
    'PENDING': 'warning',
    'ASSIGNED': 'primary',
    'PICKUP': 'info',
    'IN_PROGRESS': 'success',
    'COMPLETED': 'success',
    'CANCELLED': 'danger'
  }
  return statusMap[status] || 'info'
}

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    'PENDING': '待派单',
    'ASSIGNED': '已派单',
    'PICKUP': '已上车',
    'IN_PROGRESS': '进行中',
    'COMPLETED': '已完成',
    'CANCELLED': '已取消'
  }
  return statusMap[status] || status
}

// 格式化日期
const formatDate = (date) => {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

// 加载数据
const loadData = async () => {
  // 这里应该调用实际的API
  // 目前使用模拟数据
  stats.value = {
    totalOrders: 1250,
    totalDrivers: 156,
    totalPassengers: 892,
    totalRevenue: '125.6万'
  }
  
  recentOrders.value = [
    {
      orderNumber: 'TX20241201001',
      passengerName: '张三',
      driverName: '李师傅',
      pickupAddress: '北京市朝阳区三里屯',
      totalFare: 45.5,
      status: 'COMPLETED',
      createdAt: '2024-12-01 10:30:00'
    },
    {
      orderNumber: 'TX20241201002',
      passengerName: '李四',
      driverName: '王师傅',
      pickupAddress: '北京市海淀区中关村',
      totalFare: 32.0,
      status: 'IN_PROGRESS',
      createdAt: '2024-12-01 11:15:00'
    }
  ]
}

// 获取最近标题
const getRecentTitle = () => {
  if (userStore.isPassenger) {
    return '最近行程'
  } else if (userStore.isDriver) {
    return '最近订单'
  }
  return '最近订单'
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.overview {
  padding: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.stats-card {
  height: 120px;
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

.stats-icon.orders {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stats-icon.drivers {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stats-icon.passengers {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stats-icon.revenue {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
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

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.recent-orders {
  margin-bottom: 20px;
}
</style> 