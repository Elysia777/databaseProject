// 修复Vue语言服务器的脚本
console.log('开始修复Vue语言服务器问题...')

// 1. 清理可能的缓存文件
const fs = require('fs')
const path = require('path')

const cacheDirs = [
  'node_modules/.cache',
  'frontend/node_modules/.cache',
  '.vscode/.cache'
]

cacheDirs.forEach(dir => {
  if (fs.existsSync(dir)) {
    console.log(`清理缓存目录: ${dir}`)
    try {
      fs.rmSync(dir, { recursive: true, force: true })
    } catch (e) {
      console.log(`清理 ${dir} 失败:`, e.message)
    }
  }
})

console.log('缓存清理完成')
console.log('请重启Kiro IDE以应用修复')