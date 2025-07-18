<template>
  <div class="drivers">
    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="司机姓名">
          <el-input v-model="searchForm.realName" placeholder="请输入司机姓名" clearable />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="searchForm.phone" placeholder="请输入手机号" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="在线" value="ONLINE" />
            <el-option label="离线" value="OFFLINE" />
            <el-option label="禁用" value="BANNED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 司机列表 -->
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>{{ getPageTitle() }}</span>
          <el-button v-if="userStore.isAdmin" type="primary" @click="handleAddDriver">添加司机</el-button>
        </div>
      </template>

      <el-table :data="driverList" style="width: 100%" v-loading="loading">
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="phone" label="手机号" width="150" />
        <el-table-column prop="driverLicense" label="驾驶证号" width="180" />
        <el-table-column prop="rating" label="评分" width="100">
          <template #default="scope">
            <el-rate v-model="scope.row.rating" disabled show-score />
          </template>
        </el-table-column>
        <el-table-column prop="totalOrders" label="总订单" width="100" />
        <el-table-column prop="completedOrders" label="完成订单" width="100" />
        <el-table-column prop="completionRate" label="完成率" width="100">
          <template #default="scope">
            {{ scope.row.completionRate }}%
          </template>
        </el-table-column>
        <el-table-column prop="isOnline" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.isOnline ? 'success' : 'info'">
              {{ scope.row.isOnline ? '在线' : '离线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="handleViewDriver(scope.row)">查看</el-button>
            <el-button size="small" type="primary" @click="handleEditDriver(scope.row)">编辑</el-button>
            <el-button 
              size="small" 
              :type="scope.row.isOnline ? 'warning' : 'success'"
              @click="handleToggleStatus(scope.row)"
            >
              {{ scope.row.isOnline ? '下线' : '上线' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 司机详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="司机详情" width="800px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="姓名">{{ currentDriver.realName }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentDriver.phone }}</el-descriptions-item>
        <el-descriptions-item label="驾驶证号">{{ currentDriver.driverLicense }}</el-descriptions-item>
        <el-descriptions-item label="从业资格证">{{ currentDriver.professionalLicense }}</el-descriptions-item>
        <el-descriptions-item label="驾龄">{{ currentDriver.drivingYears }}年</el-descriptions-item>
        <el-descriptions-item label="总里程">{{ currentDriver.totalMileage }}公里</el-descriptions-item>
        <el-descriptions-item label="评分">
          <el-rate v-model="currentDriver.rating" disabled show-score />
        </el-descriptions-item>
        <el-descriptions-item label="总订单数">{{ currentDriver.totalOrders }}</el-descriptions-item>
        <el-descriptions-item label="完成订单数">{{ currentDriver.completedOrders }}</el-descriptions-item>
        <el-descriptions-item label="取消订单数">{{ currentDriver.cancelledOrders }}</el-descriptions-item>
        <el-descriptions-item label="完成率">{{ currentDriver.completionRate }}%</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentDriver.isOnline ? 'success' : 'info'">
            {{ currentDriver.isOnline ? '在线' : '离线' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ formatDate(currentDriver.createdAt) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import dayjs from 'dayjs'

const userStore = useUserStore()

// 搜索表单
const searchForm = reactive({
  realName: '',
  phone: '',
  status: ''
})

// 分页信息
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

// 司机列表
const driverList = ref([])
const loading = ref(false)

// 司机详情对话框
const detailDialogVisible = ref(false)
const currentDriver = ref({})

// 格式化日期
const formatDate = (date) => {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

// 加载司机列表
const loadDriverList = async () => {
  loading.value = true
  try {
    // 这里应该调用实际的API
    // 目前使用模拟数据
    driverList.value = [
      {
        id: 1,
        realName: '李师傅',
        phone: '13800138001',
        driverLicense: '110101199001011234',
        professionalLicense: 'BJ2024001',
        drivingYears: 5,
        totalMileage: 50000,
        rating: 4.8,
        totalOrders: 1250,
        completedOrders: 1200,
        cancelledOrders: 50,
        completionRate: 96,
        isOnline: true,
        createdAt: '2024-01-01 10:00:00'
      },
      {
        id: 2,
        realName: '王师傅',
        phone: '13800138002',
        driverLicense: '110101199002022345',
        professionalLicense: 'BJ2024002',
        drivingYears: 3,
        totalMileage: 30000,
        rating: 4.6,
        totalOrders: 800,
        completedOrders: 750,
        cancelledOrders: 50,
        completionRate: 93.75,
        isOnline: false,
        createdAt: '2024-02-01 10:00:00'
      }
    ]
    pagination.total = 50
  } catch (error) {
    ElMessage.error('加载司机列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  loadDriverList()
}

// 重置搜索
const handleReset = () => {
  Object.assign(searchForm, {
    realName: '',
    phone: '',
    status: ''
  })
  handleSearch()
}

// 分页处理
const handleSizeChange = (size) => {
  pagination.size = size
  loadDriverList()
}

const handleCurrentChange = (current) => {
  pagination.current = current
  loadDriverList()
}

// 查看司机详情
const handleViewDriver = (driver) => {
  currentDriver.value = driver
  detailDialogVisible.value = true
}

// 编辑司机
const handleEditDriver = (driver) => {
  ElMessage.info(`编辑司机: ${driver.realName}`)
}

// 切换司机状态
const handleToggleStatus = async (driver) => {
  try {
    const action = driver.isOnline ? '下线' : '上线'
    await ElMessageBox.confirm(`确定要让司机 ${driver.realName} ${action}吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    // 这里应该调用实际的API
    driver.isOnline = !driver.isOnline
    ElMessage.success(`${action}成功`)
  } catch (error) {
    // 用户取消
  }
}

// 添加司机
const handleAddDriver = () => {
  ElMessage.info('添加司机功能')
}

// 获取页面标题
const getPageTitle = () => {
  if (userStore.isDriver) {
    return '收入统计'
  }
  return '司机列表'
}

onMounted(() => {
  loadDriverList()
})
</script>

<style scoped>
.drivers {
  padding: 20px;
}

.search-card {
  margin-bottom: 20px;
}

.table-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination {
  margin-top: 20px;
  text-align: right;
}
</style> 