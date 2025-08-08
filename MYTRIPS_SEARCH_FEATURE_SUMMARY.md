# 乘客端"我的订单"搜索功能实现总结

## 功能概述

为乘客端的"我的订单"页面（MyTrips.vue）添加了完整的搜索和筛选功能，让用户能够快速找到所需的订单信息。

## 新增功能

### 1. 筛选功能
- **订单状态筛选**：预约中、等待接单、司机已接单、司机已到达、行程中、已完成、已取消
- **支付状态筛选**：已支付、未支付、已退款
- **时间范围筛选**：支持日期范围选择
- **关键词搜索**：支持搜索订单号、上车地址、目的地地址

### 2. 快速筛选标签
- 全部订单
- 待支付订单
- 已完成订单
- 已取消订单
- 今日订单
- 本周订单

### 3. 排序功能
- 时间倒序（默认）
- 时间正序
- 费用高到低
- 费用低到高

### 4. 分页功能
- 支持每页显示5、10、20、50条记录
- 完整的分页导航
- 显示总记录数和当前页信息

## 技术实现

### 1. 响应式数据
```javascript
// 筛选相关
const showFilters = ref(false)
const filters = ref({
  status: '',
  paymentStatus: '',
  dateRange: null,
  keyword: ''
})
const activeQuickFilter = ref('')
const sortBy = ref('time_desc')

// 分页相关
const currentPage = ref(1)
const pageSize = ref(10)
```

### 2. 计算属性
```javascript
// 筛选后的订单
const filteredOrders = computed(() => {
  // 状态筛选、支付状态筛选、时间范围筛选、关键词搜索、排序
})

// 分页后的订单
const paginatedOrders = computed(() => {
  // 分页逻辑
})
```

### 3. 核心方法
- `applyFilters()` - 应用筛选条件
- `resetFilters()` - 重置筛选条件
- `applyQuickFilter()` - 应用快速筛选
- `debounceSearch()` - 防抖搜索
- `handleSizeChange()` - 分页大小变更
- `handleCurrentChange()` - 页码变更

## 用户界面

### 1. 筛选区域
- 可折叠的筛选面板
- 直观的筛选条件表单
- 快速筛选标签
- 搜索和重置按钮

### 2. 订单列表头部
- 显示筛选结果统计
- 排序选择器
- 筛选状态提示

### 3. 分页组件
- 完整的分页控件
- 每页记录数选择
- 总记录数显示

## 搜索逻辑

### 1. 状态筛选
```javascript
if (filters.value.status) {
  result = result.filter(order => order.status === filters.value.status)
}
```

### 2. 支付状态筛选
```javascript
if (filters.value.paymentStatus === 'UNPAID') {
  result = result.filter(order => isUnpaid(order))
} else {
  result = result.filter(order => order.paymentStatus === filters.value.paymentStatus)
}
```

### 3. 时间范围筛选
```javascript
if (filters.value.dateRange && filters.value.dateRange.length === 2) {
  const [startDate, endDate] = filters.value.dateRange
  result = result.filter(order => {
    const orderDate = new Date(order.createdAt).toISOString().split('T')[0]
    return orderDate >= startDate && orderDate <= endDate
  })
}
```

### 4. 关键词搜索
```javascript
if (filters.value.keyword && filters.value.keyword.trim()) {
  const keyword = filters.value.keyword.trim().toLowerCase()
  result = result.filter(order => 
    order.orderNumber?.toLowerCase().includes(keyword) ||
    order.pickupAddress?.toLowerCase().includes(keyword) ||
    order.destinationAddress?.toLowerCase().includes(keyword)
  )
}
```

## 性能优化

### 1. 防抖搜索
- 关键词输入使用500ms防抖
- 避免频繁的筛选操作

### 2. 计算属性缓存
- 使用Vue的计算属性自动缓存
- 只在依赖数据变化时重新计算

### 3. 分页显示
- 只渲染当前页的订单
- 减少DOM节点数量

## 响应式设计

### 1. 移动端适配
- 筛选表单在移动端变为垂直布局
- 订单列表头部在移动端变为垂直布局
- 快速筛选标签在移动端变为垂直布局

### 2. 样式优化
- 使用Flexbox布局
- 适当的间距和字体大小
- 良好的视觉层次

## 测试验证

### 1. 功能测试
使用 `frontend/test-mytrips-search.html` 进行功能测试：
- 状态筛选测试
- 支付状态筛选测试
- 关键词搜索测试
- 快速筛选测试
- 分页功能测试

### 2. 用户体验测试
- 筛选响应速度
- 搜索结果准确性
- 界面交互流畅性
- 移动端适配效果

## 使用说明

### 1. 基本搜索
1. 点击"展开筛选"按钮
2. 选择筛选条件
3. 点击"搜索"按钮

### 2. 快速筛选
- 直接点击快速筛选标签
- 自动应用对应的筛选条件

### 3. 关键词搜索
- 在关键词输入框中输入搜索内容
- 支持搜索订单号、地址信息
- 自动防抖搜索

### 4. 排序和分页
- 使用排序下拉框选择排序方式
- 使用分页组件浏览不同页面
- 可调整每页显示数量

## 扩展性

### 1. 新增筛选条件
- 在filters对象中添加新字段
- 在filteredOrders计算属性中添加筛选逻辑
- 在UI中添加对应的表单控件

### 2. 新增快速筛选
- 在quickFilters数组中添加新选项
- 在applyQuickFilter方法中添加对应逻辑

### 3. 新增排序方式
- 在排序下拉框中添加新选项
- 在filteredOrders计算属性的排序逻辑中添加新case

这个搜索功能大大提升了用户查找订单的效率，特别是对于有大量历史订单的用户来说非常有用。