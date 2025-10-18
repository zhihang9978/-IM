#!/bin/bash

set -e

SERVER_IP=${1:-""}

if [ -z "$SERVER_IP" ]; then
    echo "使用方法: $0 <server_ip>"
    exit 1
fi

echo "=========================================="
echo "清理服务器: $SERVER_IP"
echo "=========================================="

echo "[1/5] 停止服务..."
systemctl stop lanxin-im 2>/dev/null || true
systemctl stop lanxin-new 2>/dev/null || true
systemctl stop minio 2>/dev/null || true
systemctl stop nginx 2>/dev/null || true
systemctl stop mysql 2>/dev/null || true
systemctl stop redis-server 2>/dev/null || true
systemctl stop lanxin-monitor 2>/dev/null || true

echo "[2/5] 删除服务文件..."
rm -f /etc/systemd/system/lanxin-im.service
rm -f /etc/systemd/system/lanxin-new.service
rm -f /etc/systemd/system/minio.service
rm -f /etc/systemd/system/lanxin-monitor.service
systemctl daemon-reload

echo "[3/5] 删除部署目录..."
rm -rf /var/www/im-lanxin
rm -rf /var/www/lanxin-admin
rm -rf /opt/lanxin-monitor

echo "[4/5] 删除配置文件..."
rm -f /etc/lanxin-im.yaml
rm -f /usr/local/bin/lanxin-im

echo "[5/5] 清理数据目录..."
rm -rf /data/minio

if command -v mysql &> /dev/null; then
    echo "发现MySQL，删除数据库..."
    mysql -u root -e "DROP DATABASE IF EXISTS lanxin_im;" 2>/dev/null || true
    mysql -u root -e "DROP USER IF EXISTS 'lanxin_user'@'localhost';" 2>/dev/null || true
    mysql -u root -e "DROP USER IF EXISTS 'lanxin_user'@'%';" 2>/dev/null || true
    mysql -u root -e "DROP USER IF EXISTS 'repl_user'@'154.40.45.98';" 2>/dev/null || true
    mysql -u root -e "DROP USER IF EXISTS 'repl_user'@'154.40.45.121';" 2>/dev/null || true
    mysql -u root -e "FLUSH PRIVILEGES;" 2>/dev/null || true
fi

if command -v redis-cli &> /dev/null; then
    echo "发现Redis，清理数据..."
    redis-cli FLUSHALL 2>/dev/null || true
fi

rm -f /etc/nginx/sites-enabled/lanxin-admin
rm -f /etc/nginx/sites-available/lanxin-admin

rm -f /var/log/lanxin-monitor.log

echo "=========================================="
echo "✅ 服务器清理完成!"
echo "=========================================="
