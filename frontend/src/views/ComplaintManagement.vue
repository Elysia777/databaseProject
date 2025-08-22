<template>
  <div class="complaint-management">
    <div class="page-header">
      <h2>投诉管理</h2>
      <div class="header-actions">
        <el-select v-model="statusFilter" placeholder="筛选状态" clearable @change="loadComplaints" style="width: 150px;">
          <el-option label="全部" value="" />
          <el-option label="待处理" value="PENDING" />
          <el-option label="处理中" value="INVESTIGATING" />
          <el-option label="已解决" value="RESOLVED" />
          <el-option label="已关闭" value="CLOSED" />
        </el-select>
        <el-select v-model="typeFilter" placeholder="筛选类型" clearable @change="loadComplaints" style="width: 150px;">
          <el-option label="全部" value="" />
          <el-option label="服务质量" value="SERVICE" />
          <el-option label="安全问题" value="SAFETY" />
          <el-option label="费用问题" value="PAYMENT" />
          <el-option label="行为不当" value="BEHAVIOR" />
          <el-option label="其他问题" value="OTHER" />
        </el-select>
        <el-button @click="loadComplaints" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button @click="loadStats" type="primary" plain>
          <el-icon><DataAnalysis /></el-icon>
          统计数据
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-cards" v-if="stats">
      <el-row :gutter="16">
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-number">{{ stats.totalComplaints || 0 }}</div>
              <div class="stat-label">总投诉数</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card pending">
            <div class="stat-content">
              <div class="stat-number">{{ stats.pendingCount || 0 }}</div>
              <div class="stat-label">待处理</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card processing">
            <div class="stat-content">
              <div class="stat-number">{{ stats.investigatingCount || 0 }}</div>
              <div class="stat-label">处理中</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card resolved">
            <div class="stat-content">
              <div class="stat-number">{{ stats.resolvedCount || 0 }}</div>
              <div class="stat-label">已解决</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
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

      <div v-else class="complaints-table">
        <el-table :data="complaints" style="width: 100%" @row-click="showComplaintDetail">
          <el-table-column prop="id" label="投诉ID" width="80" />
          <el-table-column prop="title" label="投诉标题" min-width="200" show-overflow-tooltip />
          <el-table-column prop="complainantName" label="投诉人" width="120" />
          <el-table-column prop="defendantName" label="被投诉人" width="120" />
          <el-table-column prop="complaint_type" label="投诉类型" width="120">
            <template #default="{ row }">
              <el-tag :type="getTypeTagType(row.complaint_type)" size="small">
                {{ getTypeText(row.complaint_type) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="处理状态" width="120">
            <template #default="{ row }">
              <el-tag :type="getStatusTagType(row.status)" size="small">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="refund_amount" label="退款金额" width="120">
            <template #default="{ row }">
              <span v-if="row.refund_amount && row.refund_amount > 0" class="refund-amount">
                ¥{{ row.refund_amount.toFixed(2) }}
              </span>
              <span v-else class="no-refund">--</span>
            </template>
          </el-table-column>
          <el-table-column prop="created_at" label="提交时间" width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.created_at) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click.stop="showComplaintDetail(row)">查看</el-button>
              <el-button 
                size="small" 
                type="primary" 
                @click.stop="showProcessDialogHandler(row)"
                :disabled="row.status === 'RESOLVED' || row.status === 'CLOSED'">
                处理
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div v-if="complaints.length > 0" class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="totalComplaints"
          layout="total, sizes, prev, pager, next, jumper"
          :page-sizes="[10, 20, 50, 100]"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- 投诉详情对话框 -->
    <el-dialog v-model="showDetailDialog" title="投诉详情" width="800px">
      <div v-if="selectedComplaint" class="complaint-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="投诉ID">{{ selectedComplaint.id }}</el-descriptions-item>
          <el-descriptions-item label="关联订单">{{ selectedComplaint.orderNumber || '无' }}</el-descriptions-item>
          <el-descriptions-item label="投诉人">{{ selectedComplaint.complainantName }}</el-descriptions-item>
          <el-descriptions-item label="被投诉人">{{ selectedComplaint.defendantName }}</el-descriptions-item>
          <el-descriptions-item label="投诉类型">
            <el-tag :type="getTypeTagType(selectedComplaint.complaint_type)">
              {{ getTypeText(selectedComplaint.complaint_type) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="处理状态">
            <el-tag :type="getStatusTagType(selectedComplaint.status)">
              {{ getStatusText(selectedComplaint.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="投诉标题" :span="2">{{ selectedComplaint.title }}</el-descriptions-item>
          <el-descriptions-item label="投诉描述" :span="2">
            <div class="description-content">{{ selectedComplaint.description }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ formatDateTime(selectedComplaint.created_at) }}</el-descriptions-item>
          <el-descriptions-item label="处理时间" v-if="selectedComplaint.resolution_time">
            {{ formatDateTime(selectedComplaint.resolution_time) }}
          </el-descriptions-item>
          <el-descriptions-item label="处理管理员" v-if="selectedComplaint.adminName">
            {{ selectedComplaint.adminName }}
          </el-descriptions-item>
          <el-descriptions-item label="处理结果" :span="2" v-if="selectedComplaint.resolution">
            <div class="resolution-content">{{ selectedComplaint.resolution }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="退款金额" v-if="selectedComplaint.refund_amount && selectedComplaint.refund_amount > 0">
            <span class="refund-amount">¥{{ selectedComplaint.refund_amount.toFixed(2) }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 订单信息 -->
        <div v-if="selectedComplaint.orderNumber" class="order-info">
          <h4>关联订单信息</h4>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="订单号">{{ selectedComplaint.orderNumber }}</el-descriptions-item>
            <el-descriptions-item label="订单金额">¥{{ selectedComplaint.actual_fare || '0.00' }}</el-descriptions-item>
            <el-descriptions-item label="起点地址" :span="2">{{ selectedComplaint.pickup_address || '无' }}</el-descriptions-item>
            <el-descriptions-item label="终点地址" :span="2">{{ selectedComplaint.destination_address || '无' }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
    </el-dialog>

    <!-- 处理投诉对话框 -->
    <el-dialog v-model="showProcessDialog" title="处理投诉" width="600px">
      <el-form :model="processForm" :rules="processRules" ref="processFormRef" label-width="100px">
        <el-form-item label="处理状态" prop="status">
          <el-select v-model="processForm.status" placeholder="选择处理状态" style="width: 100%;">
            <el-option label="处理中" value="INVESTIGATING" />
            <el-option label="已解决" value="RESOLVED" />
            <el-option label="已关闭" value="CLOSED" />
          </el-select>
        </el-form-item>

        <el-form-item label="处理结果" prop="resolution">
          <el-input 
            v-model="processForm.resolution" 
            type="textarea" 
            :rows="4" 
            placeholder="请输入处理结果和说明" />
        </el-form-item>

        <el-form-item label="退款金额" v-if="processForm.status === 'RESOLVED'">
          <el-input-number 
            v-model="processForm.refundAmount" 
            :min="0" 
            :precision="2" 
            :disabled="isOrderAlreadyRefunded"
            placeholder="退款金额（可选）"
            style="width: 100%;" />
          <div class="form-tip" v-if="!isOrderAlreadyRefunded">如需退款，请输入退款金额</div>
          <div class="form-warning" v-if="isOrderAlreadyRefunded">
            <el-icon><Warning /></el-icon>
            该订单已经退款，不能重复退款
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showProcessDialog = false">取消</el-button>
          <el-button type="primary" @click="processComplaint" :loading="processing">确认处理</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Loading, DataAnalysis, Warning } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// 响应式数据
const complaints = ref([])
const stats = ref(null)
const loading = ref(false)
const processing = ref(false)
const statusFilter = ref('')
const typeFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const totalComplaints = ref(0)

// 对话框状态
const showDetailDialog = ref(false)
const showProcessDialog = ref(false)
const selectedComplaint = ref(null)

// 处理表单
const processForm = ref({
  status: '',
  resolution: '',
  refundAmount: null
})

const processRules = {
  status: [
    { required: true, message: '请选择处理状态', trigger: 'change' }
  ],
  resolution: [
    { required: true, message: '请输入处理结果', trigger: 'blur' },
    { min: 10, max: 1000, message: '处理结果长度在 10 到 1000 个字符', trigger: 'blur' }
  ]
}

const processFormRef = ref()

// 计算属性
const adminId = computed(() => userStore.user?.id)

// 检查订单是否已经退款
const isOrderAlreadyRefunded = computed(() => {
  if (!selectedComplaint.value) return false
  
  // 检查订单支付状态是否为REFUNDED
  if (selectedComplaint.value.payment_status === 'REFUNDED') {
    return true
  }
  
  // 检查是否已有退款金额
  if (selectedComplaint.value.refund_amount && selectedComplaint.value.refund_amount > 0) {
    return true
  }
  
  return false
})

// 加载投诉列表
const loadComplaints = async () => {
  loading.value = true
  try {
    const params = new URLSearchParams({
      page: currentPage.value.toString(),
      size: pageSize.value.toString()
    })
    
    if (statusFilter.value) {
      params.append('status', statusFilter.value)
    }
    if (typeFilter.value) {
      params.append('complaintType', typeFilter.value)
    }

    const response = await fetch(`/api/complaints/admin/list?${params}`, {
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })

    const result = await response.json()
    
    if (result.code === 200) {
      complaints.value = result.data?.complaints || []
      totalComplaints.value = result.data?.total || 0
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

// 加载统计数据
const loadStats = async () => {
  try {
    const response = await fetch('/api/complaints/admin/stats', {
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })

    const result = await response.json()
    
    if (result.code === 200) {
      stats.value = result.data
    } else {
      ElMessage.error(result.message || '加载统计数据失败')
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
    ElMessage.error('加载统计数据失败')
  }
}

// 显示投诉详情
const showComplaintDetail = (complaint) => {
  selectedComplaint.value = complaint
  showDetailDialog.value = true
}

// 显示处理对话框
const showProcessDialogHandler = (complaint) => {
  selectedComplaint.value = complaint
  processForm.value = {
    status: complaint.status === 'PENDING' ? 'INVESTIGATING' : complaint.status,
    resolution: complaint.resolution || '',
    refundAmount: null
  }
  showProcessDialog.value = true
}

// 处理投诉
const processComplaint = async () => {
  if (!processFormRef.value) return

  try {
    await processFormRef.value.validate()
    
    processing.value = true

    const response = await fetch(`/api/complaints/admin/process/${selectedComplaint.value.id}?adminId=${adminId.value}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${userStore.token}`
      },
      body: JSON.stringify(processForm.value)
    })

    const result = await response.json()
    
    if (result.code === 200) {
      ElMessage.success('投诉处理成功')
      showProcessDialog.value = false
      loadComplaints()
      loadStats()
    } else {
      ElMessage.error(result.message || '处理投诉失败')
    }
  } catch (error) {
    console.error('处理投诉失败:', error)
    ElMessage.error('处理失败，请稍后重试')
  } finally {
    processing.value = false
  }
}

// 处理分页变化
const handlePageChange = (page) => {
  currentPage.value = page
  loadComplaints()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadComplaints()
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
    'SERVICE': '服务质量',
    'SAFETY': '安全问题',
    'PAYMENT': '费用问题',
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
  loadStats()
})
</script>

<style scoped>
.complaint-management {
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
  flex-wrap: wrap;
}

.stats-cards {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-card.pending {
  border-left: 4px solid #e6a23c;
}

.stat-card.processing {
  border-left: 4px solid #409eff;
}

.stat-card.resolved {
  border-left: 4px solid #67c23a;
}

.stat-content {
  padding: 8px 0;
}

.stat-number {
  font-size: 28px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #666;
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

.complaints-table {
  background: white;
  border-radius: 8px;
}

.complaints-table :deep(.el-table__row) {
  cursor: pointer;
}

.complaints-table :deep(.el-table__row:hover) {
  background-color: #f5f7fa;
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
  max-height: 200px;
  overflow-y: auto;
}

.order-info {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.order-info h4 {
  margin: 0 0 16px 0;
  color: #333;
}

.form-tip {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.form-warning {
  font-size: 12px;
  color: #e6a23c;
  margin-top: 4px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.refund-amount {
  color: #e6a23c;
  font-weight: 600;
  font-size: 14px;
}

.no-refund {
  color: #c0c4cc;
  font-size: 12px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }

  .header-actions {
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .stats-cards .el-col {
    margin-bottom: 16px;
  }

  .complaints-table {
    overflow-x: auto;
  }
}
</style>