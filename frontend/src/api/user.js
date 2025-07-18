import request from './request'

export const userApi = {
  // 用户登录
  login(data) {
    return request.post('/user/login', data)
  },

  // 用户注册
  register(data) {
    return request.post('/user/register', data)
  },

  // 获取用户信息
  getUserInfo() {
    return request.get('/user/info')
  },

  // 更新用户信息
  updateUserInfo(data) {
    return request.put('/user/info', data)
  },

  // 修改密码
  changePassword(oldPassword, newPassword) {
    return request.put('/user/password', null, {
      params: { oldPassword, newPassword }
    })
  },

  // 用户登出
  logout() {
    return request.post('/user/logout')
  }
} 