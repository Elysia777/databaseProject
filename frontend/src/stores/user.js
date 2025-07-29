import { defineStore } from "pinia";
import { ref, computed } from "vue";
import { userApi } from "@/api/user";

export const useUserStore = defineStore("user", () => {
  // 状态
  const user = ref(null);
  const token = ref(localStorage.getItem("token") || "");

  // 计算属性
  const isLoggedIn = computed(() => !!token.value);
  const userType = computed(() => user.value?.userType);
  const isAdmin = computed(() => userType.value === "ADMIN");
  const isDriver = computed(() => userType.value === "DRIVER");
  const isPassenger = computed(() => userType.value === "PASSENGER");

  // 初始化时如果有token但没有用户信息，尝试获取用户信息
  const initUserInfo = async () => {
    if (token.value && !user.value) {
      try {
        await getUserInfo();
      } catch (error) {
        console.error("获取用户信息失败:", error);
        // 如果获取用户信息失败，清除token
        logout();
      }
    }
  };

  // 方法
  const login = async (loginData) => {
    try {
      const response = await userApi.login(loginData);
      const { data } = response;

      user.value = data;
      token.value = data.token;
      localStorage.setItem("token", data.token);

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
      user.value = response.data;
      return response.data;
    } catch (error) {
      throw error;
    }
  };

  const updateUserInfo = async (userData) => {
    try {
      const response = await userApi.updateUserInfo(userData);
      user.value = response.data;
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
  };
});
