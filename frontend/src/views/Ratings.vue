<template>
  <div class="ratings">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>评价管理</span>
        </div>
      </template>
      
      <el-table :data="ratingList" style="width: 100%">
        <el-table-column prop="orderNumber" label="订单号" width="180" />
        <el-table-column prop="raterName" label="评价人" width="120" />
        <el-table-column prop="ratedName" label="被评价人" width="120" />
        <el-table-column prop="ratingType" label="评价类型" width="120">
          <template #default="scope">
            <el-tag :type="scope.row.ratingType === 'PASSENGER_TO_DRIVER' ? 'primary' : 'success'">
              {{ scope.row.ratingType === 'PASSENGER_TO_DRIVER' ? '乘客评司机' : '司机评乘客' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="rating" label="评分" width="100">
          <template #default="scope">
            <el-rate v-model="scope.row.rating" disabled show-score />
          </template>
        </el-table-column>
        <el-table-column prop="comment" label="评价内容" />
        <el-table-column prop="createdAt" label="评价时间" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="scope">
            <el-button size="small">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import dayjs from 'dayjs'

const ratingList = ref([
  {
    orderNumber: 'TX20241201001',
    raterName: '张三',
    ratedName: '李师傅',
    ratingType: 'PASSENGER_TO_DRIVER',
    rating: 5.0,
    comment: '司机服务很好，车很干净，驾驶技术也不错',
    createdAt: '2024-12-01 11:30:00'
  },
  {
    orderNumber: 'TX20241201002',
    raterName: '王师傅',
    ratedName: '李四',
    ratingType: 'DRIVER_TO_PASSENGER',
    rating: 4.5,
    comment: '乘客很有礼貌，准时到达上车地点',
    createdAt: '2024-12-01 12:15:00'
  }
])

const formatDate = (date) => {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}
</script>

<style scoped>
.ratings {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style> 