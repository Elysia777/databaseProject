// 坐标验证工具
console.log('🧭 坐标验证工具\n');

// 北京地区的坐标范围
const BEIJING_BOUNDS = {
    latitude: { min: 39.4, max: 41.0, name: '纬度' },
    longitude: { min: 115.4, max: 117.5, name: '经度' }
};

// 常见的北京地标坐标（正确的）
const BEIJING_LANDMARKS = [
    { name: '天安门广场', lat: 39.9042, lng: 116.4074 },
    { name: '西单商业区', lat: 39.9163, lng: 116.3972 },
    { name: '鸟巢体育场', lat: 39.9928, lng: 116.3906 },
    { name: '颐和园', lat: 39.9999, lng: 116.2750 },
    { name: '北京大学', lat: 39.9990, lng: 116.3059 }
];

function validateCoordinate(lat, lng, name = '坐标点') {
    console.log(`📍 验证 ${name}:`);
    console.log(`   输入: 纬度=${lat}, 经度=${lng}`);
    
    let isValid = true;
    let suggestions = [];
    
    // 检查纬度范围
    if (lat < BEIJING_BOUNDS.latitude.min || lat > BEIJING_BOUNDS.latitude.max) {
        console.log(`   ❌ 纬度 ${lat} 超出北京范围 (${BEIJING_BOUNDS.latitude.min}-${BEIJING_BOUNDS.latitude.max})`);
        isValid = false;
        
        // 检查是否经纬度颠倒
        if (lng >= BEIJING_BOUNDS.latitude.min && lng <= BEIJING_BOUNDS.latitude.max) {
            suggestions.push(`可能经纬度颠倒了，试试: 纬度=${lng}, 经度=${lat}`);
        }
    } else {
        console.log(`   ✅ 纬度 ${lat} 在北京范围内`);
    }
    
    // 检查经度范围
    if (lng < BEIJING_BOUNDS.longitude.min || lng > BEIJING_BOUNDS.longitude.max) {
        console.log(`   ❌ 经度 ${lng} 超出北京范围 (${BEIJING_BOUNDS.longitude.min}-${BEIJING_BOUNDS.longitude.max})`);
        isValid = false;
        
        // 检查是否经纬度颠倒
        if (lat >= BEIJING_BOUNDS.longitude.min && lat <= BEIJING_BOUNDS.longitude.max) {
            suggestions.push(`可能经纬度颠倒了，试试: 纬度=${lng}, 经度=${lat}`);
        }
    } else {
        console.log(`   ✅ 经度 ${lng} 在北京范围内`);
    }
    
    if (suggestions.length > 0) {
        console.log(`   💡 建议: ${suggestions[0]}`);
    }
    
    if (isValid) {
        console.log(`   🎯 ${name} 坐标有效！`);
    }
    
    console.log('');
    return isValid;
}

function findNearestLandmark(lat, lng) {
    let nearest = null;
    let minDistance = Infinity;
    
    for (const landmark of BEIJING_LANDMARKS) {
        const distance = calculateDistance(lat, lng, landmark.lat, landmark.lng);
        if (distance < minDistance) {
            minDistance = distance;
            nearest = landmark;
        }
    }
    
    return { landmark: nearest, distance: minDistance };
}

function calculateDistance(lat1, lng1, lat2, lng2) {
    const R = 6371; // 地球半径（公里）
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLng = (lng2 - lng1) * Math.PI / 180;
    const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
              Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
              Math.sin(dLng/2) * Math.sin(dLng/2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    return R * c;
}

// 测试你的错误输入
console.log('🔍 分析你的输入数据:\n');

const yourInput = {
    latitude: 116.4540,
    longitude: 39.9340
};

console.log('你的输入:');
validateCoordinate(yourInput.latitude, yourInput.longitude, '你的坐标');

console.log('正确的输入应该是:');
const correctedInput = {
    latitude: 39.9340,
    longitude: 116.4540
};
validateCoordinate(correctedInput.latitude, correctedInput.longitude, '修正后的坐标');

// 找到最近的地标
const nearest = findNearestLandmark(correctedInput.latitude, correctedInput.longitude);
console.log(`📍 最近的地标: ${nearest.landmark.name} (距离 ${nearest.distance.toFixed(2)} 公里)`);

console.log('\n' + '='.repeat(50));
console.log('📚 北京常用坐标参考:');
console.log('='.repeat(50));

BEIJING_LANDMARKS.forEach(landmark => {
    console.log(`${landmark.name}: 纬度=${landmark.lat}, 经度=${landmark.lng}`);
});

console.log('\n💡 记住口诀:');
console.log('   📏 纬度 (Latitude) = 南北方向 = 北京约39-41');
console.log('   📐 经度 (Longitude) = 东西方向 = 北京约115-117');
console.log('   🌍 北京坐标格式: (纬度39.xx, 经度116.xx)');