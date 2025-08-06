import request from './request'

export const userApi = {
  // 用户登录
  login(data) {
    return request.post('/auth/login', data)
  },

  // 用户注册
  register(data) {
    return request.post('/auth/register', data)
  },

  // 获取用户信息
  getUserInfo() {
    return request.get('/auth/user')
  },

  // 更新用户信息
  updateUserInfo(data) {
    return request.put('/auth/user', data)
  },

  // 修改密码
  changePassword(oldPassword, newPassword) {
    return request.put('/auth/password', {
      oldPassword,
      newPassword
    })
  },

  // 用户登出
  logout() {
    return request.post('/auth/logout')
  }
} 