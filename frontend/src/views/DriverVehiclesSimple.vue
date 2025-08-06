<template>
  <div class="driver-vehicles-simple">
    <div class="page-header">
      <h2>我的车辆</h2>
      <div class="header-actions">
        <el-button type="primary" @click="showAddDialog">
          <el-icon><Plus /></el-icon>
          添加车辆
        </el-button>
        <el-button @click="loadVehicles" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <div class="vehicles-container">
      <div v-if="loading" class="loading-state">
        <el-icon class="is-loading" style="font-size: 32px;"><Loading /></el-icon>
        <div style="margin-top: 10px;">加载中...</div>
      </div>

      <div v-else-if="vehicles.length === 0" class="empty-state">
        <el-empty description="暂无车辆信息">
          <el-button type="primary" @click="showAddDialog">添加第一辆车</el-button>
        </el-empty>
      </div>

      <div v-else class="vehicles-grid">
        <div v-for="vehicle in vehicles" :key="vehicle.id" class="vehicle-card" :class="{ active: vehicle.isActive }">
          <div class="vehicle-header">
            <div class="plate-number">{{ vehicle.plateNumber }}</div>
            <div v-if="vehicle.isActive" class="current-badge">
              <el-icon><Check /></el-icon>
              当前使用
            </div>
          </div>
          
          <div class="vehicle-info">
            <div class="info-row">
              <span class="label">品牌型号:</span>
              <span class="value">{{ vehicle.brand }} {{ vehicle.model }}</span>
            </div>
            <div class="info-row">
              <span class="label">颜色年份:</span>
              <span class="value">{{ vehicle.color }} · {{ vehicle.year }}年</span>
            </div>
            <div class="info-row">
              <span class="label">座位数:</span>
              <span class="value">{{ vehicle.seats }}座</span>
            </div>
            <div class="info-row">
              <span class="label">车辆类型:</span>
              <span class="value">{{ getVehicleTypeText(vehicle.vehicleType) }}</span>
            </div>
          </div>

          <div class="vehicle-actions">
            <el-button 
              v-if="!vehicle.isActive" 
              type="primary" 
              size="small" 
              @click="setActiveVehicle(vehicle.id)"
              :loading="actionLoading === vehicle.id"
            >
              设为当前
            </el-button>
            <el-button 
              type="primary" 
              size="small" 
              @click="editVehicle(vehicle)"
              plain
            >
              编辑
            </el-button>
            <el-button 
              type="danger" 
              size="small" 
              @click="deleteVehicle(vehicle.id)" 
              :disabled="vehicle.isActive"
              plain
            >
              删除
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 添加/编辑车辆对话框 -->
    <el-dialog
      :title="isEdit ? '编辑车辆' : '添加车辆'"
      v-model="dialogVisible"
      width="500px"
      @close="resetForm"
    >
      <el-form :model="vehicleForm" :rules="formRules" ref="formRef" label-width="80px">
        <el-form-item label="车牌号" prop="plateNumber">
          <el-input v-model="vehicleForm.plateNumber" placeholder="如：京A12345" />
        </el-form-item>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="品牌" prop="brand">
              <el-input v-model="vehicleForm.brand" placeholder="如：丰田" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="型号" prop="model">
              <el-input v-model="vehicleForm.model" placeholder="如：凯美瑞" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="颜色" prop="color">
              <el-select v-model="vehicleForm.color" placeholder="选择颜色">
                <el-option label="白色" value="白色" />
                <el-option label="黑色" value="黑色" />
                <el-option label="银色" value="银色" />
                <el-option label="灰色" value="灰色" />
                <el-option label="红色" value="红色" />
                <el-option label="蓝色" value="蓝色" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年份" prop="year">
              <el-date-picker
                v-model="vehicleForm.year"
                type="year"
                placeholder="选择年份"
                value-format="YYYY"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="座位数" prop="seats">
              <el-select v-model="vehicleForm.seats" placeholder="选择座位数">
                <el-option label="4座" :value="4" />
                <el-option label="5座" :value="5" />
                <el-option label="7座" :value="7" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="车辆类型" prop="vehicleType">
              <el-select v-model="vehicleForm.vehicleType" placeholder="选择类型">
                <el-option label="经济型" value="ECONOMY" />
                <el-option label="舒适型" value="COMFORT" />
                <el-option label="高级型" value="PREMIUM" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm" :loading="submitLoading">
            {{ isEdit ? '更新' : '添加' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Loading, Check } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

// 使用真实的用户store
const userStore = useUserStore()

// 响应式数据
const vehicles = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const submitLoading = ref(false)
const isEdit = ref(false)
const formRef = ref()
const actionLoading = ref(null)

// 表单数据
const vehicleForm = reactive({
  id: null,
  plateNumber: '',
  brand: '',
  model: '',
  color: '',
  year: new Date().getFullYear(),
  seats: 5,
  vehicleType: 'COMFORT',
  fuelType: 'GASOLINE'
})

// 表单验证规则
const formRules = {
  plateNumber: [
    { required: true, message: '请输入车牌号', trigger: 'blur' }
  ],
  brand: [
    { required: true, message: '请输入品牌', trigger: 'blur' }
  ],
  model: [
    { required: true, message: '请输入型号', trigger: 'blur' }
  ],
  color: [
    { required: true, message: '请选择颜色', trigger: 'change' }
  ]
}

// 计算属性
const driverId = computed(() => {
  // 从用户信息中获取司机ID
  if (userStore.user?.userType === 'DRIVER') {
    return userStore.user.id
  }
  return null
})

// 加载车辆列表
const loadVehicles = async () => {
  loading.value = true
  try {
    // 检查用户是否已登录且为司机
    if (!userStore.token) {
      ElMessage.error('请先登录')
      vehicles.value = []
      return
    }

    if (!driverId.value) {
      ElMessage.error('无法获取司机信息')
      vehicles.value = []
      return
    }

    const response = await fetch(`/api/vehicles/driver/${driverId.value}`, {
      headers: {
        'Authorization': `Bearer ${userStore.token}`,
        'Content-Type': 'application/json'
      }
    })

    const result = await response.json()
    
    if (result.code === 200) {
      vehicles.value = result.data || []
      console.log('加载车辆数据:', vehicles.value)
    } else {
      ElMessage.error(result.message || '加载车辆列表失败')
      vehicles.value = []
    }
  } catch (error) {
    console.error('加载车辆列表失败:', error)
    ElMessage.error('网络错误，请稍后重试')
    vehicles.value = []
  } finally {
    loading.value = false
  }
}

// 显示添加对话框
const showAddDialog = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

// 编辑车辆
const editVehicle = (vehicle) => {
  isEdit.value = true
  Object.assign(vehicleForm, {
    id: vehicle.id,
    plateNumber: vehicle.plateNumber,
    brand: vehicle.brand,
    model: vehicle.model,
    color: vehicle.color,
    year: vehicle.year,
    seats: vehicle.seats,
    vehicleType: vehicle.vehicleType,
    fuelType: vehicle.fuelType
  })
  dialogVisible.value = true
}

// 重置表单
const resetForm = () => {
  Object.assign(vehicleForm, {
    id: null,
    plateNumber: '',
    brand: '',
    model: '',
    color: '',
    year: new Date().getFullYear(),
    seats: 5,
    vehicleType: 'COMFORT',
    fuelType: 'GASOLINE'
  })
  if (formRef.value) {
    formRef.value.resetFields()
  }
}

// 提交表单
const submitForm = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
  } catch (error) {
    return
  }

  submitLoading.value = true
  try {
    if (!userStore.token) {
      ElMessage.error('请先登录')
      return
    }

    if (!driverId.value) {
      ElMessage.error('无法获取司机信息')
      return
    }

    const vehicleData = {
      ...vehicleForm,
      driverId: driverId.value,
      year: parseInt(vehicleForm.year)
    }

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
    
    if (result.code === 200) {
      ElMessage.success(isEdit.value ? '车辆更新成功' : '车辆添加成功')
      dialogVisible.value = false
      loadVehicles()
    } else {
      ElMessage.error(result.message || '操作失败')
    }
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error('网络错误，请稍后重试')
  } finally {
    submitLoading.value = false
  }
}

