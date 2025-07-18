// RabbitMQ消息测试工具
const axios = require('axios');

const baseUrl = 'http://localhost:8080';

// 模拟订单通知消息
const testOrderNotifications = [
    {
        orderId: 1,
        orderNumber: "ORDER" + Date.now() + Math.random().toString(36).substr(2, 9),
        driverId: 1,
        pickupAddress: "北京市朝阳区三里屯",
        destinationAddress: "北京市海淀区中关村",
        distance: 8.5,
        estimatedFare: 25.50,
        timestamp: Date.now(),
        notificationType: "ORDER_ASSIGNED",
        message: "您有新的订单，请及时查看"
    },
    {
        orderId: 2,
        orderNumber: "ORDER" + Date.now() + Math.random().toString(36).substr(2, 9),
        driverId: 2,
        pickupAddress: "北京市东城区王府井",
        destinationAddress: "北京市西城区西单",
        distance: 3.2,
        estimatedFare: 15.80,
        timestamp: Date.now(),
        notificationType: "ORDER_ASSIGNED",
        message: "您有新的订单，请及时查看"
    }
];

// 测试司机上线功能
async function testDriverOnline(driverId, latitude, longitude) {
    try {
        console.log(`\n🚗 测试司机 ${driverId} 上线...`);
        
        const response = await axios.post(
            `${baseUrl}/api/drivers/${driverId}/online`,
            null,
            {
                params: { latitude, longitude }
            }
        );
        
        console.log(`✅ 司机 ${driverId} 上线成功:`, response.data);
        return true;
    } catch (error) {
        console.error(`❌ 司机 ${driverId} 上线失败:`, error.response?.data || error.message);
        return false;
    }
}

// 测试创建订单（这会触发RabbitMQ消息）
async function testCreateOrder(orderData) {
    try {
        console.log(`\n📋 创建测试订单...`);
        
        const response = await axios.post(`${baseUrl}/api/orders`, orderData);
        
        console.log(`✅ 订单创建成功:`, response.data);
        return response.data.data;
    } catch (error) {
        console.error(`❌ 订单创建失败:`, error.response?.data || error.message);
        return null;
    }
}

// 检查RabbitMQ队列状态（需要RabbitMQ Management API）
async function checkRabbitMQQueues() {
    try {
        console.log(`\n🐰 检查RabbitMQ队列状态...`);
        
        // 注意：这需要RabbitMQ Management插件启用
        // 默认地址是 http://localhost:15672/api/queues
        // 默认用户名密码是 guest/guest
        
        const queues = [
            'driver_notification_queue',
            'order_waiting_queue',
            'order_status_queue',
            'driver_status_queue'
        ];
        
        for (const queueName of queues) {
            try {
                const response = await axios.get(
                    `http://localhost:15672/api/queues/%2F/${queueName}`,
                    {
                        auth: {
                            username: 'guest',
                            password: 'guest'
                        }
                    }
                );
                
                console.log(`📊 队列 ${queueName}:`);
                console.log(`   消息数量: ${response.data.messages}`);
                console.log(`   消费者数量: ${response.data.consumers}`);
                
            } catch (error) {
                console.log(`⚠️  无法获取队列 ${queueName} 信息 (可能需要启用Management插件)`);
            }
        }
        
    } catch (error) {
        console.log(`⚠️  无法连接到RabbitMQ Management API`);
        console.log(`   请确保RabbitMQ Management插件已启用`);
        console.log(`   启用命令: rabbitmq-plugins enable rabbitmq_management`);
    }
}

// 主测试函数
async function runRabbitMQTest() {
    console.log('🧪 开始RabbitMQ消息处理测试...\n');
    
    // 1. 测试司机上线
    console.log('='.repeat(50));
    console.log('📍 第一步：测试司机上线');
    console.log('='.repeat(50));
    
    const drivers = [
        { id: 1, lat: 39.9042, lng: 116.4074, name: '司机张三' },
        { id: 2, lat: 39.9163, lng: 116.3972, name: '司机李四' }
    ];
    
    for (const driver of drivers) {
        await testDriverOnline(driver.id, driver.lat, driver.lng);
        await new Promise(resolve => setTimeout(resolve, 1000));
    }
    
    // 2. 检查RabbitMQ队列状态
    console.log('\n' + '='.repeat(50));
    console.log('🐰 第二步：检查RabbitMQ队列状态');
    console.log('='.repeat(50));
    
    await checkRabbitMQQueues();
    
    // 3. 创建测试订单（这会触发消息队列）
    console.log('\n' + '='.repeat(50));
    console.log('📋 第三步：创建测试订单（触发消息队列）');
    console.log('='.repeat(50));
    
    const testOrder = {
        passengerId: 1,
        pickupAddress: "北京市朝阳区三里屯",
        pickupLatitude: 39.9365,
        pickupLongitude: 116.4477,
        destinationAddress: "北京市海淀区中关村",
        destinationLatitude: 39.9851,
        destinationLongitude: 116.3058,
        orderType: "REAL_TIME"
    };
    
    const createdOrder = await testCreateOrder(testOrder);
    
    if (createdOrder) {
        console.log(`\n📨 订单创建成功，应该会触发以下消息:`);
        console.log(`   1. 发送到 order_waiting_queue`);
        console.log(`   2. 系统自动分配司机`);
        console.log(`   3. 发送通知到 driver_notification_queue`);
        console.log(`\n👀 请查看后端控制台日志，确认消息处理情况`);
    }
    
    // 4. 等待一段时间，然后再次检查队列状态
    console.log('\n⏳ 等待5秒，然后检查消息处理结果...');
    await new Promise(resolve => setTimeout(resolve, 5000));
    
    await checkRabbitMQQueues();
    
    console.log('\n✨ RabbitMQ测试完成!');
    console.log('\n💡 如果看到错误，请检查:');
    console.log('   1. RabbitMQ服务是否正在运行');
    console.log('   2. 后端应用是否正常启动');
    console.log('   3. 数据库连接是否正常');
    console.log('   4. 后端控制台是否有错误日志');
}

// 运行测试
runRabbitMQTest().catch(console.error);