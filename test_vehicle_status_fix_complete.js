// 测试车辆状态更新完整修复
// 验证车辆状态显示、更新和激活限制功能

const testVehicleStatusSystem = async () => {
    console.log('🚗 开始测试车辆状态系统完整修复...\n');
    
    const baseUrl = 'http://localhost:8080/api';
    
    try {
        // 1. 测试获取所有车辆
        console.log('1️⃣ 测试获取所有车辆...');
        const vehiclesResponse = await fetch(`${baseUrl}/vehicles/all`);
        const vehiclesResult = await vehiclesResponse.json();
        
        if (!vehiclesResult.success) {
            throw new Error('获取车辆列表失败: ' + vehiclesResult.message);
        }
        
        const vehicles = vehiclesResult.data;
        console.log(`✅ 成功获取 ${vehicles.length} 辆车`);
        
        // 2. 显示车辆状态统计
        console.log('\n2️⃣ 车辆状态统计:');
        const statusCounts = {};
        vehicles.forEach(vehicle => {
            const status = vehicle.status || 'UNKNOWN';
            statusCounts[status] = (statusCounts[status] || 0) + 1;
        });
        
        Object.entries(statusCounts).forEach(([status, count]) => {
            const statusText = getStatusText(status);
            console.log(`   ${statusText}: ${count}辆`);
        });
        
        // 3. 测试车辆状态显示
        console.log('\n3️⃣ 测试车辆状态显示:');
        vehicles.slice(0, 3).forEach((vehicle, index) => {
            console.log(`   车辆${index + 1}: ${vehicle.plateNumber}`);
            console.log(`     状态: ${vehicle.status || 'UNKNOWN'} (${getStatusText(vehicle.status)})`);
            console.log(`     激活: ${vehicle.isActive ? '是' : '否'}`);
            console.log(`     司机: ${vehicle.driverName || '未知'}`);
        });
        
        // 4. 测试车辆更新（重新进入审核状态）
        if (vehicles.length > 0) {
            const testVehicle = vehicles[0];
            console.log(`\n4️⃣ 测试车辆更新重新审核: ${testVehicle.plateNumber}`);
            console.log(`   更新前状态: ${testVehicle.status}`);
            console.log(`   更新前激活: ${testVehicle.isActive}`);
            
            const updateData = {
                brand: testVehicle.brand + ' (已更新)',
                model: testVehicle.model + ' (已更新)',
                color: testVehicle.color,
                year: testVehicle.year,
                seats: testVehicle.seats,
                vehicleType: testVehicle.vehicleType
            };
            
            const updateResponse = await fetch(`${baseUrl}/vehicles/${testVehicle.id}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(updateData)
            });
            
            if (updateResponse.ok) {
                const updateResult = await updateResponse.json();
                if (updateResult.success) {
                    console.log('   ✅ 车辆更新成功');
                    console.log(`   更新后状态: ${updateResult.data.status} (应该变为PENDING)`);
                    console.log(`   更新后激活: ${updateResult.data.isActive} (应该变为false)`);
                    
                    // 验证状态是否重新进入审核
                    if (updateResult.data.status === 'PENDING' && !updateResult.data.isActive) {
                        console.log('   ✅ 状态重新进入审核 - 修复成功！');
                    } else {
                        console.log('   ❌ 状态未重新进入审核 - 修复失败！');
                    }
                } else {
                    console.log('   ❌ 车辆更新失败:', updateResult.message);
                }
            } else {
                console.log('   ❌ 车辆更新请求失败:', updateResponse.status);
            }
        }
        
        // 5. 测试车辆激活限制
        console.log('\n5️⃣ 测试车辆激活限制:');
        const pendingVehicle = vehicles.find(v => v.status === 'PENDING');
        const activeVehicle = vehicles.find(v => v.status === 'ACTIVE');
        
        if (pendingVehicle) {
            console.log(`   测试PENDING状态车辆激活: ${pendingVehicle.plateNumber}`);
            try {
                const activateResponse = await fetch(`${baseUrl}/vehicles/driver/${pendingVehicle.driverId}/active/${pendingVehicle.id}`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    }
                });
                
                if (activateResponse.ok) {
                    const activateResult = await activateResponse.json();
                    if (!activateResult.success) {
                        console.log('   ✅ 激活被正确拒绝:', activateResult.message);
                    } else {
                        console.log('   ❌ 激活应该被拒绝但成功了');
                    }
                } else {
                    console.log('   ✅ 激活请求被拒绝:', activateResponse.status);
                }
            } catch (error) {
                console.log('   ✅ 激活请求异常:', error.message);
            }
        } else {
            console.log('   ⚠️ 没有找到PENDING状态的车辆进行测试');
        }
        
        if (activeVehicle) {
            console.log(`   测试ACTIVE状态车辆激活: ${activeVehicle.plateNumber}`);
            try {
                const activateResponse = await fetch(`${baseUrl}/vehicles/driver/${activeVehicle.driverId}/active/${activeVehicle.id}`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    }
                });
                
                if (activateResponse.ok) {
                    const activateResult = await activateResponse.json();
                    if (activateResult.success) {
                        console.log('   ✅ ACTIVE状态车辆可以正常激活');
                    } else {
                        console.log('   ❌ ACTIVE状态车辆激活失败:', activateResult.message);
                    }
                } else {
                    console.log('   ❌ ACTIVE状态车辆激活请求失败:', activateResponse.status);
                }
            } catch (error) {
                console.log('   ❌ ACTIVE状态车辆激活请求异常:', error.message);
            }
        }
        
        console.log('\n🎉 车辆状态系统测试完成！');
        
    } catch (error) {
        console.error('❌ 测试失败:', error);
    }
};

// 获取状态文本
const getStatusText = (status) => {
    const statusMap = {
        'ACTIVE': '已激活',
        'PENDING': '待审核',
        'REJECTED': '已拒绝',
        'INACTIVE': '已停用',
        'UNKNOWN': '未知状态'
    };
    return statusMap[status] || '未知状态';
};

// 运行测试
testVehicleStatusSystem();
