#!/bin/bash

set -e

ROLE=${1:-master}  # master 或 slave  
MASTER_IP="154.40.45.121"

echo "=========================================="
echo "Redis主从复制配置 - $ROLE"
echo "=========================================="

if [ "$ROLE" = "master" ]; then
    echo "[主节点配置]"
    
    cat >> /etc/redis/redis.conf << EOF

bind 0.0.0.0
protected-mode yes
port 6379
requirepass LanXinRedis@2024
masterauth LanXinRedis@2024
maxmemory 2gb
maxmemory-policy allkeys-lru
save 900 1
save 300 10
save 60 10000
appendonly yes
appendfilename "appendonly.aof"
EOF

    systemctl restart redis-server
    echo "✅ Redis主节点配置完成"
    
elif [ "$ROLE" = "slave" ]; then
    echo "[从节点配置]"
    
    cat >> /etc/redis/redis.conf << EOF

bind 0.0.0.0
protected-mode yes
port 6379
requirepass LanXinRedis@2024
masterauth LanXinRedis@2024
replicaof $MASTER_IP 6379
replica-read-only yes
maxmemory 2gb
maxmemory-policy allkeys-lru
save 900 1
save 300 10
save 60 10000
appendonly yes
appendfilename "appendonly.aof"
EOF

    systemctl restart redis-server
    
    redis-cli -a LanXinRedis@2024 INFO replication
    
    echo "✅ Redis从节点配置完成"
fi

echo "=========================================="
echo "配置完成!"
echo "=========================================="
