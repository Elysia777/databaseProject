import { defineStore } from "pinia";
import { ref, computed } from "vue";
import { userApi } from "@/api/user";

export const useUserStore = defineStore("user", () => {
  // 状态
  const user = ref(JSON.parse(localStorage.getItem("user") || "null"));
  const token = ref(localStorage.getItem("token") || "");

  // 计算属性
  const isLoggedIn = computed(() => !!token.value);
  const userType = computed(() => user.value?.userType);
  const isAdmin = computed(() => userType.value === "ADMIN");
  const isDriver = computed(() => userType.value === "DRIVER");
  const isPassenger = computed(() => userType.value === "PASSENGER");

  // 初始化时如果有token但没有用户信息，尝试获取用户信息
  const initUserInfo = async () => {
    console.log('🔄 初始化用户信息...')
    console.log('Token存在:', !!token.value)
    console.log('用户信息存在:', !!user.value)
    
    if (token.value && !user.value) {
      console.log('⚠️ 有token但无用户信息，尝试获取用户信息')
      try {
        await getUserInfo();
        console.log('✅ 用户信息获取成功:', user.value)
      } catch (error) {
        console.error("❌ 获取用户信息失败:", error);
        // 如果获取用户信息失败，清除token
        logout();
      }
    } else if (user.value) {
      console.log('✅ 用户信息已存在:', user.value)
    } else {
      console.log('⚠️ 无token和用户信息')
    }
  };

  // 确保用户信息完整性的方法
  const ensureUserInfo = async () => {
    console.log('🔍 检查用户信息完整性...')
    console.log('当前用户信息:', user.value)
    
    if (!user.value && token.value) {
      console.log('🔄 用户信息丢失，尝试恢复...')
      await initUserInfo()
    }
    
    if (!user.value) {
      throw new Error('用户信息不存在，请重新登录')
    }
    
    // 检查关键字段是否缺失
    const needsRefresh = (
      (user.value.userType === 'PASSENGER' && !user.value.passengerId) ||
      (user.value.userType === 'DRIVER' && !user.value.driverId)
    )
    
    if (needsRefresh) {
      console.log('⚠️ 用户信息不完整，从服务器刷新...')
      console.log('缺失字段:', {
        userType: user.value.userType,
        passengerId: user.value.passengerId,
        driverId: user.value.driverId
      })
      
      try {
        await getUserInfo() // 这会保留原有信息并更新缺失字段
        console.log('✅ 用户信息已刷新:', user.value)
      } catch (error) {
        console.error('❌ 刷新用户信息失败:', error)
        throw new Error('无法获取完整用户信息，请重新登录')
      }
    }
    
    // 最终验证
    if (user.value.userType === 'PASSENGER' && !user.value.passengerId) {
      throw new Error('乘客ID不存在，请重新登录')
    }
    
    if (user.value.userType === 'DRIVER' && !user.value.driverId) {
      throw new Error('司机ID不存在，请重新登录')
    }
    
    return user.value
  };

  // 方法
  const login = async (loginData) => {
    try {
      const response = await userApi.login(loginData);
      const { data } = response;

      user.value = data;
      token.value = data.token;
      localStorage.setItem("token", data.token);
      localStorage.setItem("user", JSON.stringify(data));

      return data;
    } catch (error) {
      throw error;
    }
  };

  const register = async (registerData) => {
    try {
      const response = await userApi.register(registerData);
      const { data } = response;

      user.value = data;
      token.value = data.token;
      localStorage.setItem("token", data.token);
      localStorage.setItem("user", JSON.stringify(data));

      return data;
    } catch (error) {
      throw error;
    }
  };

  const logout = async () => {
    try {
      // 如果是司机用户，先下线
      if (user.value && user.value.userType === 'DRIVER' && user.value.driverId) {
        try {
          console.log('🚗 司机退出登录，自动下线...');
          await fetch(`/api/drivers/${user.value.driverId}/offline`, {
            method: 'POST',
            headers: {
              'Authorization': `Bearer ${token.value}`
            }
          });
          console.log('✅ 司机已自动下线');
        } catch (error) {
          console.error('❌ 司机下线失败:', error);
        }
      }
      
      await userApi.logout();
    } catch (error) {
      console.error("登出失败:", error);
    } finally {
      user.value = null;
      token.value = "";
      localStorage.removeItem("token");
      localStorage.removeItem("user");

      // 清除订单相关的localStorage数据
      localStorage.removeItem("currentOrder");
      localStorage.removeItem("orderStatus");
      localStorage.removeItem("driverInfo");
      localStorage.removeItem("orderUserId");
      
      // 清除司机状态数据
      localStorage.removeItem("driverState");
      localStorage.removeItem("driverUserId");
      
      // 如果是司机用户，清除司机store状态
      if (user.value && user.value.userType === 'DRIVER') {
        try {
          const { useDriverStore } = await import('./driver.js');
          const driverStore = useDriverStore();
          driverStore.clearDriverState();
          driverStore.disconnectWebSocket();
          console.log('✅ 司机store状态已清除');
        } catch (error) {
          console.error('❌ 清除司机store状态失败:', error);
        }
      }
    }
  };

  const getUserInfo = async () => {
    try {
      const response = await userApi.getUserInfo();
      
      // 保留原有的重要信息（如token），只更新服务器返回的字段
      const updatedUser = {
        ...user.value, // 保留原有信息
        ...response.data, // 用服务器数据覆盖
        token: user.value?.token || token.value // 确保token不丢失
      };
      
      user.value = updatedUser;
      localStorage.setItem("user", JSON.stringify(updatedUser));
      return updatedUser;
    } catch (error) {
      throw error;
    }
  };

  const updateUserInfo = async (userData) => {
    try {
      const response = await userApi.updateUserInfo(userData);
      user.value = response.data;
      localStorage.setItem("user", JSON.stringify(response.data));
      return response.data;
    } catch (error) {
      throw error;
    }
  };

  const changePassword = async (oldPassword, newPassword) => {
    try {
      await userApi.changePassword(oldPassword, newPassword);
    } catch (error) {
      throw error;
    }
  };

  return {
    // 状态
    user,
    token,

    // 计算属性
    isLoggedIn,
    userType,
    isAdmin,
    isDriver,
    isPassenger,

    // 方法
    login,
    register,
    logout,
    getUserInfo,
    updateUserInfo,
    changePassword,
    initUserInfo,
    ensureUserInfo,
  };
});
