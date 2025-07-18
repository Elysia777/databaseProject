<template>
  <div class="orders">
    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="订单号">
          <el-input v-model="searchForm.orderNumber" placeholder="请输入订单号" clearable />
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="待派单" value="PENDING" />
            <el-option label="已派单" value="ASSIGNED" />
            <el-option label="已上车" value="PICKUP" />
            <el-option label="进行中" value="IN_PROGRESS" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="订单类型">
          <el-select v-model="searchForm.orderType" placeholder="请选择类型" clearable>
            <el-option label="实时单" value="REAL_TIME" />
            <el-option label="预约单" value="RESERVATION" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 订单列表 -->
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>{{ getPageTitle() }}</span>
          <el-button v-if="userStore.isAdmin" type="primary" @click="handleCreateOrder">创建订单</el-button>
          <el-button v-if="userStore.isPassenger" type="primary" @click="handleCreateOrder">叫车</el-button>
        </div>
      </template>

      <el-table :data="orderList" style="width: 100%" v-loading="loading">
        <el-table-column prop="orderNumber" label="订单号" width="180" />
        <el-table-column prop="orderType" label="订单类型" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.orderType === 'REAL_TIME' ? 'primary' : 'success'">
              {{ scope.row.orderType === 'REAL_TIME' ? '实时单' : '预约单' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="passengerName" label="乘客" width="120" />
        <el-table-column prop="driverName" label="司机" width="120" />
        <el-table-column prop="pickupAddress" label="上车地址" />
        <el-table-column prop="destinationAddress" label="目的地" />
        <el-table-column prop="totalFare" label="金额" width="100">
          <template #default="scope">
            ¥{{ scope.row.totalFare }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="handleViewOrder(scope.row)">查看</el-button>
            <el-button 
              v-if="scope.row.status === 'PENDING'" 
              size="small" 
              type="primary" 
              @click="handleDispatchOrder(scope.row)"
            >
              派单
            </el-button>
            <el-button 
              v-if="scope.row.status === 'PENDING'" 
              size="small" 
              type="danger" 
              @click="handleCancelOrder(scope.row)"
            >
              取消
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

    <!-- 创建订单对话框 -->
    <el-dialog v-model="createDialogVisible" title="创建订单" width="600px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="订单类型" prop="orderType">
          <el-select v-model="createForm.orderType" placeholder="请选择订单类型" style="width: 100%">
            <el-option label="实时单" value="REAL_TIME" />
            <el-option label="预约单" value="RESERVATION" />
          </el-select>
        </el-form-item>
        <el-form-item label="上车地址" prop="pickupAddress">
          <MapSearch 
            v-model="createForm.pickupAddress" 
            placeholder="请输入上车地址"
            @select="handlePickupSelect"
          />
        </el-form-item>
        <el-form-item label="目的地" prop="destinationAddress">
          <MapSearch 
            v-model="createForm.destinationAddress" 
            placeholder="请输入目的地"
            @select="handleDestinationSelect"
          />
        </el-form-item>
        <el-form-item label="预约时间" prop="scheduledTime" v-if="createForm.orderType === 'RESERVATION'">
          <el-date-picker
            v-model="createForm.scheduledTime"
            type="datetime"
            placeholder="请选择预约时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="支付方式" prop="paymentMethod">
          <el-select v-model="createForm.paymentMethod" placeholder="请选择支付方式" style="width: 100%">
            <el-option label="微信支付" value="WECHAT" />
            <el-option label="支付宝" value="ALIPAY" />
            <el-option label="现金" value="CASH" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="createDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleCreateOrderSubmit" :loading="createLoading">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import dayjs from 'dayjs'
import MapSearch from '@/components/MapSearch.vue'

const userStore = useUserStore()

// 搜索表单
const searchForm = reactive({
  orderNumber: '',
  status: '',
  orderType: ''
})

// 分页信息
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

// 订单列表
const orderList = ref([])
const loading = ref(false)

// 创建订单相关
const createDialogVisible = ref(false)
const createLoading = ref(false)
const createFormRef = ref()
const createForm = reactive({
  orderType: '',
  pickupAddress: '',
  destinationAddress: '',
  scheduledTime: '',
  paymentMethod: '',
  pickupLatitude: null,
  pickupLongitude: null,
  destinationLatitude: null,
  destinationLongitude: null
})

const createRules = {
  orderType: [{ required: true, message: '请选择订单类型', trigger: 'change' }],
  pickupAddress: [{ required: true, message: '请输入上车地址', trigger: 'blur' }],
  destinationAddress: [{ required: true, message: '请输入目的地', trigger: 'blur' }],
  scheduledTime: [{ required: true, message: '请选择预约时间', trigger: 'change' }],
  paymentMethod: [{ required: true, message: '请选择支付方式', trigger: 'change' }]
}

// 获取状态类型
const getStatusType = (status) => {
  const statusMap = {
    'PENDING': 'warning',
    'ASSIGNED': 'primary',
    'PICKUP': 'info',
    'IN_PROGRESS': 'success',
    'COMPLETED': 'success',
    'CANCELLED': 'danger'
  }
  return statusMap[status] || 'info'
}

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    'PENDING': '待派单',
    'ASSIGNED': '已派单',
    'PICKUP': '已上车',
    'IN_PROGRESS': '进行中',
    'COMPLETED': '已完成',
    'CANCELLED': '已取消'
  }
  return statusMap[status] || status
}

// 格式化日期
const formatDate = (date) => {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

// 加载订单列表
const loadOrderList = async () => {
  loading.value = true
  try {
    // 这里应该调用实际的API
    // 目前使用模拟数据
    orderList.value = [
      {
        id: 1,
        orderNumber: 'TX20241201001',
        orderType: 'REAL_TIME',
        passengerName: '张三',
        driverName: '李师傅',
        pickupAddress: '北京市朝阳区三里屯',
        destinationAddress: '北京市海淀区中关村',
        totalFare: 45.5,
        status: 'COMPLETED',
        createdAt: '2024-12-01 10:30:00'
      },
      {
        id: 2,
        orderNumber: 'TX20241201002',
        orderType: 'RESERVATION',
        passengerName: '李四',
        driverName: '王师傅',
        pickupAddress: '北京市海淀区中关村',
        destinationAddress: '北京市西城区西单',
        totalFare: 32.0,
        status: 'IN_PROGRESS',
        createdAt: '2024-12-01 11:15:00'
      }
    ]
    pagination.total = 100
  } catch (error) {
    ElMessage.error('加载订单列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  loadOrderList()
}

// 重置搜索
const handleReset = () => {
  Object.assign(searchForm, {
    orderNumber: '',
    status: '',
    orderType: ''
  })
  handleSearch()
}

// 分页处理
const handleSizeChange = (size) => {
  pagination.size = size
  loadOrderList()
}

const handleCurrentChange = (current) => {
  pagination.current = current
  loadOrderList()
}

// 查看订单
const handleViewOrder = (order) => {
  ElMessage.info(`查看订单: ${order.orderNumber}`)
}

// 派单
const handleDispatchOrder = async (order) => {
  try {
    await ElMessageBox.confirm(`确定要为订单 ${order.orderNumber} 派单吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    ElMessage.success('派单成功')
    loadOrderList()
  } catch (error) {
    // 用户取消
  }
}

// 取消订单
const handleCancelOrder = async (order) => {
  try {
    await ElMessageBox.confirm(`确定要取消订单 ${order.orderNumber} 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    ElMessage.success('订单已取消')
    loadOrderList()
  } catch (error) {
    // 用户取消
  }
}

// 创建订单
const handleCreateOrder = () => {
  createDialogVisible.value = true
  Object.assign(createForm, {
    orderType: '',
    pickupAddress: '',
    destinationAddress: '',
    scheduledTime: '',
    paymentMethod: ''
  })
}

// 处理上车地址选择
const handlePickupSelect = (place) => {
  if (place.location) {
    const [lng, lat] = place.location.split(',').map(Number)
    createForm.pickupLongitude = lng
    createForm.pickupLatitude = lat
  }
}

// 处理目的地选择
const handleDestinationSelect = (place) => {
  if (place.location) {
    const [lng, lat] = place.location.split(',').map(Number)
    createForm.destinationLongitude = lng
    createForm.destinationLatitude = lat
  }
}

// 提交创建订单
const handleCreateOrderSubmit = async () => {
  try {
    await createFormRef.value.validate()
    createLoading.value = true
    
    // 这里应该调用实际的API
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    ElMessage.success('订单创建成功')
    createDialogVisible.value = false
    loadOrderList()
  } catch (error) {
    ElMessage.error('创建订单失败')
  } finally {
    createLoading.value = false
  }
}

// 获取页面标题
const getPageTitle = () => {
  if (userStore.isPassenger) {
    return '我的订单'
  } else if (userStore.isDriver) {
    return '我的订单'
  }
  return '订单列表'
}

onMounted(() => {
  loadOrderList()
})
</script>

<style scoped>
.orders {
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

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style> 