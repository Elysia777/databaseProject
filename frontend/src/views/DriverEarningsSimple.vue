<template>
  <div class="driver-earnings-simple">
    <h2>收入统计 (简化版)</h2>
    <p>这是一个简化的收入统计页面，用于测试路由是否正常工作。</p>
    
    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin: 20px 0;">
      <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 8px; text-align: center;">
        <div style="font-size: 24px; font-weight: bold;">¥0.00</div>
        <div>总收入</div>
      </div>
      <div style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); color: white; padding: 20px; border-radius: 8px; text-align: center;">
        <div style="font-size: 24px; font-weight: bold;">0</div>
        <div>完成订单</div>
      </div>
      <div style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); color: white; padding: 20px; border-radius: 8px; text-align: center;">
        <div style="font-size: 24px; font-weight: bold;">¥0.00</div>
        <div>平均单价</div>
      </div>
      <div style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%); color: white; padding: 20px; border-radius: 8px; text-align: center;">
        <div style="font-size: 24px; font-weight: bold;">0.0km</div>
        <div>总里程</div>
      </div>
    </div>
    
    <div style="margin-top: 20px; padding: 15px; background: #f0f9ff; border-radius: 8px;">
      <p><strong>测试状态:</strong> ✅ 简化版收入统计页面加载成功</p>
      <p><strong>说明:</strong> 如果您能看到这个页面，说明路由配置是正确的。</p>
      <p><strong>下一步:</strong> 可以逐步添加更复杂的功能和API调用。</p>
    </div>
  </div>
</template>

<script setup>
// 最简单的组件，不包含任何复杂逻辑
console.log('DriverEarningsSimple 组件已加载')
</script>

<style scoped>
.driver-earnings-simple {
  padding: 20px;
}
</style>