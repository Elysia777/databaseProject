// 批量司机注册测试脚本
const axios = require('axios');

const baseUrl = 'http://localhost:8080';

// 测试司机数据
const testDrivers = [
  {
    username: 'driver_test_001',
    password: '123456',
    phone: '13800138888',
    email: 'driver001@test.com',
    realName: '测试司机张三',
    idCard: '110101199001011234',
    userType: 'DRIVER',
    driverLicense: 'DL2024001234',
    drivingYears: 5,
    professionalLicense: 'PL2024001234'
  },
  {
    username: 'driver_test_002',
    password: '123456',
    phone: '13800138889',
    email: 'driver002@test.com',
    realName: '测试司机李四',
    idCard: '110101199002021234',
    userType: 'DRIVER',
    driverLicense: 'DL2024001235',
    drivingYears: 3,
    professionalLicense: 'PL2024001235'
  },
  {
    username: 'driver_test_003',
    password: '123456',
    phone: '13800138890',
    email: 'driver003@test.com',
    realName: '测试司机王五',
    idCard: '110101199003031234',
    userType: 'DRIVER',
    driverLicense: 'DL2024001236',
    drivingYears: 7,
    professionalLicense: 'PL2024001236'
  }
];

// 注册司机函数
async function registerDriver(driverData) {
  try {
    const response = await axios.post(`${baseUrl}/api/user/register`, driverData, {
      headers: {
        'Content-Type': 'application/json'
      }
    });
    
    console.log(`✅ 司机 ${driverData.realName} 注册成功:`, response.data);
    return response.data;
  } catch (error) {
    console.error(`❌ 司机 ${driverData.realName} 注册失败:`, error.response?.data || error.message);
    return null;
  }
}

// 司机上线函数
async function driverGoOnline(driverId, latitude, longitude) {
  try {
    const response = await axios.post(`${baseUrl}/api/drivers/${driverId}/online`, null, {
      params: {
        latitude: latitude,
        longitude: longitude
      }
    });
    
    console.log(`✅ 司机 ${driverId} 上线成功:`, response.data);
    return response.data;
  } catch (error) {
    console.error(`❌ 司机 ${driverId} 上线失败:`, error.response?.data || error.message);
    return null;
  }
}

// 主测试函数
async function runTest() {
  console.log('🚀 开始批量注册司机...\n');
  
  const registeredDrivers = [];
  
  // 批量注册司机
  for (const driverData of testDrivers) {
    const result = await registerDriver(driverData);
    if (result && result.success) {
      registeredDrivers.push({
        id: result.data.id,
        name: driverData.realName,
        username: driverData.username
      });
    }
    
    // 等待1秒避免请求过快
    await new Promise(resolve => setTimeout(resolve, 1000));
  }
  
  console.log(`\n📊 注册结果: ${registeredDrivers.length}/${testDrivers.length} 个司机注册成功\n`);
  
  // 测试司机上线
  if (registeredDrivers.length > 0) {
    console.log('🌐 开始测试司机上线...\n');
    
    const locations = [
      { lat: 39.9042, lng: 116.4074, name: '天安门广场' },
      { lat: 39.9163, lng: 116.3972, name: '西单商业区' },
      { lat: 39.9289, lng: 116.3883, name: '积水潭' }
    ];
    
    for (let i = 0; i < registeredDrivers.length && i < locations.length; i++) {
      const driver = registeredDrivers[i];
      const location = locations[i];
      
      console.log(`📍 司机 ${driver.name} 在 ${location.name} 上线...`);
      await driverGoOnline(driver.id, location.lat, location.lng);
      
      // 等待1秒
      await new Promise(resolve => setTimeout(resolve, 1000));
    }
  }
  
  console.log('\n✨ 测试完成!');
}

// 运行测试
runTest().catch(console.error);