const net = require('net');

// 测试 Redis 连接
function testRedisConnection() {
    console.log('🔍 测试 Redis 连接...');
    
    const client = new net.Socket();
    
    client.setTimeout(5000);
    
    client.connect(6379, 'localhost', () => {
        console.log('✅ 成功连接到 Redis 服务器');
        
        // 发送 PING 命令
        client.write('PING\r\n');
    });
    
    client.on('data', (data) => {
        console.log('📨 Redis 响应:', data.toString());
        client.destroy();
    });
    
    client.on('timeout', () => {
        console.error('❌ Redis 连接超时');
        client.destroy();
    });
    
    client.on('error', (err) => {
        console.error('❌ Redis 连接错误:', err.message);
    });
    
    client.on('close', () => {
        console.log('🔌 Redis 连接已关闭');
    });
}

testRedisConnection();