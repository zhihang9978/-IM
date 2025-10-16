#!/bin/bash

#===============================================================================
# 后端服务健康检查脚本
# 用途: Keepalived调用此脚本判断后端服务是否健康
# 返回: 0=健康, 1=异常
#===============================================================================

# 检查后端服务健康端点
HEALTH_URL="http://localhost:8080/health"
TIMEOUT=3

# 方法1: 使用curl检查HTTP健康端点
response=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout ${TIMEOUT} ${HEALTH_URL})

if [ "$response" = "200" ]; then
  # 进一步验证返回内容
  content=$(curl -s --connect-timeout ${TIMEOUT} ${HEALTH_URL})
  if echo "$content" | grep -q '"status":"ok"'; then
    # 健康检查通过
    exit 0
  fi
fi

# 方法2: 检查进程是否存在（备用）
if pgrep -f "lanxin-server" > /dev/null; then
  # 进程存在，但健康端点失败，可能服务有问题
  exit 1
fi

# 所有检查都失败
exit 1

