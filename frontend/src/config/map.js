// 高德地图配置
export const mapConfig = {
  // 高德地图Web端JS API密钥 - 用于地图显示、Autocomplete等
  // 注意：这个密钥必须申请Web端JS API服务
  apiKey: 'de2d5b3782511b03e23a18685faccead',
  
  // REST API密钥 - 用于搜索、路径规划等REST API调用
  // 注意：这个密钥必须申请REST API服务
  restApiKey: 'de2d5b3782511b03e23a18685faccead',
  
  // 安全密钥 - 用于Web端JS API的安全验证
  securityJsCode: '8b011259082e987043911232b625e2ca',
  
  // API版本
  version: '1.4.15',
  
  // 基础URL
  baseUrl: 'https://restapi.amap.com/v3',
  
  // 地图API URL
  mapApiUrl: 'https://webapi.amap.com/maps',
  
  // 默认插件 - 包含Autocomplete插件
  defaultPlugins: 'AMap.Geolocation,AMap.PlaceSearch,AMap.Driving,AMap.Geocoder,AMap.Autocomplete'
}

// 获取完整的API URL - 用于加载Web端JS API
export const getMapApiUrl = () => {
  return `${mapConfig.mapApiUrl}?v=${mapConfig.version}&key=${mapConfig.apiKey}&plugin=${mapConfig.defaultPlugins}`
}

// 获取REST API URL - 用于服务器端API调用
export const getRestApiUrl = (endpoint, params = {}) => {
  const url = new URL(`${mapConfig.baseUrl}/${endpoint}`)
  url.searchParams.set('key', mapConfig.restApiKey)
  url.searchParams.set('output', 'json')
  
  // 添加其他参数
  Object.keys(params).forEach(key => {
    url.searchParams.set(key, params[key])
  })
  
  return url.toString()
}

// 获取安全配置 - 用于Web端JS API安全验证
export const getSecurityConfig = () => {
  return {
    securityJsCode: mapConfig.securityJsCode
  }
} 