const net = require('net');

function checkRedisVersion() {
    console.log('🔍 检查 Redis 版本...');
    
    const client = new net.Socket();
    
    client.setTimeout(5000);
    
    client.connect(6379, 'localhost', () => {
        console.log('✅ 连接到 Redis 服务器');
        
        // 发送 INFO SERVER 命令获取版本信息
        client.write('INFO SERVER\r\n');
    });
    
    client.on('data', (data) => {
        const response = data.toString();
        console.log('📨 Redis 响应:');
        
        // 查找版本信息
        const lines = response.split('\r\n');
        for (const line of lines) {
            if (line.startsWith('redis_version:')) {
                console.log('🔢 ' + line);
            }
            if (line.startsWith('redis_git_sha1:')) {
                console.log('🔧 ' + line);
            }
        }
        
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

checkRedisVersion();