<template>
  <el-dialog
    v-model="visible"
    title="评价此次行程"
    width="400px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="review-dialog">
      <!-- 司机信息 -->
      <div class="driver-info">
        <div class="driver-avatar">
          <img 
            :src="buildAvatarUrl(driverInfo?.avatar) || getDefaultAvatar(driverInfo?.name)" 
            :alt="driverInfo?.name"
            @error="handleAvatarError"
          />
        </div>
        <div class="driver-details">
          <div class="driver-name">{{ driverInfo?.name || '司机' }}</div>
          <div class="vehicle-info">{{ driverInfo?.plateNumber }} · {{ driverInfo?.carModel }}</div>
        </div>
      </div>

      <!-- 评分 -->
      <div class="rating-section">
        <div class="section-title">请为此次行程打分</div>
        <div class="star-rating">
          <span 
            v-for="i in 5" 
            :key="i"
            class="star"
            :class="{ active: i <= rating }"
            @click="setRating(i)"
            @mouseover="hoverRating = i"
            @mouseleave="hoverRating = 0"
          >
            ★
          </span>
        </div>
        <div class="rating-text">{{ getRatingText(rating) }}</div>
      </div>

      <!-- 评价标签 -->
      <div class="tags-section">
        <div class="section-title">选择标签（可选）</div>
        <div class="tags-container">
          <el-tag
            v-for="tag in availableTags"
            :key="tag"
            :type="selectedTags.includes(tag) ? 'primary' : 'info'"
            :effect="selectedTags.includes(tag) ? 'dark' : 'plain'"
            @click="toggleTag(tag)"
            class="tag-item"
          >
            {{ tag }}
          </el-tag>
        </div>
      </div>

      <!-- 评价内容 -->
      <div class="comment-section">
        <div class="section-title">详细评价（可选）</div>
        <el-input
          v-model="comment"
          type="textarea"
          :rows="3"
          placeholder="分享您的乘车体验..."
          maxlength="200"
          show-word-limit
        />
      </div>


    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">稍后评价</el-button>
        <el-button 
          type="primary" 
          @click="submitReview"
          :loading="submitting"
          :disabled="rating === 0"
        >
          提交评价
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'

// Props
const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  orderInfo: {
    type: Object,
    default: () => ({})
  },
  driverInfo: {
    type: Object,
    default: () => ({})
  }
})

// Emits
const emit = defineEmits(['update:modelValue', 'review-submitted'])

// 响应式数据
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const rating = ref(0)
const hoverRating = ref(0)
const comment = ref('')
const selectedTags = ref([])
const submitting = ref(false)

// 可选标签
const availableTags = [
  '服务好', '准时', '驾驶平稳', '车辆干净', '路线熟悉',
  '态度友好', '车况良好', '空调适宜', '音乐合适', '无异味'
]

// 方法
const setRating = (value) => {
  rating.value = value
}

const getRatingText = (value) => {
  const texts = {
    0: '请选择评分',
    1: '很不满意',
    2: '不满意', 
    3: '一般',
    4: '满意',
    5: '非常满意'
  }
  return texts[value] || ''
}

const toggleTag = (tag) => {
  const index = selectedTags.value.indexOf(tag)
  if (index > -1) {
    selectedTags.value.splice(index, 1)
  } else {
    selectedTags.value.push(tag)
  }
}

// 构建完整的头像URL
const buildAvatarUrl = (avatarPath) => {
  if (!avatarPath) return null;
  if (avatarPath.startsWith('http')) return avatarPath;
  // 添加服务器前缀
  return `http://localhost:8080${avatarPath}`;
};

const getDefaultAvatar = (name) => {
  return `https://ui-avatars.com/api/?name=${encodeURIComponent(name || '司机')}&background=409EFF&color=fff&size=40`
}

const handleAvatarError = (event) => {
  event.target.src = getDefaultAvatar(props.driverInfo?.name)
}

const submitReview = async () => {
  if (rating.value === 0) {
    ElMessage.warning('请先选择评分')
    return
  }

  submitting.value = true
  
  try {
    const reviewData = {
      orderId: props.orderInfo.id,
      raterId: props.orderInfo.passengerId,
      ratedId: props.orderInfo.driverId,
      rating: rating.value,
      comment: comment.value.trim(),
      tags: JSON.stringify(selectedTags.value)
    }

    console.log('提交评价数据:', reviewData)

    const response = await fetch('/api/reviews', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(reviewData)
    })

    if (response.ok) {
      const result = await response.json()
      if (result.code === 200) {
        ElMessage.success('评价提交成功')
        emit('review-submitted', result.data)
        handleClose()
      } else {
        ElMessage.error(result.message || '评价提交失败')
      }
    } else {
      ElMessage.error('评价提交失败')
    }
  } catch (error) {
    console.error('提交评价失败:', error)
    ElMessage.error('评价提交失败: ' + error.message)
  } finally {
    submitting.value = false
  }
}

const handleClose = () => {
  visible.value = false
}

// 重置表单
const resetForm = () => {
  rating.value = 0
  hoverRating.value = 0
  comment.value = ''
  selectedTags.value = []
}

// 监听对话框显示状态
watch(visible, (newValue) => {
  if (newValue) {
    resetForm()
  }
})
</script>

<style scoped>
.review-dialog {
  padding: 10px 0;
}

.driver-info {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.driver-avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 12px;
}

.driver-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.driver-details {
  flex: 1;
}

.driver-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}

.vehicle-info {
  font-size: 14px;
  color: #666;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
}

.rating-section {
  margin-bottom: 24px;
  text-align: center;
}

.star-rating {
  margin: 16px 0;
}

.star {
  font-size: 32px;
  color: #ddd;
  cursor: pointer;
  margin: 0 4px;
  transition: color 0.2s;
}

.star:hover,
.star.active {
  color: #ffd700;
}

.rating-text {
  font-size: 14px;
  color: #666;
  margin-top: 8px;
}

.tags-section {
  margin-bottom: 24px;
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-item {
  cursor: pointer;
  transition: all 0.2s;
}

.tag-item:hover {
  transform: translateY(-1px);
}

.comment-section {
  margin-bottom: 16px;
}



.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .star {
    font-size: 28px;
    margin: 0 2px;
  }
  
  .tags-container {
    gap: 6px;
  }
}
</style>