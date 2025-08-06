<template>
  <div class="driver-orders-simple">
    <h2>我的订单 (简化版)</h2>
    <p>这是一个简化的订单页面，用于测试路由是否正常工作。</p>
    
    <div style="margin: 20px 0;">
      <div style="border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px; margin-bottom: 16px; background: white;">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
          <div style="display: flex; align-items: center; gap: 8px;">
            <span style="font-weight: 600; font-size: 16px;">#ORD20250805001</span>
            <span style="background: #67c23a; color: white; padding: 2px 8px; border-radius: 4px; font-size: 12px;">已完成</span>
          </div>
          <span style="color: #666; font-size: 14px;">2025-08-05 10:30</span>
        </div>
        
        <div style="display: flex; gap: 20px;">
          <div style="flex: 1;">
            <div style="margin-bottom: 8px;">
              <span style="color: #67c23a;">📍</span>
              <span style="margin-left: 5px;">北京市朝阳区三里屯SOHO</span>
            </div>
            <div>
              <span style="color: #e6a23c;">🎯</span>
              <span style="margin-left: 5px;">北京市海淀区中关村软件园</span>
            </div>
          </div>
          <div style="text-align: right;">
            <div style="color: #f56c6c; font-weight: 600; font-size: 16px;">¥45.80</div>
            <div style="color: #666; font-size: 12px;">12.5km</div>
          </div>
        </div>
      </div>
      
      <div style="border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px; background: white;">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
          <div style="display: flex; align-items: center; gap: 8px;">
            <span style="font-weight: 600; font-size: 16px;">#ORD20250805002</span>
            <span style="background: #e6a23c; color: white; padding: 2px 8px; border-radius: 4px; font-size: 12px;">预约单</span>
            <span style="background: #67c23a; color: white; padding: 2px 8px; border-radius: 4px; font-size: 12px;">已完成</span>
          </div>
          <span style="color: #666; font-size: 14px;">2025-08-05 14:15</span>
        </div>
        
        <div style="display: flex; gap: 20px;">
          <div style="flex: 1;">
            <div style="margin-bottom: 8px;">
              <span style="color: #67c23a;">📍</span>
              <span style="margin-left: 5px;">北京市东城区王府井大街</span>
            </div>
            <div>
              <span style="color: #e6a23c;">🎯</span>
              <span style="margin-left: 5px;">北京首都国际机场T3航站楼</span>
            </div>
          </div>
          <div style="text-align: right;">
            <div style="color: #f56c6c; font-weight: 600; font-size: 16px;">¥89.50</div>
            <div style="color: #666; font-size: 12px;">28.3km</div>
          </div>
        </div>
      </div>
    </div>
    
    <div style="margin-top: 20px; padding: 15px; background: #f0f9ff; border-radius: 8px;">
      <p><strong>测试状态:</strong> ✅ 简化版订单页面加载成功</p>
      <p><strong>说明:</strong> 如果您能看到这个页面，说明路由配置是正确的。</p>
      <p><strong>下一步:</strong> 可以逐步添加更复杂的功能和API调用。</p>
    </div>
  </div>
</template>

<script setup>
// 最简单的组件，不包含任何复杂逻辑
console.log('DriverOrdersSimple 组件已加载')
</script>

<style scoped>
.driver-orders-simple {
  padding: 20px;
}
</style>