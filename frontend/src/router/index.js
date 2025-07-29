import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/test',
    name: 'Test',
    component: () => import('@/views/Test.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/Dashboard.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'DashboardHome',
        redirect: (to) => {
          const userStore = useUserStore()
          if (userStore.isPassenger) {
            return '/dashboard/passenger-map'
          } else if (userStore.isDriver) {
            return '/dashboard/driver-map'
          }
          return '/dashboard/statistics'
        }
      },

      {
        path: 'drivers',
        name: 'Drivers',
        component: () => import('@/views/Drivers.vue')
      },
      {
        path: 'vehicles',
        name: 'Vehicles',
        component: () => import('@/views/Vehicles.vue')
      },

      {
        path: 'passenger-map',
        name: 'PassengerMap',
        component: () => import('@/views/PassengerMap.vue')
      },
      {
        path: 'driver-map',
        name: 'DriverMap',
        component: () => import('@/views/DriverMap.vue')
      },
      {
        path: 'my-trips',
        name: 'MyTrips',
        component: () => import('@/views/MyTrips.vue')
      },

      {
        path: 'complaints',
        name: 'Complaints',
        component: () => import('@/views/Complaints.vue')
      },
      {
        path: 'statistics',
        name: 'Statistics',
        component: () => import('@/views/Statistics.vue')
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  
  // 如果有token但没有用户信息，先尝试获取用户信息
  if (userStore.token && !userStore.user) {
    try {
      await userStore.initUserInfo()
    } catch (error) {
      console.error('初始化用户信息失败:', error)
      // 如果获取用户信息失败，清除登录状态
      userStore.logout()
      next('/login')
      return
    }
  }
  
  // 需要登录但未登录，跳转到登录页
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next('/login')
    return
  }
  
  // 已登录用户访问登录/注册页，跳转到仪表板
  if ((to.path === '/login' || to.path === '/register') && userStore.isLoggedIn) {
    next('/dashboard')
    return
  }
  
  // 乘客访问dashboard根路径，跳转到地图页面
  if (to.path === '/dashboard' && userStore.isPassenger) {
    next('/dashboard/passenger-map')
    return
  }
  
  // 司机访问dashboard根路径，跳转到司机地图页面
  if (to.path === '/dashboard' && userStore.isDriver) {
    next('/dashboard/driver-map')
    return
  }
  
  next()
})

export default router 