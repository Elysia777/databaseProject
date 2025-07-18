<template>
  <div class="complaints">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>投诉处理</span>
        </div>
      </template>
      
      <el-table :data="complaintList" style="width: 100%">
        <el-table-column prop="orderNumber" label="订单号" width="180" />
        <el-table-column prop="complainantName" label="投诉人" width="120" />
        <el-table-column prop="defendantName" label="被投诉人" width="120" />
        <el-table-column prop="complaintType" label="投诉类型" width="120">
          <template #default="scope">
            <el-tag :type="getComplaintTypeTag(scope.row.complaintType)">
              {{ getComplaintTypeText(scope.row.complaintType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="投诉标题" width="200" />
        <el-table-column prop="status" label="处理状态" width="120">
          <template #default="scope">
            <el-tag :type="getStatusTag(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="投诉时间" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button size="small">查看</el-button>
            <el-button 
              v-if="scope.row.status === 'PENDING'" 
              size="small" 
              type="primary"
            >
              处理
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import dayjs from 'dayjs'

const complaintList = ref([
  {
    orderNumber: 'TX20241201001',
    complainantName: '张三',
    defendantName: '李师傅',
    complaintType: 'SERVICE',
    title: '司机服务态度差',
    status: 'PENDING',
    createdAt: '2024-12-01 10:30:00'
  },
  {
    orderNumber: 'TX20241201002',
    complainantName: '王师傅',
    defendantName: '李四',
    complaintType: 'PAYMENT',
    title: '乘客拒绝支付车费',
    status: 'INVESTIGATING',
    createdAt: '2024-12-01 11:15:00'
  }
])

const getComplaintTypeTag = (type) => {
  const typeMap = {
    'SERVICE': 'warning',
    'SAFETY': 'danger',
    'PAYMENT': 'info',
    'BEHAVIOR': 'primary',
    'OTHER': ''
  }
  return typeMap[type] || ''
}

const getComplaintTypeText = (type) => {
  const typeMap = {
    'SERVICE': '服务问题',
    'SAFETY': '安全问题',
    'PAYMENT': '支付问题',
    'BEHAVIOR': '行为问题',
    'OTHER': '其他'
  }
  return typeMap[type] || type
}

const getStatusTag = (status) => {
  const statusMap = {
    'PENDING': 'warning',
    'INVESTIGATING': 'primary',
    'RESOLVED': 'success',
    'CLOSED': 'info'
  }
  return statusMap[status] || ''
}

const getStatusText = (status) => {
  const statusMap = {
    'PENDING': '待处理',
    'INVESTIGATING': '调查中',
    'RESOLVED': '已解决',
    'CLOSED': '已关闭'
  }
  return statusMap[status] || status
}

const formatDate = (date) => {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}
</script>

<style scoped>
.complaints {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style> 