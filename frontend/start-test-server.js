#!/usr/bin/env node
/**
 * 简单的HTTP服务器，用于提供头像上传测试页面
 * 避免file://协议导致的CORS问题
 */

const http = require('http');
const fs = require('fs');
const path = require('path');
const { exec } = require('child_process');

const PORT = 3000;
const HOST = 'localhost';

// MIME类型映射
const mimeTypes = {
    '.html': 'text/html',
    '.js': 'text/javascript',
    '.css': 'text/css',
    '.json': 'application/json',
    '.png': 'image/png',
    '.jpg': 'image/jpeg',
    '.gif': 'image/gif',
    '.svg': 'image/svg+xml',
    '.ico': 'image/x-icon'
};

const server = http.createServer((req, res) => {
    // 添加CORS头
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

    if (req.method === 'OPTIONS') {
        res.writeHead(200);
        res.end();
        return;
    }

    let filePath = path.join(__dirname, req.url === '/' ? '/test-avatar-upload-server.html' : req.url);
    
    // 安全检查，防止目录遍历攻击
    if (!filePath.startsWith(__dirname)) {
        res.writeHead(403);
        res.end('Forbidden');
        return;
    }

    fs.readFile(filePath, (err, data) => {
        if (err) {
            if (err.code === 'ENOENT') {
                res.writeHead(404);
                res.end('File not found');
            } else {
                res.writeHead(500);
                res.end('Server error');
            }
            return;
        }

        const ext = path.extname(filePath);
        const contentType = mimeTypes[ext] || 'text/plain';
        
        res.writeHead(200, { 'Content-Type': contentType });
        res.end(data);
    });
});

server.listen(PORT, HOST, () => {
    console.log('🚀 测试服务器已启动');
    console.log(`📍 地址: http://${HOST}:${PORT}`);
    console.log(`🖼️ 头像上传测试: http://${HOST}:${PORT}/test-avatar-upload-server.html`);
    console.log(`🔍 头像持久化测试: http://${HOST}:${PORT}/test-avatar-persistence.html`);
    console.log(`🎯 头像显示调试: http://${HOST}:${PORT}/test-avatar-display.html`);
    console.log(`🐛 用户数据调试: http://${HOST}:${PORT}/test-user-data-debug.html`);
    console.log(`🔧 用户状态修复: http://${HOST}:${PORT}/test-user-state-fix.html`);
    console.log(`📋 评价系统测试: http://${HOST}:${PORT}/test-review-system.html`);
    console.log('⚠️  请确保后端服务已在 http://localhost:8080 启动');
    console.log('🛑 按 Ctrl+C 停止服务器');
    console.log('-'.repeat(60));

    // 自动打开浏览器（仅在Windows和macOS上）
    const platform = process.platform;
    const url = `http://${HOST}:${PORT}/test-avatar-display.html`;
    
    if (platform === 'win32') {
        exec(`start ${url}`);
    } else if (platform === 'darwin') {
        exec(`open ${url}`);
    } else {
        console.log(`🌐 请在浏览器中打开: ${url}`);
    }
});

server.on('error', (err) => {
    if (err.code === 'EADDRINUSE') {
        console.log(`❌ 端口 ${PORT} 已被占用，请关闭其他服务或修改端口号`);
    } else {
        console.log(`❌ 启动服务器失败: ${err.message}`);
    }
});

process.on('SIGINT', () => {
    console.log('\n🛑 服务器已停止');
    process.exit(0);
});