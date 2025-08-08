<template>
  <div class="vehicle-management">
    <div class="page-header">
      <h2>车辆管理</h2>
      <div class="header-actions">
        <el-button @click="refreshData" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button type="primary" @click="showAddDialog">
          <el-icon><Plus /></el-icon>
          添加车辆
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-icon total">
              <el-icon><Van /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-number">{{ stats.totalVehicles }}</div>
              <div class="stats-label">总车辆数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-icon active">
              <el-icon><CircleCheck /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-number">{{ stats.activeVehicles }}</div>
              <div class="stats-label">活跃车辆</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-icon pending">
              <el-icon><Clock /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-number">{{ stats.pendingVehicles }}</div>
              <div class="stats-label">待审核</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-icon rejected">
              <el-icon><CircleClose /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-number">{{ stats.rejectedVehicles }}</div>
              <div class="stats-label">已拒绝</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选条件 -->
    <el-card class="filter-card">
      <el-form :model="filters" inline>
        <el-form-item label="车辆状态">
          <el-select v-model="filters.status" placeholder="选择状态" clearable style="width: 150px">
            <el-option label="活跃" value="ACTIVE" />
            <el-option label="待审核" value="PENDING" />
            <el-option label="已拒绝" value="REJECTED" />
            <el-option label="已停用" value="INACTIVE" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="车辆类型">
          <el-select v-model="filters.type" placeholder="选择类型" clearable style="width: 150px">
            <el-option label="轿车" value="SEDAN" />
            <el-option label="SUV" value="SUV" />
            <el-option label="MPV" value="MPV" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="司机姓名">
          <el-input
            v-model="filters.driverName"
            placeholder="搜索司机姓名"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        
        <el-form-item label="车牌号">
          <el-input
            v-model="filters.plateNumber"
            placeholder="搜索车牌号"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="searchVehicles">搜索</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 车辆列表 -->
    <el-card class="table-card">
      <el-table
        :data="vehicles"
        v-loading="loading"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        
        <el-table-column prop="id" label="ID" width="80" />
        
        <el-table-column label="车辆图片" width="100">
          <template #default="scope">
            <el-image
              :src="getVehicleImageUrl(scope.row.vehicleImage)"
              :preview-src-list="[getVehicleImageUrl(scope.row.vehicleImage)]"
              fit="cover"
              style="width: 60px; height: 40px; border-radius: 4px;"
              @error="handleImageError"
            >
              <template #error>
                <div class="image-slot">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
          </template>
        </el-table-column>
        
        <el-table-column prop="plateNumber" label="车牌号" width="120" />
        
        <el-table-column prop="brand" label="品牌" width="100" />
        
        <el-table-column prop="model" label="型号" width="120" />
        
        <el-table-column prop="color" label="颜色" width="80" />
        
        <el-table-column label="车辆类型" width="100">
          <template #default="scope">
            <el-tag :type="getVehicleTypeTagType(scope.row.vehicleType)">
              {{ getVehicleTypeText(scope.row.vehicleType) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column label="司机信息" width="150">
          <template #default="scope">
            <div class="driver-info" v-if="scope.row.driverName">
              <img 
                :src="getAvatarUrl(scope.row.driverAvatar)" 
                :alt="scope.row.driverName"
                class="driver-avatar"
                @error="handleAvatarError"
              />
              <div class="driver-details">
                <div class="driver-name">{{ scope.row.driverName }}</div>
                <div class="driver-phone">{{ scope.row.driverPhone }}</div>
              </div>
            </div>
            <span v-else class="no-driver">未分配司机</span>
          </template>
        </el-table-column>
        
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusTagType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column label="注册时间" width="180">
          <template #default="scope">
            {{ formatTime(scope.row.createdAt) }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button
              type="primary"
              size="small"
              @click="viewVehicleDetail(scope.row)"
            >
              查看详情
            </el-button>
            <el-button
              v-if="scope.row.status === 'PENDING'"
              type="success"
              size="small"
              @click="approveVehicle(scope.row)"
            >
              审核通过
            </el-button>
            <el-button
              v-if="scope.row.status === 'PENDING'"
              type="danger"
              size="small"
              @click="rejectVehicle(scope.row)"
            >
              拒绝
            </el-button>
            <el-button
              v-if="scope.row.status === 'ACTIVE'"
              type="warning"
              size="small"
              @click="deactivateVehicle(scope.row)"
            >
              停用
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.currentPage"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 车辆详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="车辆详情"
      width="800px"
    >
      <div v-if="selectedVehicle" class="vehicle-detail">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="车辆ID">{{ selectedVehicle.id }}</el-descriptions-item>
              <el-descriptions-item label="车牌号">{{ selectedVehicle.plateNumber }}</el-descriptions-item>
              <el-descriptions-item label="品牌">{{ selectedVehicle.brand }}</el-descriptions-item>
              <el-descriptions-item label="型号">{{ selectedVehicle.model }}</el-descriptions-item>
              <el-descriptions-item label="颜色">{{ selectedVehicle.color }}</el-descriptions-item>
              <el-descriptions-item label="车辆类型">
                <el-tag :type="getVehicleTypeTagType(selectedVehicle.vehicleType)">
                  {{ getVehicleTypeText(selectedVehicle.vehicleType) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="getStatusTagType(selectedVehicle.status)">
                  {{ getStatusText(selectedVehicle.status) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="注册时间">{{ formatTime(selectedVehicle.createdAt) }}</el-descriptions-item>
            </el-descriptions>
          </el-col>
          <el-col :span="12">
            <div class="vehicle-images">
              <h4>车辆图片</h4>
              <el-image
                :src="getVehicleImageUrl(selectedVehicle.vehicleImage)"
                :preview-src-list="[getVehicleImageUrl(selectedVehicle.vehicleImage)]"
                fit="cover"
                style="width: 100%; height: 200px; border-radius: 8px;"
              >
                <template #error>
                  <div class="image-slot">
                    <el-icon><Picture /></el-icon>
                    <div>暂无图片</div>
                  </div>
                </template>
              </el-image>
            </div>
          </el-col>
        </el-row>
        
        <div v-if="selectedVehicle.driverName" class="driver-section">
          <h4>司机信息</h4>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="司机姓名">{{ selectedVehicle.driverName }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ selectedVehicle.driverPhone }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button 
          v-if="selectedVehicle && selectedVehicle.status === 'PENDING'"
          type="success" 
          @click="approveVehicle(selectedVehicle)"
        >
          审核通过
        </el-button>
        <el-button 
          v-if="selectedVehicle && selectedVehicle.status === 'PENDING'"
          type="danger" 
          @click="rejectVehicle(selectedVehicle)"
        >
          拒绝
        </el-button>
      </template>
    </el-dialog>

    <!-- 添加车辆对话框 -->
    <el-dialog
      v-model="addDialogVisible"
      title="添加车辆"
      width="600px"
    >
      <el-form :model="vehicleForm" :rules="vehicleRules" ref="vehicleFormRef" label-width="100px">
        <el-form-item label="车牌号" prop="plateNumber">
          <el-input v-model="vehicleForm.plateNumber" placeholder="请输入车牌号" />
        </el-form-item>
        <el-form-item label="品牌" prop="brand">
          <el-input v-model="vehicleForm.brand" placeholder="请输入车辆品牌" />
        </el-form-item>
        <el-form-item label="型号" prop="model">
          <el-input v-model="vehicleForm.model" placeholder="请输入车辆型号" />
        </el-form-item>
        <el-form-item label="颜色" prop="color">
          <el-input v-model="vehicleForm.color" placeholder="请输入车辆颜色" />
        </el-form-item>
        <el-form-item label="车辆类型" prop="vehicleType">
          <el-select v-model="vehicleForm.vehicleType" placeholder="请选择车辆类型">
            <el-option label="轿车" value="SEDAN" />
            <el-option label="SUV" value="SUV" />
            <el-option label="MPV" value="MPV" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="司机ID" prop="driverId">
          <el-input v-model="vehicleForm.driverId" type="number" placeholder="请输入司机ID" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitVehicle" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// 响应式数据
const loading = ref(false)
const submitting = ref(false)
const vehicles = ref([])
const selectedVehicles = ref([])
const detailDialogVisible = ref(false)
const addDialogVisible = ref(false)
const selectedVehicle = ref(null)
const vehicleFormRef = ref(null)

// 统计数据
const stats = reactive({
  totalVehicles: 0,
  activeVehicles: 0,
  pendingVehicles: 0,
  rejectedVehicles: 0
})

// 筛选条件
const filters = reactive({
  status: null,
  type: null,
  driverName: '',
  plateNumber: ''
})

// 分页
const pagination = reactive({
  currentPage: 1,
  pageSize: 20,
  total: 0
})

// 车辆表单
const vehicleForm = reactive({
  plateNumber: '',
  brand: '',
  model: '',
  color: '',
  vehicleType: '',
  driverId: ''
})

// 表单验证规则
const vehicleRules = {
  plateNumber: [
    { required: true, message: '请输入车牌号', trigger: 'blur' }
  ],
  brand: [
    { required: true, message: '请输入车辆品牌', trigger: 'blur' }
  ],
  model: [
    { required: true, message: '请输入车辆型号', trigger: 'blur' }
  ],
  color: [
    { required: true, message: '请输入车辆颜色', trigger: 'blur' }
  ],
  vehicleType: [
    { required: true, message: '请选择车辆类型', trigger: 'change' }
  ],
  driverId: [
    { required: true, message: '请输入司机ID', trigger: 'blur' }
  ]
}

// 方法
const loadVehicles = async () => {
  loading.value = true
  try {
    console.log('正在加载车辆数据...')
    
    const response = await fetch('/api/vehicles/all')
    console.log('车辆API响应状态:', response.status)
    
    if (response.ok) {
      const data = await response.json()
      console.log('车辆数据:', data)
      
      const isSuccess = data.success === true || data.code === 200
      if (isSuccess && data.data) {
        const vehiclesData = data.data
        
        // 处理车辆数据
        vehicles.value = vehiclesData.map(vehicle => ({
          ...vehicle,
          driverName: vehicle.driverName || '未分配',
          driverPhone: vehicle.driverPhone || '',
          driverAvatar: vehicle.driverAvatar || null
        }))
        
        // 更新统计数据
        updateStats(vehiclesData)
        
        pagination.total = vehiclesData.length
      } else {
        console.warn('车辆数据格式不正确:', data)
        vehicles.value = []
      }
    } else {
      console.warn('车辆API请求失败:', response.status)
      vehicles.value = []
    }
  } catch (error) {
    console.error('加载车辆数据失败:', error)
    ElMessage.error(`加载车辆数据失败: ${error.message}`)
    vehicles.value = []
  } finally {
    loading.value = false
  }
}

const updateStats = (vehiclesData) => {
  stats.totalVehicles = vehiclesData.length
  stats.activeVehicles = vehiclesData.filter(v => v.status === 'ACTIVE').length
  stats.pendingVehicles = vehiclesData.filter(v => v.status === 'PENDING').length
  stats.rejectedVehicles = vehiclesData.filter(v => v.status === 'REJECTED').length
}

const searchVehicles = async () => {
  loading.value = true
  try {
    console.log('正在搜索车辆数据...', filters)
    
    const response = await fetch('/api/vehicles/all')
    if (response.ok) {
      const data = await response.json()
      const isSuccess = data.success === true || data.code === 200
      
      if (isSuccess && data.data) {
        let filteredData = data.data
        
        // 按状态筛选
        if (filters.status) {
          filteredData = filteredData.filter(vehicle => vehicle.status === filters.status)
        }
        
        // 按车辆类型筛选
        if (filters.type) {
          filteredData = filteredData.filter(vehicle => vehicle.vehicleType === filters.type)
        }
        
        // 按司机姓名搜索
        if (filters.driverName && filters.driverName.trim()) {
          const keyword = filters.driverName.trim().toLowerCase()
          filteredData = filteredData.filter(vehicle => 
            vehicle.driverName && vehicle.driverName.toLowerCase().includes(keyword)
          )
        }
        
        // 按车牌号搜索
        if (filters.plateNumber && filters.plateNumber.trim()) {
          const keyword = filters.plateNumber.trim().toLowerCase()
          filteredData = filteredData.filter(vehicle => 
            vehicle.plateNumber && vehicle.plateNumber.toLowerCase().includes(keyword)
          )
        }
        
        // 处理筛选后的数据
        vehicles.value = filteredData.map(vehicle => ({
          ...vehicle,
          driverName: vehicle.driverName || '未分配',
          driverPhone: vehicle.driverPhone || '',
          driverAvatar: vehicle.driverAvatar || null
        }))
        
        // 更新统计数据
        updateStats(filteredData)
        pagination.total = filteredData.length
        
        ElMessage.success(`找到 ${filteredData.length} 辆符合条件的车辆`)
      }
    }
  } catch (error) {
    console.error('搜索车辆数据失败:', error)
    ElMessage.error(`搜索失败: ${error.message}`)
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  filters.status = null
  filters.type = null
  filters.driverName = ''
  filters.plateNumber = ''
  loadVehicles()
}

const refreshData = () => {
  loadVehicles()
  ElMessage.success('数据已刷新')
}

const handleSelectionChange = (selection) => {
  selectedVehicles.value = selection
}

const viewVehicleDetail = (vehicle) => {
  selectedVehicle.value = vehicle
  detailDialogVisible.value = true
}

const showAddDialog = () => {
  // 重置表单
  Object.assign(vehicleForm, {
    plateNumber: '',
    brand: '',
    model: '',
    color: '',
    vehicleType: '',
    driverId: ''
  })
  addDialogVisible.value = true
}

const submitVehicle = async () => {
  try {
    await vehicleFormRef.value.validate()
    
    submitting.value = true
    
    const response = await fetch('/api/vehicles', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(vehicleForm)
    })
    
    const result = await response.json()
    
    if (result.success || result.code === 200) {
      ElMessage.success('车辆添加成功')
      addDialogVisible.value = false
      loadVehicles()
    } else {
      ElMessage.error(result.message || '添加车辆失败')
    }
  } catch (error) {
    if (error.message) {
      ElMessage.error('请填写完整的车辆信息')
    } else {
      ElMessage.error('添加车辆失败: ' + error.message)
    }
  } finally {
    submitting.value = false
  }
}

const approveVehicle = async (vehicle) => {
  try {
    await ElMessageBox.confirm(
      `确定要审核通过车辆 ${vehicle.plateNumber} 吗？`,
      '确认审核',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'success',
      }
    )
    
    const response = await fetch(`/api/vehicles/${vehicle.id}/approve`, {
      method: 'POST'
    })
    
    const result = await response.json()
    
    if (result.success || result.code === 200) {
      ElMessage.success('车辆审核通过')
      detailDialogVisible.value = false
      loadVehicles()
    } else {
      ElMessage.error(result.message || '审核失败')
    }
  } catch (error) {
    if (error.message) {
      ElMessage.error('审核失败: ' + error.message)
    }
    // 用户取消时不显示错误
  }
}

const rejectVehicle = async (vehicle) => {
  try {
    await ElMessageBox.confirm(
      `确定要拒绝车辆 ${vehicle.plateNumber} 吗？`,
      '确认拒绝',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    const response = await fetch(`/api/vehicles/${vehicle.id}/reject`, {
      method: 'POST'
    })
    
    const result = await response.json()
    
    if (result.success || result.code === 200) {
      ElMessage.success('车辆已拒绝')
      detailDialogVisible.value = false
      loadVehicles()
    } else {
      ElMessage.error(result.message || '拒绝失败')
    }
  } catch (error) {
    if (error.message) {
      ElMessage.error('拒绝失败: ' + error.message)
    }
    // 用户取消时不显示错误
  }
}

const deactivateVehicle = async (vehicle) => {
  try {
    await ElMessageBox.confirm(
      `确定要停用车辆 ${vehicle.plateNumber} 吗？`,
      '确认停用',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    const response = await fetch(`/api/vehicles/${vehicle.id}/deactivate`, {
      method: 'POST'
    })
    
    const result = await response.json()
    
    if (result.success || result.code === 200) {
      ElMessage.success('车辆已停用')
      loadVehicles()
    } else {
      ElMessage.error(result.message || '停用失败')
    }
  } catch (error) {
    if (error.message) {
      ElMessage.error('停用失败: ' + error.message)
    }
    // 用户取消时不显示错误
  }
}

const handleSizeChange = (size) => {
  pagination.pageSize = size
  loadVehicles()
}

const handleCurrentChange = (page) => {
  pagination.currentPage = page
  loadVehicles()
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString()
}

const getVehicleImageUrl = (image) => {
  if (!image || image === 'null' || image === 'undefined') {
    return 'https://via.placeholder.com/300x200?text=No+Image'
  }
  if (image.startsWith('http')) return image
  if (image.startsWith('/uploads/')) {
    return `http://localhost:8080${image}`
  }
  return `http://localhost:8080/uploads/vehicles/${image}`
}

const getAvatarUrl = (avatar) => {
  if (!avatar || avatar === 'null' || avatar === 'undefined') {
    return 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
  }
  if (avatar.startsWith('http')) return avatar
  if (avatar.startsWith('/uploads/')) {
    return `http://localhost:8080${avatar}`
  }
  return `http://localhost:8080/uploads/avatars/${avatar}`
}

const handleImageError = (event) => {
  event.target.src = 'https://via.placeholder.com/300x200?text=No+Image'
}

const handleAvatarError = (event) => {
  event.target.src = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
}

const getVehicleTypeText = (type) => {
  const typeMap = {
    'SEDAN': '轿车',
    'SUV': 'SUV',
    'MPV': 'MPV',
    'OTHER': '其他'
  }
  return typeMap[type] || type
}

const getVehicleTypeTagType = (type) => {
  const typeMap = {
    'SEDAN': 'primary',
    'SUV': 'success',
    'MPV': 'warning',
    'OTHER': 'info'
  }
  return typeMap[type] || 'default'
}

const getStatusText = (status) => {
  const statusMap = {
    'ACTIVE': '活跃',
    'PENDING': '待审核',
    'REJECTED': '已拒绝',
    'INACTIVE': '已停用'
  }
  return statusMap[status] || status
}

const getStatusTagType = (status) => {
  const typeMap = {
    'ACTIVE': 'success',
    'PENDING': 'warning',
    'REJECTED': 'danger',
    'INACTIVE': 'info'
  }
  return typeMap[status] || 'default'
}

onMounted(() => {
  loadVehicles()
})
</script>

<style scoped>
.vehicle-management {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  color: #333;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.stats-row {
  margin-bottom: 20px;
}

.stats-card {
  height: 100px;
}

.stats-content {
  display: flex;
  align-items: center;
  height: 100%;
}

.stats-icon {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
  font-size: 20px;
  color: white;
}

.stats-icon.total {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stats-icon.active {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.stats-icon.pending {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stats-icon.rejected {
  background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%);
}

.stats-info {
  flex: 1;
}

.stats-number {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin-bottom: 5px;
}

.stats-label {
  font-size: 14px;
  color: #666;
}

.filter-card {
  margin-bottom: 20px;
}

.table-card {
  margin-bottom: 20px;
}

.driver-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.driver-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.driver-details {
  flex: 1;
}

.driver-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.driver-phone {
  font-size: 12px;
  color: #666;
}

.no-driver {
  color: #999;
  font-style: italic;
}

.image-slot {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
  font-size: 12px;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.vehicle-detail {
  padding: 20px 0;
}

.vehicle-images {
  margin-bottom: 20px;
}

.vehicle-images h4 {
  margin-bottom: 10px;
  color: #333;
}

.driver-section {
  margin-top: 20px;
}

.driver-section h4 {
  margin-bottom: 10px;
  color: #333;
}
</style>