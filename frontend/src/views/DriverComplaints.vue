<template>
  <div class="driver-complaints">
    <div class="page-header">
      <h2>我的投诉</h2>
      <div class="header-actions">
        <el-button @click="showCreateDialog = true" type="primary">
          <el-icon><Plus /></el-icon>
          投诉乘客
        </el-button>
        <el-button @click="loadComplaints" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 投诉列表 -->
    <div class="complaints-container">
      <el-card v-if="loading" class="loading-card">
        <div class="loading-content">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>加载中...</span>
        </div>
      </el-card>

      <div v-else-if="complaints.length === 0" class="empty-state">
        <el-empty description="暂无投诉记录" />
      </div>

      <div v-else class="complaints-list">
        <el-card v-for="complaint in complaints" :key="complaint.id" class="complaint-card" @click="showComplaintDetail(complaint)">
          <div class="complaint-header">
            <div class="complaint-info">
              <span class="complaint-title">{{ complaint.title }}</span>
              <el-tag :type="getStatusTagType(complaint.status)" size="small">
                {{ getStatusText(complaint.status) }}
              </el-tag>
              <el-tag :type="getTypeTagType(complaint.complaintType)" size="small">
                {{ getTypeText(complaint.complaintType) }}
              </el-tag>
            </div>
            <div class="complaint-time">
              {{ formatDateTime(complaint.createdAt) }}
            </div>
          </div>

          <div class="complaint-content">
            <div class="complaint-description">
              {{ complaint.description }}
            </div>
            <div v-if="complaint.resolution" class="complaint-resolution">
              <div class="resolution-label">处理结果：</div>
              <div class="resolution-content">{{ complaint.resolution }}</div>
            </div>
          </div>
        </el-card>
      </div>

      <div v-if="complaints.length > 0" class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="totalComplaints"
          layout="prev, pager, next, jumper"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 创建投诉对话框 -->
    <el-dialog v-model="showCreateDialog" title="投诉乘客" width="600px">
      <el-form :model="complaintForm" :rules="complaintRules" ref="complaintFormRef" label-width="100px">
        <el-form-item label="关联订单" prop="orderId">
          <el-select v-model="complaintForm.orderId" placeholder="选择要投诉的订单" style="width: 100%;" @change="handleOrderChange">
            <el-option 
              v-for="order in availableOrders" 
              :key="order.id" 
              :label="`${order.orderNumber} - ${order.pickupAddress}`" 
              :value="order.id">
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="投诉类型" prop="complaintType">
          <el-select v-model="complaintForm.complaintType" placeholder="选择投诉类型" style="width: 100%;">
            <el-option label="乘客行为不当" value="BEHAVIOR" />
            <el-option label="安全问题" value="SAFETY" />
            <el-option label="费用纠纷" value="PAYMENT" />
            <el-option label="服务态度" value="SERVICE" />
            <el-option label="其他问题" value="OTHER" />
          </el-select>
        </el-form-item>

        <el-form-item label="投诉标题" prop="title">
          <el-input v-model="complaintForm.title" placeholder="请输入投诉标题" />
        </el-form-item>

        <el-form-item label="投诉描述" prop="description">
          <el-input 
            v-model="complaintForm.description" 
            type="textarea" 
            :rows="4" 
            placeholder="请详细描述投诉内容" />
        </el-form-item>

        <el-form-item label="证据文件">
          <el-upload
            v-model:file-list="evidenceFiles"
            action="/api/upload"
            list-type="picture-card"
            :on-preview="handlePreview"
            :on-remove="handleRemove"
            :before-upload="beforeUpload">
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showCreateDialog = false">取消</el-button>
          <el-button type="primary" @click="submitComplaint" :loading="submitting">提交投诉</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 投诉详情对话框 -->
    <el-dialog v-model="showDetailDialog" title="投诉详情" width="700px">
      <div v-if="selectedComplaint" class="complaint-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="投诉标题" :span="2">{{ selectedComplaint.title }}</el-descriptions-item>
          <el-descriptions-item label="投诉类型">
            <el-tag :type="getTypeTagType(selectedComplaint.complaintType)">
              {{ getTypeText(selectedComplaint.complaintType) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="处理状态">
            <el-tag :type="getStatusTagType(selectedComplaint.status)">
              {{ getStatusText(selectedComplaint.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ formatDateTime(selectedComplaint.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="处理时间" v-if="selectedComplaint.resolutionTime">
            {{ formatDateTime(selectedComplaint.resolutionTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="投诉描述" :span="2">
            <div class="description-content">{{ selectedComplaint.description }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="处理结果" :span="2" v-if="selectedComplaint.resolution">
            <div class="resolution-content">{{ selectedComplaint.resolution }}</div>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Refresh, Loading } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// 响应式数据
const complaints = ref([])
const availableOrders = ref([])
const loading = ref(false)
const submitting = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const totalComplaints = ref(0)

// 对话框状态
const showCreateDialog = ref(false)
const showDetailDialog = ref(false)
const selectedComplaint = ref(null)

// 表单数据
const complaintForm = ref({
  orderId: null,
  defendantId: null, // 这个字段会由后端根据订单信息自动推断
  complaintType: '',
  title: '',
  description: ''
})

const evidenceFiles = ref([])

// 表单验证规则
const complaintRules = {
  orderId: [
    { required: true, message: '请选择关联订单', trigger: 'change' }
  ],
  complaintType: [
    { required: true, message: '请选择投诉类型', trigger: 'change' }
  ],
  title: [
    { required: true, message: '请输入投诉标题', trigger: 'blur' },
    { min: 5, max: 100, message: '标题长度在 5 到 100 个字符', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请输入投诉描述', trigger: 'blur' },
    { min: 10, max: 1000, message: '描述长度在 10 到 1000 个字符', trigger: 'blur' }
  ]
}

const complaintFormRef = ref()

// 计算属性
const userId = computed(() => userStore.user?.id)

// 加载投诉列表
const loadComplaints = async () => {
  if (!userId.value) {
    ElMessage.error('请先登录')
    return
  }

  loading.value = true
  try {
    const response = await fetch(`/api/complaints/user/${userId.value}?page=${currentPage.value}&size=${pageSize.value}`, {
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })

    const result = await response.json()
    
    if (result.code === 200) {
      complaints.value = result.data || []
      totalComplaints.value = complaints.value.length
    } else {
      ElMessage.error(result.message || '加载投诉列表失败')
    }
  } catch (error) {
    console.error('加载投诉列表失败:', error)
    ElMessage.error('网络错误，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 加载可投诉的订单（司机的已完成订单）
const loadAvailableOrders = async () => {
  if (!userId.value) return

  try {
    // 使用通用API通过用户ID获取订单
    const response = await fetch(`/api/orders/user/${userId.value}/orders`, {
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })

    const result = await response.json()
    
    if (result.code === 200) {
      // 只显示已完成且已支付的订单（可以投诉）
      availableOrders.value = (result.data || []).filter(order => 
        order.status === 'COMPLETED' && order.paymentStatus === 'PAID'
      )
      console.log('加载到可投诉订单:', availableOrders.value.length, '个')
    } else {
      console.log('获取订单失败:', result.message)
    }
  } catch (error) {
    console.error('加载订单列表失败:', error)
  }
}

// 处理订单选择
const handleOrderChange = (orderId) => {
  const selectedOrder = availableOrders.value.find(order => order.id === orderId)
  if (selectedOrder) {
    // 不需要在前端设置defendantId，让后端从订单中推断
    // 后端会根据投诉人身份和订单信息自动确定被投诉人的用户ID
    complaintForm.value.defendantId = null
    console.log('选择订单:', selectedOrder.orderNumber, '乘客ID:', selectedOrder.passengerId)
  }
}

// 提交投诉
const submitComplaint = async () => {
  if (!complaintFormRef.value) return

  try {
    await complaintFormRef.value.validate()
    
    submitting.value = true

    const requestData = {
      ...complaintForm.value,
      evidenceFiles: evidenceFiles.value.map(file => file.url).filter(Boolean)
    }

    const response = await fetch(`/api/complaints/create?complainantId=${userId.value}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${userStore.token}`
      },
      body: JSON.stringify(requestData)
    })

    const result = await response.json()
    
    if (result.code === 200) {
      ElMessage.success('投诉提交成功')
      showCreateDialog.value = false
      resetForm()
      loadComplaints()
    } else {
      ElMessage.error(result.message || '提交投诉失败')
    }
  } catch (error) {
    console.error('提交投诉失败:', error)
    ElMessage.error('提交失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

// 重置表单
const resetForm = () => {
  complaintForm.value = {
    orderId: null,
    defendantId: null,
    complaintType: '',
    title: '',
    description: ''
  }
  evidenceFiles.value = []
  if (complaintFormRef.value) {
    complaintFormRef.value.resetFields()
  }
}

// 显示投诉详情
const showComplaintDetail = (complaint) => {
  selectedComplaint.value = complaint
  showDetailDialog.value = true
}

// 处理分页变化
const handlePageChange = (page) => {
  currentPage.value = page
  loadComplaints()
}

// 文件上传相关
const handlePreview = (file) => {
  console.log('预览文件:', file)
}

const handleRemove = (file) => {
  console.log('移除文件:', file)
}

const beforeUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('上传文件大小不能超过 2MB!')
    return false
  }
  return true
}

// 获取状态标签类型
const getStatusTagType = (status) => {
  const statusMap = {
    'PENDING': 'warning',
    'INVESTIGATING': 'primary',
    'RESOLVED': 'success',
    'CLOSED': 'info'
  }
  return statusMap[status] || 'info'
}

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    'PENDING': '待处理',
    'INVESTIGATING': '处理中',
    'RESOLVED': '已解决',
    'CLOSED': '已关闭'
  }
  return statusMap[status] || status
}

// 获取类型标签类型
const getTypeTagType = (type) => {
  const typeMap = {
    'SERVICE': 'primary',
    'SAFETY': 'danger',
    'PAYMENT': 'warning',
    'BEHAVIOR': 'danger',
    'OTHER': 'info'
  }
  return typeMap[type] || 'info'
}

// 获取类型文本
const getTypeText = (type) => {
  const typeMap = {
    'SERVICE': '服务态度',
    'SAFETY': '安全问题',
    'PAYMENT': '费用纠纷',
    'BEHAVIOR': '行为不当',
    'OTHER': '其他问题'
  }
  return typeMap[type] || type
}

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return '--'
  try {
    return new Date(dateTime).toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch (error) {
    return '--'
  }
}

// 页面加载时获取数据
onMounted(() => {
  loadComplaints()
  loadAvailableOrders()
})
</script>

<style scoped>
.driver-complaints {
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

.complaints-container {
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

.complaints-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.complaint-card {
  transition: all 0.3s ease;
  cursor: pointer;
}

.complaint-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.complaint-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.complaint-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.complaint-title {
  font-weight: 600;
  color: #333;
  font-size: 16px;
}

.complaint-time {
  color: #666;
  font-size: 14px;
}

.complaint-content {
  color: #666;
  font-size: 14px;
  line-height: 1.5;
}

.complaint-description {
  margin-bottom: 12px;
}

.complaint-resolution {
  background: #f8f9fa;
  padding: 12px;
  border-radius: 4px;
  border-left: 4px solid #67c23a;
}

.resolution-label {
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}

.resolution-content {
  color: #666;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding: 20px 0;
}

.complaint-detail .description-content,
.complaint-detail .resolution-content {
  white-space: pre-wrap;
  line-height: 1.6;
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

  .complaint-header {
    flex-direction: column;
    gap: 8px;
    align-items: flex-start;
  }
}
</style>