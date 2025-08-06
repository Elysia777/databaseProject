<template>
  <div class="driver-vehicles">
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
      <el-card v-if="loading" class="loading-card">
        <div class="loading-content">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>加载中...</span>
        </div>
      </el-card>

      <div v-else-if="vehicles.length === 0" class="empty-state">
        <el-empty description="暂无车辆信息">
          <el-button type="primary" @click="showAddDialog">添加第一辆车</el-button>
        </el-empty>
      </div>

      <div v-else class="vehicles-list">
        <el-card v-for="vehicle in vehicles" :key="vehicle.id" class="vehicle-card" :class="{ active: vehicle.isActive }">
          <div class="vehicle-header">
            <div class="vehicle-main-info">
              <div class="plate-number-section">
                <span class="plate-number">{{ vehicle.plateNumber }}</span>
                <el-tag v-if="vehicle.isActive" type="success" size="small">
                  <el-icon><Check /></el-icon>
                  当前使用
                </el-tag>
              </div>
              <div class="vehicle-basic-info">
                {{ vehicle.brand }} {{ vehicle.model }} · {{ vehicle.color }} · {{ vehicle.year }}年
              </div>
            </div>
            <div class="vehicle-actions">
              <el-button 
                v-if="!vehicle.isActive" 
                type="primary" 
                size="small" 
                @click.stop="handleSetActive(vehicle.id)"
                :loading="actionLoading === vehicle.id"
              >
                设为当前
              </el-button>
              <el-button 
                type="primary" 
                size="small" 
                @click.stop="handleEdit(vehicle)"
                plain
              >
                <el-icon><Edit /></el-icon>
              </el-button>
              <el-button 
                type="danger" 
                size="small" 
                @click.stop="handleDelete(vehicle.id)" 
                :disabled="vehicle.isActive"
                plain
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>

          <div class="vehicle-details">
            <div class="detail-item">
              <el-icon><User /></el-icon>
              <span>{{ vehicle.seats }}座</span>
            </div>
            <div class="detail-item">
              <el-icon><Setting /></el-icon>
              <span>{{ getVehicleTypeText(vehicle.vehicleType) }}</span>
            </div>
            <div class="detail-item">
              <el-icon><Lightning /></el-icon>
              <span>{{ getFuelTypeText(vehicle.fuelType) }}</span>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 添加/编辑车辆对话框 -->
    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="600px"
      @close="resetForm"
    >
      <el-form :model="vehicleForm" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="车牌号" prop="plateNumber">
          <el-input v-model="vehicleForm.plateNumber" placeholder="请输入车牌号，如：京A12345" />
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
                <el-option label="其他" value="其他" />
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
                <el-option label="8座" :value="8" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="车辆类型" prop="vehicleType">
              <el-select v-model="vehicleForm.vehicleType" placeholder="选择车辆类型">
                <el-option label="经济型" value="ECONOMY" />
                <el-option label="舒适型" value="COMFORT" />
                <el-option label="高级型" value="PREMIUM" />
                <el-option label="豪华型" value="LUXURY" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="燃料类型" prop="fuelType">
          <el-select v-model="vehicleForm.fuelType" placeholder="选择燃料类型">
            <el-option label="汽油" value="GASOLINE" />
            <el-option label="柴油" value="DIESEL" />
            <el-option label="电动" value="ELECTRIC" />
            <el-option label="混合动力" value="HYBRID" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm" :loading="submitLoading">
            {{ isEdit ? '更新' : '添加' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Edit, Delete, Loading, Check, User, Setting, Lightning } from '@element-plus/icons-vue'

// 模拟用户store
const mockUserStore = {
  user: { id: 1, driverId: 1 },
  token: localStorage.getItem('token') || ''
}

// 响应式数据
const vehicles = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const submitLoading = ref(false)
const isEdit = ref(false)
const formRef = ref()
const actionLoading = ref(null) // 用于按钮加载状态

// 表单数据
const vehicleForm = reactive({
  id: null,
  plateNumber: '',
  brand: '',
  model: '',
  color: '',
  year: new Date().getFullYear(),
  seats: 5,
  vehicleType: 'ECONOMY',
  fuelType: 'GASOLINE'
})

// 表单验证规则
const formRules = {
  plateNumber: [
    { required: true, message: '请输入车牌号', trigger: 'blur' },
    { pattern: /^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼使领][A-Z][A-Z0-9]{4,5}[A-Z0-9挂学警港澳]$/, message: '请输入正确的车牌号格式', trigger: 'blur' }
  ],
  brand: [
    { required: true, message: '请输入品牌', trigger: 'blur' }
  ],
  model: [
    { required: true, message: '请输入型号', trigger: 'blur' }
  ],
  color: [
    { required: true, message: '请选择颜色', trigger: 'change' }
  ],
  year: [
    { required: true, message: '请选择年份', trigger: 'change' }
  ],
  seats: [
    { required: true, message: '请选择座位数', trigger: 'change' }
  ],
  vehicleType: [
    { required: true, message: '请选择车辆类型', trigger: 'change' }
  ],
  fuelType: [
    { required: true, message: '请选择燃料类型', trigger: 'change' }
  ]
}

