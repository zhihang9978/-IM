#!/bin/bash

set -e

echo "=========================================="
echo "蓝信IM - 安装服务器依赖"
echo "=========================================="

echo "[1/8] 更新系统包..."
apt update && apt upgrade -y

echo "[2/8] 安装基础工具..."
apt install -y curl wget git vim htop net-tools ufw software-properties-common

echo "[3/8] 安装Go 1.21..."
if ! command -v go &> /dev/null; then
    wget https://go.dev/dl/go1.21.0.linux-amd64.tar.gz
    rm -rf /usr/local/go
    tar -C /usr/local -xzf go1.21.0.linux-amd64.tar.gz
    echo 'export PATH=$PATH:/usr/local/go/bin' >> /etc/profile
    source /etc/profile
    rm go1.21.0.linux-amd64.tar.gz
fi
go version

echo "[4/8] 安装MySQL 8.0..."
if ! command -v mysql &> /dev/null; then
    apt install -y mysql-server mysql-client
    systemctl start mysql
    systemctl enable mysql
fi
mysql --version

echo "[5/8] 安装Redis 7.0..."
if ! command -v redis-server &> /dev/null; then
    curl -fsSL https://packages.redis.io/gpg | gpg --dearmor -o /usr/share/keyrings/redis-archive-keyring.gpg
    echo "deb [signed-by=/usr/share/keyrings/redis-archive-keyring.gpg] https://packages.redis.io/deb $(lsb_release -cs) main" | tee /etc/apt/sources.list.d/redis.list
    apt update
    apt install -y redis
    systemctl start redis-server
    systemctl enable redis-server
fi
redis-server --version

echo "[6/8] 安装MinIO..."
if ! command -v minio &> /dev/null; then
    wget https://dl.min.io/server/minio/release/linux-amd64/minio
    chmod +x minio
    mv minio /usr/local/bin/
fi
minio --version

echo "[7/8] 安装Nginx..."
if ! command -v nginx &> /dev/null; then
    apt install -y nginx
    systemctl start nginx
    systemctl enable nginx
fi
nginx -v

echo "[8/8] 安装Node.js..."
if ! command -v node &> /dev/null; then
    curl -fsSL https://deb.nodesource.com/setup_18.x | bash -
    apt install -y nodejs
fi
node -v
npm -v

echo "=========================================="
echo "✅ 所有依赖安装完成!"
echo "=========================================="
