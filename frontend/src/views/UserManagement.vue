<template>
  <div class="user-management">
    <!-- 搜索和筛选 -->
    <el-card class="filter-card">
      <div class="filter-row">
        <div class="filter-left">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索用户名、姓名或手机号"
            style="width: 300px; margin-right: 10px;"
            clearable
            @input="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          
          <el-select
            v-model="userTypeFilter"
            placeholder="用户类型"
            style="width: 120px; margin-right: 10px;"
            @change="handleFilter"
          >
            <el-option label="全部" value="" />
            <el-option label="乘客" value="PASSENGER" />
            <el-option label="司机" value="DRIVER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
          
          <el-select
            v-model="statusFilter"
            placeholder="状态"
            style="width: 120px;"
            @change="handleFilter"
          >
            <el-option label="全部" value="" />
            <el-option label="正常" value="ACTIVE" />
            <el-option label="禁用" value="BANNED" />
          </el-select>
        </div>
        
        <div class="filter-right">
          <el-button type="primary" @click="refreshData">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </div>
    </el-card>
    
    <!-- 用户列表 -->
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>用户列表 ({{ filteredUsers.length }})</span>
        </div>
      </template>
      
      <el-table
        :data="paginatedUsers"
        style="width: 100%"
        v-loading="loading"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="真实姓名" width="120" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" width="180" />
        <el-table-column prop="userType" label="用户类型" width="100">
          <template #default="scope">
            <el-tag :type="getUserTypeColor(scope.row.userType)">
              {{ getUserTypeText(scope.row.userType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 'ACTIVE' ? 'success' : 'danger'">
              {{ scope.row.status === 'ACTIVE' ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="viewUser(scope.row)">查看</el-button>
            <el-button
              size="small"
              :type="scope.row.status === 'ACTIVE' ? 'danger' : 'success'"
              @click="toggleUserStatus(scope.row)"
            >
              {{ scope.row.status === 'ACTIVE' ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="filteredUsers.length"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
    
    <!-- 用户详情对话框 -->
    <el-dialog
      v-model="userDetailVisible"
      title="用户详情"
      width="600px"
    >
      <div v-if="selectedUser" class="user-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="用户ID">{{ selectedUser.id }}</el-descriptions-item>
          <el-descriptions-item label="用户名">{{ selectedUser.username }}</el-descriptions-item>
          <el-descriptions-item label="真实姓名">{{ selectedUser.realName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ selectedUser.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ selectedUser.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="用户类型">
            <el-tag :type="getUserTypeColor(selectedUser.userType)">
              {{ getUserTypeText(selectedUser.userType) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="selectedUser.status === 'ACTIVE' ? 'success' : 'danger'">
              {{ selectedUser.status === 'ACTIVE' ? '正常' : '禁用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ selectedUser.createdAt }}</el-descriptions-item>
        </el-descriptions>
      </div>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="userDetailVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// 响应式数据
const loading = ref(false)
const users = ref([])
const selectedUsers = ref([])
const selectedUser = ref(null)
const userDetailVisible = ref(false)

// 筛选条件
const searchKeyword = ref('')
const userTypeFilter = ref('')
const statusFilter = ref('')

// 分页
const currentPage = ref(1)
const pageSize = ref(20)

// 计算属性
const filteredUsers = computed(() => {
  let filtered = users.value
  console.log('原始用户数据:', users.value.length, users.value)
  
  // 搜索筛选
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    filtered = filtered.filter(user =>
      user.username.toLowerCase().includes(keyword) ||
      (user.realName && user.realName.toLowerCase().includes(keyword)) ||
      (user.phone && user.phone.includes(keyword))
    )
    console.log('搜索筛选后:', filtered.length)
  }
  
  // 用户类型筛选
  if (userTypeFilter.value) {
    filtered = filtered.filter(user => user.userType === userTypeFilter.value)
    console.log('类型筛选后:', filtered.length)
  }
  
  // 状态筛选
  if (statusFilter.value) {
    filtered = filtered.filter(user => user.status === statusFilter.value)
    console.log('状态筛选后:', filtered.length)
  }
  
  console.log('最终筛选结果:', filtered.length, filtered)
  return filtered
})

const paginatedUsers = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  const paginated = filteredUsers.value.slice(start, end)
  console.log('分页数据:', `第${currentPage.value}页`, `每页${pageSize.value}条`, `显示${paginated.length}条`, paginated)
  return paginated
})

// 方法
const loadUsers = async () => {
  loading.value = true
  try {
    console.log('正在请求用户数据...')
    const response = await fetch('/api/auth/users')
    console.log('响应状态:', response.status)
    
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`)
    }
    
    const data = await response.json()
    console.log('用户数据响应:', data)
    
    // 兼容两种响应格式：{success: true} 或 {code: 200}
    const isSuccess = data.success === true || data.code === 200;
    console.log('判断是否成功:', isSuccess, 'data.success:', data.success, 'data.code:', data.code);
    
    if (isSuccess) {
      users.value = data.data.map(user => ({
        ...user,
        createdAt: user.createdAt ? new Date(user.createdAt).toLocaleString() : '-'
      }))
      console.log('处理后的用户数据:', users.value)
      console.log('用户数据示例:', users.value[0])
      ElMessage.success(`成功加载 ${users.value.length} 个用户`)
    } else {
      ElMessage.error(data.message || '加载用户数据失败')
    }
  } catch (error) {
    console.error('加载用户失败:', error)
    ElMessage.error(`网络错误: ${error.message}`)
    
    // 如果是404错误，提供更具体的提示
    if (error.message.includes('404')) {
      ElMessage.warning('后端服务可能未启动，请检查后端服务是否运行在8080端口')
    }
  } finally {
    loading.value = false
  }
}

const refreshData = () => {
  loadUsers()
  ElMessage.success('数据已刷新')
}

const handleSearch = () => {
  currentPage.value = 1
}

const handleFilter = () => {
  currentPage.value = 1
}

const handleSelectionChange = (selection) => {
  selectedUsers.value = selection
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
}

const handleCurrentChange = (page) => {
  currentPage.value = page
}

const getUserTypeColor = (type) => {
  const colors = {
    'PASSENGER': '',
    'DRIVER': 'success',
    'ADMIN': 'warning'
  }
  return colors[type] || ''
}

const getUserTypeText = (type) => {
  const texts = {
    'PASSENGER': '乘客',
    'DRIVER': '司机',
    'ADMIN': '管理员'
  }
  return texts[type] || type
}

const viewUser = (user) => {
  selectedUser.value = user
  userDetailVisible.value = true
}

const toggleUserStatus = async (user) => {
  const newStatus = user.status === 'ACTIVE' ? 'BANNED' : 'ACTIVE'
  const action = newStatus === 'ACTIVE' ? '启用' : '禁用'
  
  try {
    await ElMessageBox.confirm(`确定要${action}用户 ${user.username} 吗？`, '确认操作')
    
    // 这里应该调用API更新用户状态
    // const response = await fetch(`/api/users/${user.id}/status`, {
    //   method: 'PUT',
    //   headers: { 'Content-Type': 'application/json' },
    //   body: JSON.stringify({ status: newStatus })
    // })
    
    // 模拟更新成功
    user.status = newStatus
    ElMessage.success(`用户${action}成功`)
    
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(`${action}用户失败`)
    }
  }
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
.user-management {
  padding: 20px;
}

.filter-card {
  margin-bottom: 20px;
}

.filter-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-left {
  display: flex;
  align-items: center;
}

.table-card {
  min-height: 600px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination-container {
  margin-top: 20px;
  text-align: right;
}

.user-detail {
  padding: 20px 0;
}
</style>