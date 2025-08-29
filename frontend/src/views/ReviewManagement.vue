<template>
  <div class="review-management">
    <div class="page-header">
      <h2>评价管理</h2>
      <div class="header-actions">
        <el-button @click="refreshData" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-icon total">
              <el-icon><Star /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-number">{{ stats.totalReviews }}</div>
              <div class="stats-label">总评价数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-icon average">
              <el-icon><TrendCharts /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-number">{{ stats.averageRating }}</div>
              <div class="stats-label">平均评分</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-icon today">
              <el-icon><Calendar /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-number">{{ stats.todayReviews }}</div>
              <div class="stats-label">今日评价</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-icon complaints">
              <el-icon><Warning /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-number">{{ stats.lowRatingReviews }}</div>
              <div class="stats-label">低分评价</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选条件 -->
    <el-card class="filter-card">
      <el-form :model="filters" inline>
        <el-form-item label="评分">
          <el-select v-model="filters.rating" placeholder="选择评分" clearable style="width: 150px">
            <el-option label="5星" :value="5" />
            <el-option label="4星" :value="4" />
            <el-option label="3星" :value="3" />
            <el-option label="2星" :value="2" />
            <el-option label="1星" :value="1" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="评价类型">
          <el-select v-model="filters.type" placeholder="选择类型" clearable style="width: 180px">
            <el-option label="乘客评价司机" value="PASSENGER_TO_DRIVER" />
            <el-option label="司机评价乘客" value="DRIVER_TO_PASSENGER" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        
        <el-form-item label="关键词">
          <el-input
            v-model="filters.keyword"
            placeholder="搜索评价内容"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="searchReviews">搜索</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 评价列表 -->
    <el-card class="table-card">
      <el-table
        :data="reviews"
        v-loading="loading"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        
        <el-table-column prop="id" label="ID" width="80" />
        
        <el-table-column label="评分" width="120">
          <template #default="scope">
            <el-rate
              v-model="scope.row.rating"
              disabled
              show-score
              text-color="#ff9900"
              score-template="{value}"
            />
          </template>
        </el-table-column>
        
        <el-table-column label="评价者" width="120">
          <template #default="scope">
            <div class="user-info">
              <img 
                :src="getAvatarUrl(scope.row.reviewerAvatar)" 
                :alt="scope.row.reviewerName"
                class="user-avatar"
                @error="handleImageError"
              />
              <span>{{ scope.row.reviewerName }}</span>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column label="被评价者" width="120">
          <template #default="scope">
            <div class="user-info">
              <img 
                :src="getAvatarUrl(scope.row.revieweeAvatar)" 
                :alt="scope.row.revieweeName"
                class="user-avatar"
                @error="handleImageError"
              />
              <span>{{ scope.row.revieweeName }}</span>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column label="评价类型" width="120">
          <template #default="scope">
            <el-tag :type="scope.row.reviewType === 'PASSENGER_TO_DRIVER' ? 'primary' : 'success'">
              {{ scope.row.reviewType === 'PASSENGER_TO_DRIVER' ? '乘客→司机' : '司机→乘客' }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="comment" label="评价内容" min-width="200">
          <template #default="scope">
            <div class="comment-content">
              {{ scope.row.comment || '无评价内容' }}
            </div>
          </template>
        </el-table-column>
        
        <el-table-column label="评价标签" width="200">
          <template #default="scope">
            <div class="tags-container">
              <el-tag
                v-for="tag in parseTagsFromString(scope.row.tags)"
                :key="tag"
                size="small"
                type="info"
                class="tag-item"
              >
                {{ tag }}
              </el-tag>
              <span v-if="!scope.row.tags || parseTagsFromString(scope.row.tags).length === 0" class="no-tags">
                无标签
              </span>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="orderId" label="订单ID" width="100" />
        
        <el-table-column label="评价时间" width="180">
          <template #default="scope">
            {{ formatTime(scope.row.createdAt) }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="scope">
            <el-button
              type="primary"
              size="small"
              @click="viewReviewDetail(scope.row)"
            >
              查看详情
            </el-button>
            <el-button
              type="danger"
              size="small"
              @click="deleteReview(scope.row)"
              v-if="scope.row.rating <= 2"
            >
              删除
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

    <!-- 评价详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="评价详情"
      width="600px"
    >
      <div v-if="selectedReview" class="review-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="评价ID">{{ selectedReview.id }}</el-descriptions-item>
          <el-descriptions-item label="订单ID">{{ selectedReview.orderId }}</el-descriptions-item>
          <el-descriptions-item label="评分">
            <el-rate v-model="selectedReview.rating" disabled show-score />
          </el-descriptions-item>
          <el-descriptions-item label="评价类型">
            <el-tag :type="selectedReview.reviewType === 'PASSENGER_TO_DRIVER' ? 'primary' : 'success'">
              {{ selectedReview.reviewType === 'PASSENGER_TO_DRIVER' ? '乘客评价司机' : '司机评价乘客' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="评价者">{{ selectedReview.reviewerName }}</el-descriptions-item>
          <el-descriptions-item label="被评价者">{{ selectedReview.revieweeName }}</el-descriptions-item>
          <el-descriptions-item label="评价时间" :span="2">{{ formatTime(selectedReview.createdAt) }}</el-descriptions-item>
        </el-descriptions>
        
        <div class="comment-section">
          <h4>评价内容</h4>
          <div class="comment-text">
            {{ selectedReview.comment || '无评价内容' }}
          </div>
        </div>
        
        <div class="tags-section">
          <h4>评价标签</h4>
          <div class="tags-display">
            <el-tag
              v-for="tag in parseTagsFromString(selectedReview.tags)"
              :key="tag"
              type="info"
              class="tag-item"
            >
              {{ tag }}
            </el-tag>
            <span v-if="!selectedReview.tags || parseTagsFromString(selectedReview.tags).length === 0" class="no-tags">
              无标签
            </span>
          </div>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button 
          type="danger" 
          @click="deleteReview(selectedReview)"
          v-if="selectedReview && selectedReview.rating <= 2"
        >
          删除评价
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// 响应式数据
const loading = ref(false)
const reviews = ref([])
const selectedReviews = ref([])
const detailDialogVisible = ref(false)
const selectedReview = ref(null)

// 统计数据
const stats = reactive({
  totalReviews: 0,
  averageRating: 0,
  todayReviews: 0,
  lowRatingReviews: 0
})

// 筛选条件
const filters = reactive({
  rating: null,
  type: null,
  dateRange: null,
  keyword: ''
})

// 分页
const pagination = reactive({
  currentPage: 1,
  pageSize: 20,
  total: 0
})

// 方法
const loadReviews = async () => {
  loading.value = true
  try {
    console.log('正在加载评价数据...')
    
    const response = await fetch('/api/reviews/all')
    console.log('评价API响应状态:', response.status)
    
    if (response.ok) {
      const data = await response.json()
      console.log('评价数据:', data)
      
      const isSuccess = data.success === true || data.code === 200
      if (isSuccess && data.data) {
        const reviewsData = data.data
        
        // 处理评价数据
        reviews.value = reviewsData.map(review => ({
          ...review,
          reviewerName: review.reviewerName || review.raterName || '未知用户',
          revieweeName: review.revieweeName || review.ratedName || '未知用户',
          reviewerAvatar: review.reviewerAvatar || review.raterAvatar || null,
          revieweeAvatar: review.revieweeAvatar || review.ratedAvatar || null,
          reviewType: review.reviewType || 'PASSENGER_TO_DRIVER'
        }))
        
        // 更新统计数据
        updateStats(reviewsData)
        
        pagination.total = reviewsData.length
      } else {
        console.warn('评价数据格式不正确:', data)
        reviews.value = []
      }
    } else {
      console.warn('评价API请求失败:', response.status)
      reviews.value = []
    }
  } catch (error) {
    console.error('加载评价数据失败:', error)
    ElMessage.error(`加载评价数据失败: ${error.message}`)
    reviews.value = []
  } finally {
    loading.value = false
  }
}

const updateStats = (reviewsData) => {
  stats.totalReviews = reviewsData.length
  
  if (reviewsData.length > 0) {
    const totalRating = reviewsData.reduce((sum, review) => sum + (review.rating || 0), 0)
    stats.averageRating = (totalRating / reviewsData.length).toFixed(1)
  } else {
    stats.averageRating = 0
  }
  
  // 今日评价数
  const today = new Date().toISOString().split('T')[0]
  stats.todayReviews = reviewsData.filter(review => {
    const reviewDate = review.createdAt || review.created_at
    return reviewDate && reviewDate.startsWith(today)
  }).length
  
  // 低分评价数（1-2星）
  stats.lowRatingReviews = reviewsData.filter(review => review.rating <= 2).length
}

const searchReviews = async () => {
  loading.value = true
  try {
    console.log('正在搜索评价数据...', filters)
    
    const response = await fetch('/api/reviews/all')
    if (response.ok) {
      const data = await response.json()
      const isSuccess = data.success === true || data.code === 200
      
      if (isSuccess && data.data) {
        let filteredData = data.data
        
        // 按评分筛选
        if (filters.rating !== null) {
          filteredData = filteredData.filter(review => 
            Math.floor(review.rating) === filters.rating
          )
        }
        
        // 按评价类型筛选
        if (filters.type) {
          filteredData = filteredData.filter(review => 
            review.reviewType === filters.type || 
            (filters.type === 'passenger_to_driver' && review.reviewType === 'PASSENGER_TO_DRIVER') ||
            (filters.type === 'driver_to_passenger' && review.reviewType === 'DRIVER_TO_PASSENGER')
          )
        }
        
        // 按时间范围筛选
        if (filters.dateRange && filters.dateRange.length === 2) {
          const [startDate, endDate] = filters.dateRange
          filteredData = filteredData.filter(review => {
            const reviewDate = new Date(review.createdAt || review.created_at)
            const start = new Date(startDate)
            const end = new Date(endDate)
            end.setHours(23, 59, 59, 999) // 包含结束日期的整天
            return reviewDate >= start && reviewDate <= end
          })
        }
        
        // 按关键词搜索
        if (filters.keyword && filters.keyword.trim()) {
          const keyword = filters.keyword.trim().toLowerCase()
          filteredData = filteredData.filter(review => 
            (review.comment && review.comment.toLowerCase().includes(keyword)) ||
            (review.reviewerName && review.reviewerName.toLowerCase().includes(keyword)) ||
            (review.revieweeName && review.revieweeName.toLowerCase().includes(keyword))
          )
        }
        
        // 处理筛选后的数据
        reviews.value = filteredData.map(review => ({
          ...review,
          reviewerName: review.reviewerName || review.raterName || '未知用户',
          revieweeName: review.revieweeName || review.ratedName || '未知用户',
          reviewerAvatar: review.reviewerAvatar || review.raterAvatar || null,
          revieweeAvatar: review.revieweeAvatar || review.ratedAvatar || null,
          reviewType: review.reviewType || 'PASSENGER_TO_DRIVER'
        }))
        
        // 更新统计数据
        updateStats(filteredData)
        pagination.total = filteredData.length
        
        ElMessage.success(`找到 ${filteredData.length} 条符合条件的评价`)
      }
    }
  } catch (error) {
    console.error('搜索评价数据失败:', error)
    ElMessage.error(`搜索失败: ${error.message}`)
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  filters.rating = null
  filters.type = null
  filters.dateRange = null
  filters.keyword = ''
  loadReviews()
}

const refreshData = () => {
  loadReviews()
  ElMessage.success('数据已刷新')
}

const handleSelectionChange = (selection) => {
  selectedReviews.value = selection
}

const viewReviewDetail = (review) => {
  selectedReview.value = review
  detailDialogVisible.value = true
}

const deleteReview = async (review) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除这条评价吗？此操作不可恢复。`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    if (!review || !review.id) {
      ElMessage.error('无效的评价信息，无法删除')
      return
    }

    const resp = await fetch(`/api/reviews/${review.id}`, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json'
      }
    })

    if (!resp.ok) {
      throw new Error(`HTTP ${resp.status}`)
    }

    const data = await resp.json()
    const isSuccess = data.success === true || data.code === 200
    if (isSuccess) {
      ElMessage.success('评价已删除')
      detailDialogVisible.value = false
      await loadReviews()
    } else {
      ElMessage.error(data.message || '删除失败')
    }
  } catch (e) {
    if (e && e.message) {
      // 只有在不是用户取消的情况下提示错误
      if (!/cancel/i.test(e.message)) {
        ElMessage.error(`删除失败: ${e.message}`)
      }
    }
  }
}

const handleSizeChange = (size) => {
  pagination.pageSize = size
  loadReviews()
}

const handleCurrentChange = (page) => {
  pagination.currentPage = page
  loadReviews()
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString()
}

const getAvatarUrl = (avatar) => {
  if (!avatar || avatar === 'null' || avatar === 'undefined') {
    return 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
  }
  if (avatar.startsWith('http')) return avatar
  // 修复头像路径，去掉重复的路径部分
  if (avatar.startsWith('/uploads/')) {
    return `http://localhost:8080${avatar}`
  }
  return `http://localhost:8080/uploads/avatars/${avatar}`
}

const handleImageError = (event) => {
  event.target.src = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
}

const parseTagsFromString = (tagsString) => {
  if (!tagsString || tagsString === 'null' || tagsString === 'undefined') {
    return []
  }
  
  try {
    // 尝试解析JSON字符串
    const parsed = JSON.parse(tagsString)
    if (Array.isArray(parsed)) {
      return parsed.filter(tag => tag && tag.trim())
    }
    return []
  } catch (error) {
    // 如果不是JSON格式，尝试按逗号分割
    if (typeof tagsString === 'string') {
      return tagsString.split(',').map(tag => tag.trim()).filter(tag => tag)
    }
    return []
  }
}

onMounted(() => {
  loadReviews()
})
</script>

<style scoped>
.review-management {
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

.stats-icon.average {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.stats-icon.today {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stats-icon.complaints {
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

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.comment-content {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.review-detail {
  padding: 20px 0;
}

.comment-section {
  margin-top: 20px;
}

.comment-section h4 {
  margin-bottom: 10px;
  color: #333;
}

.comment-text {
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
  line-height: 1.6;
  min-height: 60px;
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
}

.tag-item {
  margin: 2px;
}

.no-tags {
  color: #999;
  font-size: 12px;
  font-style: italic;
}

.tags-section {
  margin-top: 20px;
}

.tags-section h4 {
  margin-bottom: 10px;
  color: #333;
}

.tags-display {
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
  min-height: 40px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}
</style>