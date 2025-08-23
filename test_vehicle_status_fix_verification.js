// 测试车辆状态修复验证脚本
// 验证编辑后状态更新和审核功能

const testVehicleStatusFix = async () => {
    console.log('🚗 开始测试车辆状态修复验证...\n');
    
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
        
        // 3. 测试车辆编辑后状态更新
        if (vehicles.length > 0) {
            const testVehicle = vehicles[0];
            console.log(`\n3️⃣ 测试车辆编辑后状态更新: ${testVehicle.plateNumber}`);
            console.log(`   编辑前状态: ${testVehicle.status}`);
            console.log(`   编辑前激活: ${testVehicle.isActive}`);
            
            const updateData = {
                brand: testVehicle.brand + ' (已更新)',
                model: testVehicle.model + ' (已更新)',
                color: testVehicle.color,
                year: testVehicle.year,
                seats: testVehicle.seats,
                vehicleType: testVehicle.vehicleType
            };
            
            console.log('   正在更新车辆信息...');
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
                        console.log(`   期望: status=PENDING, isActive=false`);
                        console.log(`   实际: status=${updateResult.data.status}, isActive=${updateResult.data.isActive}`);
                    }
                } else {
                    console.log('   ❌ 车辆更新失败:', updateResult.message);
                }
            } else {
                console.log('   ❌ 车辆更新请求失败:', updateResponse.status);
            }
        }
        
        // 4. 测试审核功能
        console.log('\n4️⃣ 测试审核功能...');
        const pendingVehicle = vehicles.find(v => v.status === 'PENDING');
        
        if (pendingVehicle) {
            console.log(`   找到待审核车辆: ${pendingVehicle.plateNumber}`);
            console.log('   正在测试审核通过...');
            
            const approveResponse = await fetch(`${baseUrl}/vehicles/${pendingVehicle.id}/approve`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                }
            });
            
            if (approveResponse.ok) {
                const approveResult = await approveResponse.json();
                if (approveResult.success) {
                    console.log('   ✅ 审核通过成功');
                    
                    // 验证审核后的状态
                    const verifyResponse = await fetch(`${baseUrl}/vehicles/all`);
                    const verifyResult = await verifyResponse.json();
                    const updatedVehicle = verifyResult.data.find(v => v.id === pendingVehicle.id);
                    
                    if (updatedVehicle && updatedVehicle.status === 'ACTIVE') {
                        console.log('   ✅ 车辆状态已更新为ACTIVE');
                    } else {
                        console.log('   ❌ 车辆状态更新失败');
                    }
                } else {
                    console.log('   ❌ 审核通过失败:', approveResult.message);
                }
            } else {
                console.log('   ❌ 审核请求失败:', approveResponse.status);
            }
        } else {
            console.log('   ⚠️ 没有找到PENDING状态的车辆进行审核测试');
        }
        
        // 5. 测试拒绝功能
        console.log('\n5️⃣ 测试拒绝功能...');
        const activeVehicle = vehicles.find(v => v.status === 'ACTIVE');
        
        if (activeVehicle) {
            console.log(`   找到活跃车辆: ${activeVehicle.plateNumber}`);
            console.log('   正在测试拒绝...');
            
            const rejectResponse = await fetch(`${baseUrl}/vehicles/${activeVehicle.id}/reject`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                }
            });
            
            if (rejectResponse.ok) {
                const rejectResult = await rejectResponse.json();
                if (rejectResult.success) {
                    console.log('   ✅ 拒绝成功');
                    
                    // 验证拒绝后的状态
                    const verifyResponse = await fetch(`${baseUrl}/vehicles/all`);
                    const verifyResult = await verifyResponse.json();
                    const updatedVehicle = verifyResult.data.find(v => v.id === activeVehicle.id);
                    
                    if (updatedVehicle && updatedVehicle.status === 'REJECTED') {
                        console.log('   ✅ 车辆状态已更新为REJECTED');
                    } else {
                        console.log('   ❌ 车辆状态更新失败');
                    }
                } else {
                    console.log('   ❌ 拒绝失败:', rejectResult.message);
                }
            } else {
                console.log('   ❌ 拒绝请求失败:', rejectResponse.status);
            }
        } else {
            console.log('   ⚠️ 没有找到ACTIVE状态的车辆进行拒绝测试');
        }
        
        console.log('\n🎉 车辆状态修复验证测试完成！');
        
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
testVehicleStatusFix();
