#!/bin/bash

echo "正在停止旧的开发服务器..."
pkill -f "vite" 2>/dev/null
pkill -f "pnpm dev" 2>/dev/null
sleep 2

echo "正在拉取最新代码..."
git pull origin devin/1760621402-deployment-setup

echo "正在安装依赖..."
pnpm install

echo "正在清理缓存..."
rm -rf node_modules/.vite
rm -rf dist

echo "正在启动开发服务器..."
pnpm dev

echo ""
echo "=========================================="
echo "开发服务器已启动！"
echo "请访问: http://localhost:3000/login"
echo "测试账号: testadmin"
echo "测试密码: Test@123456"
echo "=========================================="
