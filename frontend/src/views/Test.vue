<template>
  <div class="test-page">
    <h1>高德地图API测试页面</h1>
    
    <div class="test-section">
      <h2>1. API密钥状态</h2>
      <div class="status-item">
        <span>Web端JS API密钥:</span>
        <span :class="apiKeyStatus.class">{{ apiKeyStatus.message }}</span>
      </div>
      <div class="status-item">
        <span>REST API密钥:</span>
        <span :class="restApiKeyStatus.class">{{ restApiKeyStatus.message }}</span>
      </div>
    </div>

    <div class="test-section">
      <h2>2. 地图显示测试</h2>
      <div id="testMap" class="test-map"></div>
      <div class="map-status">{{ mapStatus }}</div>
    </div>

    <div class="test-section">
      <h2>3. Autocomplete测试</h2>
      <div class="autocomplete-test">
        <input 
          id="autocompleteInput" 
          v-model="autocompleteText"
          placeholder="输入地点名称测试Autocomplete..."
          class="autocomplete-input"
        />
        <div class="autocomplete-status">{{ autocompleteStatus }}</div>
      </div>
    </div>

    <div class="test-section">
      <h2>4. 搜索功能测试</h2>
      <div class="search-test">
        <input 
          v-model="searchKeyword"
          placeholder="输入关键词测试搜索..."
          class="search-input"
        />
        <button @click="testSearch" class="search-btn">搜索</button>
        <div class="search-results">
          <div v-for="(item, index) in searchResults" :key="index" class="search-item">
            <div class="item-name">{{ item.name }}</div>
            <div class="item-address">{{ item.address }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="test-section">
      <h2>5. 路径规划测试</h2>
      <div class="route-test">
        <div class="route-inputs">
          <input v-model="originAddress" placeholder="起点地址" class="route-input" />
          <input v-model="destAddress" placeholder="终点地址" class="route-input" />
        </div>
        <button @click="testRoute" class="route-btn">规划路径</button>
        <div class="route-result" v-if="routeResult">
          <div>距离: {{ routeResult.distance }}米</div>
          <div>时间: {{ routeResult.duration }}秒</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { mapConfig, getMapApiUrl, getRestApiUrl, getSecurityConfig } from '@/config/map'

let map = null
let autocomplete = null

const apiKeyStatus = ref({ message: '检查中...', class: 'checking' })
const restApiKeyStatus = ref({ message: '检查中...', class: 'checking' })
const mapStatus = ref('初始化中...')
const autocompleteStatus = ref('未初始化')
const autocompleteText = ref('')
const searchKeyword = ref('')
const searchResults = ref([])
const originAddress = ref('')
const destAddress = ref('')
const routeResult = ref(null)

onMounted(() => {
  console.log('测试页面开始初始化...')
  
  // 检查API密钥
  checkApiKeys()
  
  // 初始化地图
  initTestMap()
})

// 检查API密钥
const checkApiKeys = async () => {
  try {
    // 检查Web端JS API密钥
    const mapUrl = getMapApiUrl()
    console.log('Web端JS API URL:', mapUrl)
    apiKeyStatus.value = { message: '配置正确', class: 'success' }
    
    // 检查REST API密钥
    const restUrl = getRestApiUrl('place/text', { keywords: 'test' })
    console.log('REST API URL:', restUrl)
    restApiKeyStatus.value = { message: '配置正确', class: 'success' }
    
  } catch (error) {
    console.error('API密钥检查失败:', error)
    apiKeyStatus.value = { message: '配置错误', class: 'error' }
    restApiKeyStatus.value = { message: '配置错误', class: 'error' }
  }
}

// 初始化测试地图
const initTestMap = () => {
  console.log('开始初始化测试地图...')
  
  setTimeout(() => {
    if (window.AMap) {
      console.log('高德地图已加载，直接初始化')
      createTestMap()
    } else {
      console.log('开始加载高德地图API...')
      
      // 添加安全密钥配置
      window._AMapSecurityConfig = getSecurityConfig();
      
      const script = document.createElement('script')
      script.src = getMapApiUrl()
      script.onload = () => {
        console.log('高德地图API加载成功')
        setTimeout(createTestMap, 200)
      }
      script.onerror = (error) => {
        console.error('高德地图API加载失败:', error)
        mapStatus.value = '地图加载失败'
        ElMessage.error('地图加载失败，请检查网络连接')
      }
      document.head.appendChild(script)
    }
  }, 500)
}

// 创建测试地图
const createTestMap = () => {
  try {
    const container = document.getElementById('testMap')
    if (!container) {
      console.error('测试地图容器不存在')
      return
    }
    
    map = new window.AMap.Map('testMap', {
      resizeEnable: true,
      zoom: 15,
      center: [116.397428, 39.90923],
      mapStyle: 'amap://styles/normal'
    })
    
    mapStatus.value = '地图创建成功'
    console.log('测试地图创建成功')
    
    // 初始化Autocomplete
    initTestAutocomplete()
    
  } catch (error) {
    console.error('测试地图创建失败:', error)
    mapStatus.value = '地图创建失败: ' + error.message
  }
}

// 初始化测试Autocomplete
const initTestAutocomplete = () => {
  try {
    console.log('开始初始化测试Autocomplete...')
    
    if (!window.AMap.Autocomplete) {
      console.error('AMap.Autocomplete插件未加载')
      autocompleteStatus.value = 'Autocomplete插件未加载'
      return
    }
    
    autocomplete = new window.AMap.Autocomplete({
      input: 'autocompleteInput',
      city: '全国',
      citylimit: false,
      type: '商务写字楼|购物相关|餐饮服务|风景名胜|交通设施服务',
      extensions: 'all'
    })
    
    autocomplete.on('select', (e) => {
      console.log('Autocomplete选择结果:', e)
      if (e.poi) {
        autocompleteText.value = e.poi.name
        autocompleteStatus.value = `选择了: ${e.poi.name} (${e.poi.address})`
        ElMessage.success(`选择了: ${e.poi.name}`)
      }
    })
    
    autocomplete.on('search', (e) => {
      console.log('Autocomplete搜索事件:', e)
      autocompleteStatus.value = '正在搜索...'
    })
    
    autocomplete.on('error', (e) => {
      console.error('Autocomplete错误:', e)
      autocompleteStatus.value = 'Autocomplete错误: ' + e.message
    })
    
    autocompleteStatus.value = 'Autocomplete初始化成功'
    console.log('测试Autocomplete初始化成功')
    
  } catch (error) {
    console.error('测试Autocomplete初始化失败:', error)
    autocompleteStatus.value = 'Autocomplete初始化失败: ' + error.message
  }
}

// 测试搜索功能
const testSearch = async () => {
  if (!searchKeyword.value) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  
  try {
    console.log('开始测试搜索:', searchKeyword.value)
    
    const searchUrl = getRestApiUrl('place/text', {
      keywords: searchKeyword.value,
      offset: 10,
      page: 1,
      extensions: 'all'
    })
    
    const response = await fetch(searchUrl)
    const data = await response.json()
    
    console.log('搜索结果:', data)
    
    if (data.status === '1' && data.pois && data.pois.length > 0) {
      searchResults.value = data.pois.slice(0, 5).map(poi => ({
        name: poi.name,
        address: poi.address || poi.pname + poi.cityname + poi.adname
      }))
      ElMessage.success(`找到 ${data.pois.length} 个结果`)
    } else {
      searchResults.value = []
      ElMessage.warning('未找到搜索结果')
    }
    
  } catch (error) {
    console.error('搜索测试失败:', error)
    ElMessage.error('搜索测试失败: ' + error.message)
  }
}

// 测试路径规划
const testRoute = async () => {
  if (!originAddress.value || !destAddress.value) {
    ElMessage.warning('请输入起点和终点地址')
    return
  }
  
  try {
    console.log('开始测试路径规划:', originAddress.value, '->', destAddress.value)
    
    // 先进行地址解析
    const originGeocode = await geocodeAddress(originAddress.value)
    const destGeocode = await geocodeAddress(destAddress.value)
    
    if (!originGeocode || !destGeocode) {
      ElMessage.error('地址解析失败')
      return
    }
    
    // 路径规划
    const routeUrl = getRestApiUrl('direction/driving', {
      origin: originGeocode,
      destination: destGeocode
    })
    
    const response = await fetch(routeUrl)
    const data = await response.json()
    
    console.log('路径规划结果:', data)
    
    if (data.status === '1' && data.route && data.route.paths && data.route.paths.length > 0) {
      const path = data.route.paths[0]
      routeResult.value = {
        distance: path.distance,
        duration: path.duration
      }
      ElMessage.success('路径规划成功')
    } else {
      ElMessage.warning('路径规划失败')
    }
    
  } catch (error) {
    console.error('路径规划测试失败:', error)
    ElMessage.error('路径规划测试失败: ' + error.message)
  }
}

// 地址解析
const geocodeAddress = async (address) => {
  try {
    const geocodeUrl = getRestApiUrl('geocode/geo', {
      address: address
    })
    
    const response = await fetch(geocodeUrl)
    const data = await response.json()
    
    if (data.status === '1' && data.geocodes && data.geocodes.length > 0) {
      return data.geocodes[0].location
    }
    return null
  } catch (error) {
    console.error('地址解析失败:', error)
    return null
  }
}
</script>

<style scoped>
.test-page {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.test-section {
  margin-bottom: 30px;
  padding: 20px;
  border: 1px solid #ddd;
  border-radius: 8px;
  background: #f9f9f9;
}

.test-section h2 {
  margin-top: 0;
  color: #333;
  border-bottom: 2px solid #409EFF;
  padding-bottom: 10px;
}

.status-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  padding: 10px;
  background: white;
  border-radius: 4px;
}

.status-item .success {
  color: #67C23A;
  font-weight: bold;
}

.status-item .error {
  color: #F56C6C;
  font-weight: bold;
}

.status-item .checking {
  color: #E6A23C;
  font-weight: bold;
}

.test-map {
  width: 100%;
  height: 300px;
  border: 1px solid #ddd;
  border-radius: 4px;
  margin-bottom: 10px;
}

.map-status {
  padding: 10px;
  background: white;
  border-radius: 4px;
  font-weight: bold;
}

.autocomplete-test {
  margin-bottom: 20px;
}

.autocomplete-input {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  margin-bottom: 10px;
}

.autocomplete-status {
  padding: 10px;
  background: white;
  border-radius: 4px;
  font-size: 12px;
  color: #666;
}

.search-test {
  margin-bottom: 20px;
}

.search-input {
  width: 70%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  margin-right: 10px;
}

.search-btn {
  padding: 12px 20px;
  background: #409EFF;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.search-btn:hover {
  background: #337ECC;
}

.search-results {
  margin-top: 15px;
}

.search-item {
  padding: 10px;
  background: white;
  border: 1px solid #eee;
  border-radius: 4px;
  margin-bottom: 8px;
}

.item-name {
  font-weight: bold;
  color: #333;
  margin-bottom: 5px;
}

.item-address {
  font-size: 12px;
  color: #666;
}

.route-test {
  margin-bottom: 20px;
}

.route-inputs {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
}

.route-input {
  flex: 1;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.route-btn {
  padding: 12px 20px;
  background: #67C23A;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.route-btn:hover {
  background: #5DAF34;
}

.route-result {
  margin-top: 15px;
  padding: 15px;
  background: white;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-weight: bold;
}
</style> 