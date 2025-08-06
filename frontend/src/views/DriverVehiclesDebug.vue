<template>
  <div class="driver-vehicles-debug">
    <!-- 调试信息 -->
    <div class="debug-info">
      <h3>🔧 调试信息</h3>
      <div>用户信息: {{ userStore.user ? `${userStore.user.username} (ID: ${userStore.user.id})` : '未登录' }}</div>
      <div>Token: {{ userStore.token ? '✅ 存在' : '❌ 不存在' }}</div>
      <div>司机ID: {{ driverId || '未获取到' }}</div>
      <div>车辆数量: {{ vehicles.length }}</div>
    </div>

    <!-- 页面头部 -->
    <div class="page-header">
      <h2>我的车辆 (调试版)</h2>
      <div class="header-actions">
        <el-button 
          type="primary" 
          @click="handleAddClick"
          :disabled="!userStore.token"
        >
          添加车辆
        </el-button>
        <el-button 
          @click="handleRefreshClick" 
          :loading="loading"
          :disabled="!userStore.token"
        >
          刷新
        </el-button>
        <el-button @click="handleDebugClick" type="warning">
          调试测试
        </el-button>
      </div>
    </div>

    <!-- 车辆列表 -->
    <div class="vehicles-container">
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-state">
        <div>加载中...</div>
      </div>

      <!-- 空状态 -->
      <div v-else-if="vehicles.length === 0" class="empty-state">
        <div>暂无车辆信息</div>
        <el-button type="primary" @click="handleAddClick" :disabled="!userStore.token">
          添加第一辆车
        </el-button>
      </div>

      <!-- 车辆列表 -->
      <div v-else class="vehicles-list">
        <div 
          v-for="vehicle in vehicles" 
          :key="vehicle.id" 
          class="vehicle-item"
          :class="{ active: vehicle.isActive }"
        >
          <div class="vehicle-info">
            <div class="plate-number">{{ vehicle.plateNumber }}</div>
            <div class="vehicle-details">
              {{ vehicle.brand }} {{ vehicle.model }} - {{ vehicle.color }} - {{ vehicle.year }}年
            </div>
            <div v-if="vehicle.isActive" class="active-badge">当前使用</div>
          </div>
          
          <div class="vehicle-actions">
            <el-button 
              v-if="!vehicle.isActive" 
              type="primary" 
              size="small" 
              @click="handleSetActiveClick(vehicle.id)"
              :loading="actionLoading === vehicle.id"
            >
              设为当前
            </el-button>
            <el-button 
              type="primary" 
              size="small" 
              @click="handleEditClick(vehicle)"
              plain
            >
              编辑
            </el-button>
            <el-button 
              type="danger" 
              size="small" 
              @click="handleDeleteClick(vehicle.id)" 
              :disabled="vehicle.isActive"
              plain
            >
              删除
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 添加/编辑对话框 -->
    <el-dialog
      :title="isEdit ? '编辑车辆' : '添加车辆'"
      v-model="dialogVisible"
      width="500px"
    >
      <el-form :model="vehicleForm" label-width="80px">
        <el-form-item label="车牌号">
          <el-input v-model="vehicleForm.plateNumber" placeholder="如：京A12345" />
        </el-form-item>
        <el-form-item label="品牌">
          <el-input v-model="vehicleForm.brand" placeholder="如：丰田" />
        </el-form-item>
        <el-form-item label="型号">
          <el-input v-model="vehicleForm.model" placeholder="如：凯美瑞" />
        </el-form-item>
        <el-form-item label="颜色">
          <el-select v-model="vehicleForm.color" placeholder="选择颜色" style="width: 100%">
            <el-option label="白色" value="白色" />
            <el-option label="黑色" value="黑色" />
            <el-option label="银色" value="银色" />
            <el-option label="红色" value="红色" />
          </el-select>
        </el-form-item>
        <el-form-item label="年份">
          <el-input v-model="vehicleForm.year" placeholder="如：2023" type="number" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmitClick" :loading="submitLoading">
            {{ isEdit ? '更新' : '添加' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 调试日志 -->
    <div class="debug-logs" v-if="debugLogs.length > 0">
      <h4>调试日志:</h4>
      <div v-for="(log, index) in debugLogs" :key="index" class="log-item">
        <strong>{{ log.action }}:</strong> {{ log.message }}
        <div v-if="log.data" class="log-data">{{ JSON.stringify(log.data, null, 2) }}</div>
      </div>
      <el-button @click="clearDebugLogs" size="small">清除日志</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

// 使用用户store
const userStore = useUserStore()

// 响应式数据
const vehicles = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const submitLoading = ref(false)
const isEdit = ref(false)
const actionLoading = ref(null)
const debugLogs = ref([])

// 表单数据
const vehicleForm = reactive({
  id: null,
  plateNumber: '',
  brand: '',
  model: '',
  color: '',
  year: new Date().getFullYear()
})

// 计算属性
const driverId = computed(() => {
  if (userStore.user?.userType === 'DRIVER') {
    return userStore.user.id
  }
  return null
})

// 添加调试日志
const addDebugLog = (action, message, data = null) => {
  debugLogs.value.push({
    action,
    message,
    data,
    timestamp: new Date().toLocaleTimeString()
  })
  console.log(`[${action}] ${message}`, data)
}

// 清除调试日志
const clearDebugLogs = () => {
  debugLogs.value = []
}

// 处理添加按钮点击
const handleAddClick = () => {
  addDebugLog('按钮点击', '添加车辆按钮被点击')
  
  if (!userStore.token) {
    ElMessage.error('请先登录')
    addDebugLog('错误', '用户未登录')
    return
  }
  
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
  addDebugLog('对话框', '添加车辆对话框已打开')
}

// 处理刷新按钮点击
const handleRefreshClick = () => {
  addDebugLog('按钮点击', '刷新按钮被点击')
  loadVehicles()
}

// 处理调试按钮点击
const handleDebugClick = () => {
  addDebugLog('调试测试', '调试按钮被点击', {
    userStore: {
      user: userStore.user,
      token: userStore.token ? '存在' : '不存在'
    },
    driverId: driverId.value,
    vehiclesCount: vehicles.value.length
  })
  ElMessage.info('调试信息已记录到控制台')
}

// 处理编辑按钮点击
const handleEditClick = (vehicle) => {
  addDebugLog('按钮点击', `编辑车辆按钮被点击，车辆ID: ${vehicle.id}`)
  
  if (!userStore.token) {
    ElMessage.error('请先登录')
    return
  }
  
  isEdit.value = true
  Object.assign(vehicleForm, {
    id: vehicle.id,
    plateNumber: vehicle.plateNumber,
    brand: vehicle.brand,
    model: vehicle.model,
    color: vehicle.color,
    year: vehicle.year
  })
  dialogVisible.value = true
  addDebugLog('对话框', '编辑车辆对话框已打开')
}

// 处理设置激活按钮点击
const handleSetActiveClick = async (vehicleId) => {
  addDebugLog('按钮点击', `设置激活车辆按钮被点击，车辆ID: ${vehicleId}`)
  
  if (!userStore.token) {
    ElMessage.error('请先登录')
    return
  }

  if (!driverId.value) {
    ElMessage.error('无法获取司机信息')
    return
  }

  actionLoading.value = vehicleId
  try {
    addDebugLog('API调用', `开始设置激活车辆，司机ID: ${driverId.value}, 车辆ID: ${vehicleId}`)

    const response = await fetch(`/api/vehicles/driver/${driverId.value}/active/${vehicleId}`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${userStore.token}`,
        'Content-Type': 'application/json'
      }
    })

    const result = await response.json()
    addDebugLog('API响应', '设置激活车辆响应', result)
    
    if (result.code === 200) {
      ElMessage.success('已设置为当前车辆')
      await loadVehicles()
    } else {
      ElMessage.error(result.message || '设置失败')
    }
  } catch (error) {
    addDebugLog('API错误', '设置激活车辆失败', error.message)
    ElMessage.error(`设置失败: ${error.message}`)
  } finally {
    actionLoading.value = null
  }
}

// 处理删除按钮点击
const handleDeleteClick = async (vehicleId) => {
  addDebugLog('按钮点击', `删除车辆按钮被点击，车辆ID: ${vehicleId}`)
  
  if (!userStore.token) {
    ElMessage.error('请先登录')
    return
  }

  try {
    await ElMessageBox.confirm('确定要删除这辆车吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    addDebugLog('API调用', `开始删除车辆，车辆ID: ${vehicleId}`)

    const response = await fetch(`/api/vehicles/${vehicleId}`, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${userStore.token}`,
        'Content-Type': 'application/json'
      }
    })

    const result = await response.json()
    addDebugLog('API响应', '删除车辆响应', result)
    
    if (result.code === 200) {
      ElMessage.success('车辆删除成功')
      await loadVehicles()
    } else {
      ElMessage.error(result.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      addDebugLog('API错误', '删除车辆失败', error.message)
      ElMessage.error(`删除失败: ${error.message}`)
    }
  }
}

// 处理提交按钮点击
const handleSubmitClick = async () => {
  addDebugLog('按钮点击', '提交表单按钮被点击')
  
  if (!userStore.token) {
    ElMessage.error('请先登录')
    return
  }

  if (!driverId.value) {
    ElMessage.error('无法获取司机信息')
    return
  }

  // 简单验证
  if (!vehicleForm.plateNumber || !vehicleForm.brand || !vehicleForm.model) {
    ElMessage.error('请填写完整信息')
    return
  }

  submitLoading.value = true
  try {
    const vehicleData = {
      ...vehicleForm,
      driverId: driverId.value,
      year: parseInt(vehicleForm.year),
      seats: 5,
      vehicleType: 'COMFORT',
      fuelType: 'GASOLINE'
    }

    addDebugLog('API调用', `开始${isEdit.value ? '更新' : '添加'}车辆`, vehicleData)

    let response
    if (isEdit.value) {
      response = await fetch(`/api/vehicles/${vehicleForm.id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${userStore.token}`
        },
        body: JSON.stringify(vehicleData)
      })
    } else {
      response = await fetch('/api/vehicles', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${userStore.token}`
        },
        body: JSON.stringify(vehicleData)
      })
    }

    const result = await response.json()
    addDebugLog('API响应', `${isEdit.value ? '更新' : '添加'}车辆响应`, result)
    
    if (result.code === 200) {
      ElMessage.success(isEdit.value ? '车辆更新成功' : '车辆添加成功')
      dialogVisible.value = false
      await loadVehicles()
    } else {
      ElMessage.error(result.message || '操作失败')
    }
  } catch (error) {
    addDebugLog('API错误', `${isEdit.value ? '更新' : '添加'}车辆失败`, error.message)
    ElMessage.error(`操作失败: ${error.message}`)
  } finally {
    submitLoading.value = false
  }
}

// 重置表单
const resetForm = () => {
  Object.assign(vehicleForm, {
    id: null,
    plateNumber: '',
    brand: '',
    model: '',
    color: '',
    year: new Date().getFullYear()
  })
}

// 加载车辆列表
const loadVehicles = async () => {
  loading.value = true
  try {
    if (!userStore.token) {
      addDebugLog('错误', '用户未登录，无法加载车辆')
      ElMessage.error('请先登录')
      vehicles.value = []
      return
    }

    if (!driverId.value) {
      addDebugLog('错误', '无法获取司机ID')
      ElMessage.error('无法获取司机信息，请重新登录')
      vehicles.value = []
      return
    }

    addDebugLog('API调用', `开始加载车辆，司机ID: ${driverId.value}`)

    const response = await fetch(`/api/vehicles/driver/${driverId.value}`, {
      headers: {
        'Authorization': `Bearer ${userStore.token}`,
        'Content-Type': 'application/json'
      }
    })

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`)
    }

    const result = await response.json()
    addDebugLog('API响应', '加载车辆响应', result)
    
    if (result.code === 200) {
      vehicles.value = result.data || []
      addDebugLog('成功', `成功加载${vehicles.value.length}辆车`)
      if (vehicles.value.length === 0) {
        ElMessage.info('暂无车辆信息，请添加车辆')
      }
    } else {
      addDebugLog('错误', '加载车辆失败', result.message)
      ElMessage.error(result.message || '加载车辆列表失败')
      vehicles.value = []
    }
  } catch (error) {
    addDebugLog('API错误', '加载车辆失败', error.message)
    ElMessage.error(`网络错误: ${error.message}`)
    vehicles.value = []
  } finally {
    loading.value = false
  }
}

