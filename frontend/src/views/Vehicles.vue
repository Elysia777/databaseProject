<template>
  <div class="vehicles">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>车辆管理</span>
          <el-button type="primary">添加车辆</el-button>
        </div>
      </template>
      
      <el-table :data="vehicleList" style="width: 100%">
        <el-table-column prop="plateNumber" label="车牌号" width="150" />
        <el-table-column prop="brand" label="品牌" width="120" />
        <el-table-column prop="model" label="型号" width="120" />
        <el-table-column prop="color" label="颜色" width="100" />
        <el-table-column prop="vehicleType" label="类型" width="100">
          <template #default="scope">
            <el-tag :type="getVehicleTypeTag(scope.row.vehicleType)">
              {{ getVehicleTypeText(scope.row.vehicleType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="driverName" label="司机" width="120" />
        <el-table-column prop="isActive" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.isActive ? 'success' : 'danger'">
              {{ scope.row.isActive ? '激活' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button size="small">查看</el-button>
            <el-button size="small" type="primary">编辑</el-button>
            <el-button size="small" type="danger">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const vehicleList = ref([
  {
    plateNumber: '京A12345',
    brand: '丰田',
    model: '凯美瑞',
    color: '白色',
    vehicleType: 'COMFORT',
    driverName: '李师傅',
    isActive: true
  },
  {
    plateNumber: '京B67890',
    brand: '本田',
    model: '雅阁',
    color: '黑色',
    vehicleType: 'ECONOMY',
    driverName: '王师傅',
    isActive: true
  }
])

const getVehicleTypeTag = (type) => {
  const typeMap = {
    'ECONOMY': '',
    'COMFORT': 'success',
    'PREMIUM': 'warning',
    'LUXURY': 'danger'
  }
  return typeMap[type] || ''
}

const getVehicleTypeText = (type) => {
  const typeMap = {
    'ECONOMY': '经济型',
    'COMFORT': '舒适型',
    'PREMIUM': '豪华型',
    'LUXURY': '奢华型'
  }
  return typeMap[type] || type
}
</script>

<style scoped>
.vehicles {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style> 