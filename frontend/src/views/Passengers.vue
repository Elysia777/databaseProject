<template>
  <div class="passengers">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ getPageTitle() }}</span>
        </div>
      </template>
      
      <el-table :data="passengerList" style="width: 100%">
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="phone" label="手机号" width="150" />
        <el-table-column prop="email" label="邮箱" width="200" />
        <el-table-column prop="rating" label="评分" width="100">
          <template #default="scope">
            <el-rate v-model="scope.row.rating" disabled show-score />
          </template>
        </el-table-column>
        <el-table-column prop="totalOrders" label="总订单" width="100" />
        <el-table-column prop="totalSpent" label="总消费" width="120">
          <template #default="scope">
            ¥{{ scope.row.totalSpent }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="scope">
            <el-button size="small">查看</el-button>
            <el-button size="small" type="primary">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useUserStore } from '@/stores/user'
import dayjs from 'dayjs'

const userStore = useUserStore()

const passengerList = ref([
  {
    realName: '张三',
    phone: '13800138001',
    email: 'zhangsan@example.com',
    rating: 4.5,
    totalOrders: 25,
    totalSpent: 1250.50,
    createdAt: '2024-01-01 10:00:00'
  },
  {
    realName: '李四',
    phone: '13800138002',
    email: 'lisi@example.com',
    rating: 4.8,
    totalOrders: 18,
    totalSpent: 890.00,
    createdAt: '2024-02-01 10:00:00'
  }
])

const formatDate = (date) => {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

// 获取页面标题
const getPageTitle = () => {
  if (userStore.isPassenger) {
    return '行程历史'
  }
  return '乘客管理'
}
</script>

<style scoped>
.passengers {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style> 