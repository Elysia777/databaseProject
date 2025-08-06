#!/usr/bin/env python3
"""
简单的HTTP服务器，用于提供头像上传测试页面
避免file://协议导致的CORS问题
"""

import http.server
import socketserver
import os
import webbrowser
from pathlib import Path

# 设置端口
PORT = 3000

# 获取当前目录
current_dir = Path(__file__).parent

class CustomHTTPRequestHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(current_dir), **kwargs)
    
    def end_headers(self):
        # 添加CORS头
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type, Authorization')
        super().end_headers()

def start_server():
    try:
        with socketserver.TCPServer(("", PORT), CustomHTTPRequestHandler) as httpd:
            print(f"🚀 测试服务器已启动")
            print(f"📍 地址: http://localhost:{PORT}")
            print(f"🖼️ 头像上传测试: http://localhost:{PORT}/test-avatar-upload-server.html")
            print(f"📋 评价系统测试: http://localhost:{PORT}/test-review-system.html")
            print(f"⚠️  请确保后端服务已在 http://localhost:8080 启动")
            print(f"🛑 按 Ctrl+C 停止服务器")
            print("-" * 60)
            
            # 自动打开浏览器
            webbrowser.open(f'http://localhost:{PORT}/test-avatar-upload-server.html')
            
            httpd.serve_forever()
    except KeyboardInterrupt:
        print("\n🛑 服务器已停止")
    except OSError as e:
        if e.errno == 48:  # Address already in use
            print(f"❌ 端口 {PORT} 已被占用，请关闭其他服务或修改端口号")
        else:
            print(f"❌ 启动服务器失败: {e}")

if __name__ == "__main__":
    start_server()