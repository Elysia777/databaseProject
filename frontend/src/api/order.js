import request from './request'

export const orderApi = {
  // 创建订单
  createOrder(data) {
    return request.post('/orders/create', data)
  },

  // 获取订单列表
  getOrderList(params) {
    return request.get('/orders/list', { params })
  },

  // 获取订单详情
  getOrderDetail(id) {
    return request.get(`/orders/${id}`)
  },

  // 取消订单
  cancelOrder(id, reason) {
    return request.put(`/orders/${id}/cancel`, { reason })
  },

  // 派单
  dispatchOrder(id, driverId) {
    return request.put(`/orders/${id}/dispatch`, { driverId })
  },

  // 完成订单
  completeOrder(id) {
    return request.put(`/orders/${id}/complete`)
  }
} 