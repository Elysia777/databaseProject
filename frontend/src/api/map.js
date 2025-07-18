import request from './request'

export const mapApi = {
  // 搜索附近的地点
  searchNearbyPlaces(params) {
    return request.get('/map/search/nearby', { params })
  },

  // 地理编码（地址转坐标）
  geocode(params) {
    return request.get('/map/geocode', { params })
  },

  // 逆地理编码（坐标转地址）
  reverseGeocode(params) {
    return request.get('/map/reverse-geocode', { params })
  },

  // 路径规划
  routePlanning(params) {
    return request.get('/map/route', { params })
  }
} 