<template>
  <div class="map-search">
    <el-input
      v-model="searchKeyword"
      :placeholder="placeholder"
      clearable
      @input="handleInput"
      @focus="handleFocus"
      @blur="handleBlur"
    >
      <template #prefix>
        <el-icon><Location /></el-icon>
      </template>
    </el-input>
    
    <!-- 搜索结果下拉列表 -->
    <div v-if="showDropdown && searchResults.length > 0" class="search-dropdown">
      <div
        v-for="(place, index) in searchResults"
        :key="index"
        class="search-item"
        @click="selectPlace(place)"
      >
        <div class="place-name">{{ place.name }}</div>
        <div class="place-address">{{ place.address }}</div>
        <div class="place-distance">{{ place.distance }}米</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Location } from '@element-plus/icons-vue'
import { mapApi } from '@/api/map'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: '请输入地址'
  },
  latitude: {
    type: Number,
    default: 39.9042 // 北京默认纬度
  },
  longitude: {
    type: Number,
    default: 116.4074 // 北京默认经度
  },
  radius: {
    type: Number,
    default: 3000
  }
})

const emit = defineEmits(['update:modelValue', 'select'])

const searchKeyword = ref(props.modelValue)
const searchResults = ref([])
const showDropdown = ref(false)
const searchTimeout = ref(null)

// 监听输入值变化
watch(() => props.modelValue, (newVal) => {
  searchKeyword.value = newVal
})

// 处理输入
const handleInput = () => {
  emit('update:modelValue', searchKeyword.value)
  
  // 清除之前的定时器
  if (searchTimeout.value) {
    clearTimeout(searchTimeout.value)
  }
  
  // 延迟搜索，避免频繁请求
  searchTimeout.value = setTimeout(() => {
    if (searchKeyword.value.trim()) {
      searchPlaces()
    } else {
      searchResults.value = []
      showDropdown.value = false
    }
  }, 500)
}

// 处理聚焦
const handleFocus = () => {
  if (searchResults.value.length > 0) {
    showDropdown.value = true
  }
}

// 处理失焦
const handleBlur = () => {
  // 延迟隐藏，让点击事件先执行
  setTimeout(() => {
    showDropdown.value = false
  }, 200)
}

// 搜索地点
const searchPlaces = async () => {
  try {
    const response = await mapApi.searchNearbyPlaces({
      keywords: searchKeyword.value,
      latitude: props.latitude,
      longitude: props.longitude,
      radius: props.radius
    })
    
    searchResults.value = response.data || []
    showDropdown.value = searchResults.value.length > 0
  } catch (error) {
    console.error('搜索地点失败:', error)
    ElMessage.error('搜索地点失败，请稍后重试')
  }
}

// 选择地点
const selectPlace = (place) => {
  searchKeyword.value = place.name
  emit('update:modelValue', place.name)
  emit('select', {
    name: place.name,
    address: place.address,
    location: place.location,
    distance: place.distance
  })
  showDropdown.value = false
}
</script>

<style scoped>
.map-search {
  position: relative;
  width: 100%;
}

.search-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: white;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  z-index: 1000;
  max-height: 300px;
  overflow-y: auto;
}

.search-item {
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f5f5f5;
  transition: background-color 0.3s;
}

.search-item:hover {
  background-color: #f5f7fa;
}

.search-item:last-child {
  border-bottom: none;
}

.place-name {
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.place-address {
  font-size: 12px;
  color: #909399;
  margin-bottom: 2px;
}

.place-distance {
  font-size: 12px;
  color: #c0c4cc;
}
</style> 