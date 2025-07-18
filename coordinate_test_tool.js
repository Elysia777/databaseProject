// 经纬度测试工具
const axios = require('axios');

const baseUrl = 'http://localhost:8080';

// 测试坐标点 - 北京市著名地标
const testLocations = [
    {
        name: '天安门广场',
        latitude: 39.9042,   // 纬度 (Latitude)
        longitude: 116.4074, // 经度 (Longitude)
        description: '北京市东城区'
    },
    {
        name: '西单商业区',
        latitude: 39.9163,
        longitude: 116.3972,
        description: '北京市西城区'
    },
    {
        name: '鸟巢体育场',
        latitude: 39.9928,
        longitude: 116.3906,
        description: '北京市朝阳区'
    }
];

// 测试司机上线
async function testDriverOnline(driverId, location) {
    try {
        console.log(`\n🚗 测试司机 ${driverId} 在 ${location.name} 上线...`);
        console.log(`📍 输入坐标: 纬度=${location.latitude}, 经度=${location.longitude}`);
        
        const response = await axios.post(
            `${baseUrl}/api/drivers/${driverId}/online`,
            null,
            {
                params: {
                    latitude: location.latitude,
                    longitude: location.longitude
                }
            }
        );
        
        console.log(`✅ 上线成功:`, response.data);
        return true;
    } catch (error) {
        console.error(`❌ 上线失败:`, error.response?.data || error.message);
        return false;
    }
}

// 获取司机信息验证坐标
async function getDriverInfo(driverId) {
    try {
        const response = await axios.get(`${baseUrl}/api/drivers/${driverId}`);
        const driver = response.data.data;
        
        console.log(`📋 司机信息:`);
        console.log(`   ID: ${driver.id}`);
        console.log(`   在线状态: ${driver.isOnline ? '在线' : '离线'}`);
        console.log(`   存储坐标: 纬度=${driver.currentLatitude}, 经度=${driver.currentLongitude}`);
        
        return driver;
    } catch (error) {
        console.error(`❌ 获取司机信息失败:`, error.response?.data || error.message);
        return null;
    }
}

// 测试附近司机查询
async function testNearbyDrivers(searchLocation) {
    try {
        console.log(`\n🔍 搜索 ${searchLocation.name} 附近的司机...`);
        console.log(`📍 搜索坐标: 纬度=${searchLocation.latitude}, 经度=${searchLocation.longitude}`);
        
        const response = await axios.get(`${baseUrl}/api/drivers/nearby`, {
            params: {
                latitude: searchLocation.latitude,
                longitude: searchLocation.longitude,
                radiusKm: 10.0
            }
        });
        
        const drivers = response.data.data;
        console.log(`📊 找到 ${drivers.length} 个附近司机:`);
        
        drivers.forEach((driver, index) => {
            console.log(`   ${index + 1}. 司机ID: ${driver.id}, 坐标: (${driver.currentLatitude}, ${driver.currentLongitude})`);
        });
        
        return drivers;
    } catch (error) {
        console.error(`❌ 搜索附近司机失败:`, error.response?.data || error.message);
        return [];
    }
}

// 计算两点间距离（用于验证）
function calculateDistance(lat1, lng1, lat2, lng2) {
    const R = 6371; // 地球半径（公里）
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLng = (lng2 - lng1) * Math.PI / 180;
    const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
              Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
              Math.sin(dLng/2) * Math.sin(dLng/2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    return R * c; // 距离（公里）
}

// 主测试函数
async function runCoordinateTest() {
    console.log('🧪 开始经纬度坐标测试...\n');
    
    // 1. 测试司机上线
    for (let i = 0; i < testLocations.length; i++) {
        const location = testLocations[i];
        const driverId = i + 1;
        
        const success = await testDriverOnline(driverId, location);
        if (success) {
            // 验证存储的坐标
            await new Promise(resolve => setTimeout(resolve, 1000)); // 等待1秒
            const driver = await getDriverInfo(driverId);
            
            if (driver) {
                // 检查坐标是否正确存储
                const inputLat = location.latitude;
                const inputLng = location.longitude;
                const storedLat = parseFloat(driver.currentLatitude);
                const storedLng = parseFloat(driver.currentLongitude);
                
                const latDiff = Math.abs(inputLat - storedLat);
                const lngDiff = Math.abs(inputLng - storedLng);
                
                if (latDiff < 0.0001 && lngDiff < 0.0001) {
                    console.log(`✅ 坐标存储正确`);
                } else {
                    console.log(`⚠️  坐标存储可能有问题:`);
                    console.log(`   输入: (${inputLat}, ${inputLng})`);
                    console.log(`   存储: (${storedLat}, ${storedLng})`);
                    console.log(`   差异: 纬度差${latDiff.toFixed(6)}, 经度差${lngDiff.toFixed(6)}`);
                }
            }
        }
        
        await new Promise(resolve => setTimeout(resolve, 2000)); // 等待2秒
    }
    
    // 2. 测试附近司机查询
    console.log('\n' + '='.repeat(50));
    console.log('🔍 测试附近司机查询功能');
    console.log('='.repeat(50));
    
    for (const location of testLocations) {
        await testNearbyDrivers(location);
        await new Promise(resolve => setTimeout(resolve, 2000));
    }
    
    // 3. 距离验证
    console.log('\n' + '='.repeat(50));
    console.log('📏 距离计算验证');
    console.log('='.repeat(50));
    
    const beijing = testLocations[0]; // 天安门
    const xidan = testLocations[1];   // 西单
    
    const distance = calculateDistance(
        beijing.latitude, beijing.longitude,
        xidan.latitude, xidan.longitude
    );
    
    console.log(`📍 ${beijing.name} 到 ${xidan.name} 的直线距离: ${distance.toFixed(2)} 公里`);
    console.log(`   (实际距离约为 3-4 公里，如果计算结果差异很大，说明坐标顺序有问题)`);
    
    console.log('\n✨ 坐标测试完成!');
    console.log('\n💡 如果发现问题，请检查:');
    console.log('   1. Redis GEO 存储时的坐标顺序');
    console.log('   2. 查询时的参数顺序');
    console.log('   3. 数据库存储的坐标字段');
}

// 运行测试
runCoordinateTest().catch(console.error);