// 设置激活车辆
const setActiveVehicle = async (vehicleId) => {
  actionLoading.value = vehicleId
  try {
    if (!userStore.token) {
      ElMessage.error('请先登录')
      return
    }

    if (!driverId.value) {
      ElMessage.error('无法获取司机信息')
      return
    }

    const response = await fetch(`/api/vehicles/driver/${driverId.value}/active/${vehicleId}`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${userStore.token}`,
        'Content-Type': 'application/json'
      }
    })

    const result = await response.json()
    
    if (result.code === 200) {
      ElMessage.success('已设置为当前车辆')
      await loadVehicles()
    } else {
      ElMessage.error(result.message || '设置失败')
    }
  } catch (error) {
    console.error('设置激活车辆失败:', error)
    ElMessage.error('网络错误，请稍后重试')
  } finally {
    actionLoading.value = null
  }
}

// 删除车辆
const deleteVehicle = async (vehicleId) => {
  try {
    await ElMessageBox.confirm('确定要删除这辆车吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    if (!userStore.token) {
      ElMessage.error('请先登录')
      return
    }

    const response = await fetch(`/api/vehicles/${vehicleId}`, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${userStore.token}`,
        'Content-Type': 'application/json'
      }
    })

    const result = await response.json()
    
    if (result.code === 200) {
      ElMessage.success('车辆删除成功')
      loadVehicles()
    } else {
      ElMessage.error(result.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除车辆失败:', error)
      ElMessage.error('网络错误，请稍后重试')
    }
  }
}

// 获取车辆类型文本
const getVehicleTypeText = (type) => {
  const typeMap = {
    'ECONOMY': '经济型',
    'COMFORT': '舒适型',
    'PREMIUM': '高级型',
    'LUXURY': '豪华型'
  }
  return typeMap[type] || '经济型'
}

// 页面加载时获取车辆列表
onMounted(() => {
  loadVehicles()
})
</script>

<style scoped>
.driver-vehicles-simple {
  padding: 0;
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
  min-height: 400px;
}

.loading-state {
  text-align: center;
  padding: 60px 20px;
  color: #666;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.vehicles-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
}

.vehicle-card {
  background: white;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px;
  transition: all 0.3s ease;
  position: relative;
}

.vehicle-card:hover {
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.vehicle-card.active {
  border-color: #67c23a;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
}

.vehicle-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f3f4f6;
}

.plate-number {
  font-weight: 700;
  color: #1f2937;
  font-size: 22px;
  font-family: 'Courier New', monospace;
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
  padding: 8px 16px;
  border-radius: 8px;
  border: 2px solid #cbd5e1;
  letter-spacing: 2px;
}

.current-badge {
  display: flex;
  align-items: center;
  gap: 4px;
  background: #67c23a;
  color: white;
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.vehicle-info {
  margin-bottom: 16px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 14px;
}

.info-row:last-child {
  margin-bottom: 0;
}

.label {
  color: #6b7280;
  font-weight: 500;
}

.value {
  color: #374151;
  font-weight: 600;
}

.vehicle-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .vehicles-grid {
    grid-template-columns: 1fr;
  }
  
  .page-header {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }

  .header-actions {
    justify-content: center;
  }

  .vehicle-actions {
    justify-content: center;
  }
}
</style>