// 页面加载时的初始化
onMounted(async () => {
  addDebugLog('页面加载', '组件已挂载', {
    user: userStore.user,
    token: userStore.token ? '存在' : '不存在'
  })
  
  // 如果没有用户信息但有token，尝试初始化用户信息
  if (userStore.token && !userStore.user) {
    try {
      addDebugLog('初始化', '尝试初始化用户信息')
      await userStore.initUserInfo()
    } catch (error) {
      addDebugLog('初始化错误', '初始化用户信息失败', error.message)
    }
  }
  
  // 加载车辆数据
  await loadVehicles()
})
</script>

<style scoped>
.driver-vehicles-debug {
  padding: 0;
}

.debug-info {
  background: #f0f9ff;
  border: 1px solid #3b82f6;
  border-radius: 8px;
  padding: 15px;
  margin-bottom: 20px;
}

.debug-info h3 {
  margin-top: 0;
  color: #1e40af;
}

.debug-info div {
  margin-bottom: 5px;
  color: #1e40af;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 0 4px;
}

.page-header h2 {
  margin: 0;
  color: #333;
  font-size: 24px;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.vehicles-container {
  min-height: 300px;
}

.loading-state, .empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #666;
}

.vehicles-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.vehicle-item {
  background: white;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: all 0.3s ease;
}

.vehicle-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.vehicle-item.active {
  border-color: #67c23a;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
}

.vehicle-info {
  flex: 1;
}

.plate-number {
  font-weight: 700;
  color: #1f2937;
  font-size: 18px;
  font-family: 'Courier New', monospace;
  margin-bottom: 5px;
}

.vehicle-details {
  color: #6b7280;
  font-size: 14px;
}

.active-badge {
  color: #67c23a;
  font-weight: 600;
  font-size: 12px;
  margin-top: 5px;
}

.vehicle-actions {
  display: flex;
  gap: 8px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.debug-logs {
  margin-top: 30px;
  background: #fef3c7;
  border: 1px solid #f59e0b;
  border-radius: 8px;
  padding: 15px;
}

.debug-logs h4 {
  margin-top: 0;
  color: #92400e;
}

.log-item {
  margin-bottom: 10px;
  padding: 8px;
  background: white;
  border-radius: 4px;
  font-size: 12px;
}

.log-data {
  background: #f8f8f8;
  padding: 5px;
  border-radius: 3px;
  font-family: monospace;
  white-space: pre-wrap;
  margin-top: 5px;
}
</style>