<template>
  <div class="dashboard">
    <!-- 侧边栏 -->
    <el-aside width="250px" class="sidebar">
      <div class="logo">
        <h2>网约车运营系统</h2>
      </div>
      
      <el-menu
        :default-active="$route.path"
        class="sidebar-menu"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <!-- 管理员菜单 -->
        <template v-if="userStore.isAdmin">
        <el-menu-item index="/dashboard/overview">
          <el-icon><DataBoard /></el-icon>
          <span>概览</span>
        </el-menu-item>
        
        <el-menu-item index="/dashboard/orders">
          <el-icon><Document /></el-icon>
          <span>订单管理</span>
        </el-menu-item>
        
        <el-menu-item index="/dashboard/drivers">
          <el-icon><User /></el-icon>
          <span>司机管理</span>
        </el-menu-item>
        
        <el-menu-item index="/dashboard/vehicles">
          <el-icon><Van /></el-icon>
          <span>车辆管理</span>
        </el-menu-item>
        
        <el-menu-item index="/dashboard/passengers">
          <el-icon><Avatar /></el-icon>
          <span>乘客管理</span>
        </el-menu-item>
        
        <el-menu-item index="/dashboard/ratings">
          <el-icon><Star /></el-icon>
          <span>评价管理</span>
        </el-menu-item>
        
        <el-menu-item index="/dashboard/complaints">
          <el-icon><Warning /></el-icon>
          <span>投诉处理</span>
        </el-menu-item>
        
        <el-menu-item index="/dashboard/statistics">
          <el-icon><TrendCharts /></el-icon>
          <span>数据统计</span>
        </el-menu-item>
        </template>
        
        <!-- 乘客菜单 -->
        <template v-else-if="userStore.isPassenger">
          <el-menu-item index="/dashboard/passenger-map">
            <el-icon><Location /></el-icon>
            <span>叫车</span>
          </el-menu-item>
          
          <el-menu-item index="/dashboard/overview">
            <el-icon><DataBoard /></el-icon>
            <span>我的行程</span>
          </el-menu-item>
          
          <el-menu-item index="/dashboard/orders">
            <el-icon><Document /></el-icon>
            <span>订单记录</span>
          </el-menu-item>
          
          <el-menu-item index="/dashboard/passengers">
            <el-icon><Avatar /></el-icon>
            <span>行程历史</span>
          </el-menu-item>
          
          <el-menu-item index="/dashboard/ratings">
            <el-icon><Star /></el-icon>
            <span>我的钱包</span>
          </el-menu-item>
        </template>
        
        <!-- 司机菜单 -->
        <template v-else-if="userStore.isDriver">
          <el-menu-item index="/dashboard/overview">
            <el-icon><DataBoard /></el-icon>
            <span>工作台</span>
          </el-menu-item>
          
          <el-menu-item index="/dashboard/orders">
            <el-icon><Document /></el-icon>
            <span>我的订单</span>
          </el-menu-item>
          
          <el-menu-item index="/dashboard/drivers">
            <el-icon><User /></el-icon>
            <span>收入统计</span>
          </el-menu-item>
        </template>
        
        <!-- 通用菜单 -->
        <el-menu-item index="/dashboard/profile">
          <el-icon><Setting /></el-icon>
          <span>个人设置</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    
    <!-- 主内容区 -->
    <el-container class="main-container">
      <!-- 顶部导航 -->
      <el-header class="header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>{{ getUserTypeText() }}</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentPageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" :src="userStore.user?.avatar">
                {{ userStore.user?.realName?.charAt(0) }}
              </el-avatar>
              <span class="username">{{ userStore.user?.realName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人设置</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      
      <!-- 内容区 -->
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 页面标题映射
const pageTitleMap = computed(() => {
  if (userStore.isAdmin) {
    return {
  '/dashboard/overview': '概览',
  '/dashboard/orders': '订单管理',
  '/dashboard/drivers': '司机管理',
  '/dashboard/vehicles': '车辆管理',
  '/dashboard/passengers': '乘客管理',
  '/dashboard/ratings': '评价管理',
  '/dashboard/complaints': '投诉处理',
  '/dashboard/statistics': '数据统计',
  '/dashboard/profile': '个人设置'
}
  } else if (userStore.isPassenger) {
    return {
      '/dashboard/passenger-map': '叫车',
      '/dashboard/overview': '我的行程',
      '/dashboard/orders': '订单记录',
      '/dashboard/passengers': '行程历史',
      '/dashboard/ratings': '我的钱包',
      '/dashboard/profile': '个人设置'
    }
  } else if (userStore.isDriver) {
    return {
      '/dashboard/overview': '工作台',
      '/dashboard/orders': '我的订单',
      '/dashboard/drivers': '收入统计',
      '/dashboard/profile': '个人设置'
    }
  }
  return {}
})

const currentPageTitle = computed(() => {
  return pageTitleMap.value[route.path] || '首页'
})

// 获取用户类型文本
const getUserTypeText = () => {
  if (userStore.isAdmin) {
    return '管理后台'
  } else if (userStore.isPassenger) {
    return '乘客端'
  } else if (userStore.isDriver) {
    return '司机端'
  }
  return '首页'
}

const handleCommand = async (command) => {
  if (command === 'profile') {
    router.push('/dashboard/profile')
  } else if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await userStore.logout()
      router.push('/login')
    } catch (error) {
      // 用户取消
    }
  }
}
</script>

<style scoped>
.dashboard {
  display: flex;
  height: 100vh;
}

.sidebar {
  background-color: #304156;
  color: white;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid #435266;
}

.logo h2 {
  color: white;
  font-size: 18px;
  margin: 0;
}

.sidebar-menu {
  border: none;
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.header {
  background: white;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.header-left {
  flex: 1;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.user-info:hover {
  background-color: #f5f5f5;
}

.username {
  margin: 0 8px;
  color: #333;
}

.main-content {
  background-color: #f5f5f5;
  padding: 20px;
  overflow-y: auto;
}
</style> 