// 计算属性
const driverId = computed(() => mockUserStore.user?.driverId || mockUserStore.user?.id || 1)
const dialogTitle = computed(() => isEdit.value ? '编辑车辆' : '添加车辆')

// 加载车辆列表
const loadVehicles = async () => {
  loading.value = true
  try {
    const token = localStorage.getItem('token')
    if (!token) {
      ElMessage.warning('请先登录')
      // 模拟数据
      vehicles.value = [
        {
          id: 1,
          plateNumber: '京A12345',
          brand: '丰田',
          model: '凯美瑞',
          color: '白色',
          year: 2022,
          seats: 5,
          vehicleType: 'COMFORT',
          fuelType: 'GASOLINE',
          isActive: true
        }
      ]
      return
    }

    const response = await fetch(`/api/vehicles/driver/${driverId.value}`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })

    const result = await response.json()
    
    if (result.code === 200) {
      vehicles.value = result.data || []
    } else {
      ElMessage.error(result.message || '加载车辆列表失败')
    }
  } catch (error) {
    console.error('加载车辆列表失败:', error)
    ElMessage.error('网络错误，请稍后重试')
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
    vehicleType: 'ECONOMY',
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
    const token = localStorage.getItem('token')
    if (!token) {
      ElMessage.error('请先登录')
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
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(vehicleData)
      })
    } else {
      response = await fetch('/api/vehicles', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
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

// 处理设置激活车辆
const handleSetActive = async (vehicleId) => {
  actionLoading.value = vehicleId
  try {
    const token = localStorage.getItem('token')
    if (!token) {
      ElMessage.error('请先登录')
      return
    }

    const response = await fetch(`/api/vehicles/driver/${driverId.value}/active/${vehicleId}`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`
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

// 处理编辑车辆
const handleEdit = (vehicle) => {
  editVehicle(vehicle)
}

// 处理删除车辆
const handleDelete = async (vehicleId) => {
  try {
    await ElMessageBox.confirm('确定要删除这辆车吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteVehicle(vehicleId)
  } catch (error) {
    // 用户取消删除
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

    const token = localStorage.getItem('token')
    if (!token) {
      ElMessage.error('请先登录')
      return
    }

    const response = await fetch(`/api/vehicles/${vehicleId}`, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${token}`
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
  return typeMap[type] || type
}

// 获取燃料类型文本
const getFuelTypeText = (type) => {
  const typeMap = {
    'GASOLINE': '汽油',
    'DIESEL': '柴油',
    'ELECTRIC': '电动',
    'HYBRID': '混动'
  }
  return typeMap[type] || type
}

// 页面加载时获取车辆列表
onMounted(() => {
  loadVehicles()
})

// 导出方法供模板使用
defineExpose({
  loadVehicles,
  showAddDialog,
  editVehicle,
  resetForm,
  submitForm,
  handleSetActive,
  handleEdit,
  handleDelete,
  getVehicleTypeText,
  getFuelTypeText
})
</script>

<style scoped>
.driver-vehicles {
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

.vehicles-container {
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

.vehicles-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.vehicle-card {
  transition: all 0.3s ease;
}

.vehicle-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.vehicle-card.active {
  border: 2px solid #67c23a;
}

.vehicle-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.vehicle-main-info {
  flex: 1;
}

.plate-number-section {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.plate-number {
  font-weight: 700;
  color: #1f2937;
  font-size: 20px;
  font-family: 'Courier New', monospace;
  background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%);
  padding: 8px 12px;
  border-radius: 6px;
  border: 2px solid #d1d5db;
  letter-spacing: 1px;
}

.vehicle-basic-info {
  color: #6b7280;
  font-size: 14px;
  font-weight: 500;
}

.vehicle-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.vehicle-details {
  display: flex;
  gap: 24px;
  padding-top: 12px;
  border-top: 1px solid #f3f4f6;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #6b7280;
  font-size: 14px;
}



.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
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

  .vehicle-header {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }

  .vehicle-actions {
    justify-content: center;
  }

  .vehicle-details {
    grid-template-columns: 1fr;
  }
}
</style>