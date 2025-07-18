<template>
  <div class="statistics">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-icon orders">
              <el-icon><Document /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-number">{{ stats.todayOrders }}</div>
              <div class="stats-label">今日订单</div>
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
              <div class="stats-number">{{ stats.onlineDrivers }}</div>
              <div class="stats-label">在线司机</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-icon rating">
              <el-icon><Star /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-number">{{ stats.avgRating }}</div>
              <div class="stats-label">平均评分</div>
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
              <span>收入统计（近7天）</span>
            </div>
          </template>
          <div class="chart-container">
            <v-chart :option="revenueTrendOption" style="height: 300px" />
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 更多统计图表 -->
    <el-row :gutter="20" class="charts-row">
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>车辆类型分布</span>
            </div>
          </template>
          <div class="chart-container">
            <v-chart :option="vehicleTypeOption" style="height: 300px" />
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>订单状态分布</span>
            </div>
          </template>
          <div class="chart-container">
            <v-chart :option="orderStatusOption" style="height: 300px" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'

use([CanvasRenderer, LineChart, BarChart, PieChart, GridComponent, TooltipComponent, LegendComponent])

// 统计数据
const stats = ref({
  todayOrders: 0,
  todayRevenue: 0,
  onlineDrivers: 0,
  avgRating: 0
})

// 订单趋势图表配置
const orderTrendOption = ref({
  tooltip: {
    trigger: 'axis'
  },
  xAxis: {
    type: 'category',
    data: ['12-01', '12-02', '12-03', '12-04', '12-05', '12-06', '12-07']
  },
  yAxis: {
    type: 'value'
  },
  series: [
    {
      name: '订单数',
      type: 'line',
      data: [120, 132, 101, 134, 90, 230, 210],
      smooth: true
    }
  ]
})

// 收入趋势图表配置
const revenueTrendOption = ref({
  tooltip: {
    trigger: 'axis'
  },
  xAxis: {
    type: 'category',
    data: ['12-01', '12-02', '12-03', '12-04', '12-05', '12-06', '12-07']
  },
  yAxis: {
    type: 'value'
  },
  series: [
    {
      name: '收入',
      type: 'bar',
      data: [1200, 1320, 1010, 1340, 900, 2300, 2100]
    }
  ]
})

// 车辆类型分布图表配置
const vehicleTypeOption = ref({
  tooltip: {
    trigger: 'item'
  },
  legend: {
    orient: 'vertical',
    left: 'left'
  },
  series: [
    {
      name: '车辆类型',
      type: 'pie',
      radius: '50%',
      data: [
        { value: 45, name: '经济型' },
        { value: 30, name: '舒适型' },
        { value: 15, name: '豪华型' },
        { value: 10, name: '奢华型' }
      ],
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      }
    }
  ]
})

// 订单状态分布图表配置
const orderStatusOption = ref({
  tooltip: {
    trigger: 'item'
  },
  legend: {
    orient: 'vertical',
    left: 'left'
  },
  series: [
    {
      name: '订单状态',
      type: 'pie',
      radius: '50%',
      data: [
        { value: 60, name: '已完成' },
        { value: 20, name: '进行中' },
        { value: 10, name: '待派单' },
        { value: 10, name: '已取消' }
      ],
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      }
    }
  ]
})

// 加载数据
const loadData = async () => {
  // 这里应该调用实际的API
  // 目前使用模拟数据
  stats.value = {
    todayOrders: 156,
    todayRevenue: '12,580',
    onlineDrivers: 89,
    avgRating: 4.6
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.statistics {
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

.stats-icon.revenue {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.stats-icon.drivers {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stats-icon.rating {
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
</style> 