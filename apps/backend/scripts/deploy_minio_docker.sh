#!/bin/bash

#===============================================================================
# MinIO自建对象存储部署脚本
# 用途: 在服务器上部署S3兼容的对象存储服务
# 重要: COS使用自建MinIO，不是腾讯云！
#===============================================================================

echo "开始部署MinIO自建对象存储..."

# 创建数据目录
mkdir -p /data/minio

# 部署MinIO
docker run -d \
  --name minio \
  -p 9000:9000 \
  -p 9001:9001 \
  -e "MINIO_ROOT_USER=minioadmin" \
  -e "MINIO_ROOT_PASSWORD=minioadmin123456" \
  -v /data/minio:/data \
  --restart=always \
  minio/minio server /data --console-address ":9001"

echo "等待MinIO启动（10秒）..."
sleep 10

# 验证MinIO是否运行
if docker ps | grep -q minio; then
  echo "✅ MinIO容器运行中"
else
  echo "❌ MinIO启动失败"
  exit 1
fi

echo ""
echo "═══════════════════════════════════════════════════════════"
echo "MinIO部署成功！"
echo "═══════════════════════════════════════════════════════════"
echo ""
echo "访问地址:"
echo "  控制台: http://localhost:9001"
echo "  API:    http://localhost:9000"
echo ""
echo "登录信息:"
echo "  用户名: minioadmin"
echo "  密码:   minioadmin123456"
echo ""
echo "下一步操作:"
echo "  1. 访问 http://YOUR_SERVER_IP:9001"
echo "  2. 使用上述账号登录"
echo "  3. 创建Bucket: lanxin-files"
echo "  4. 设置访问策略为Private"
echo "  5. 创建AccessKey（在Access Keys页面）"
echo "  6. 更新后端配置文件 config.yaml:"
echo "     storage:"
echo "       cos:"
echo "         secret_id: \"YOUR_ACCESS_KEY\""
echo "         secret_key: \"YOUR_SECRET_KEY\""
echo "         base_url: \"http://YOUR_SERVER_IP:9000\""
echo "  7. 重启后端服务: systemctl restart lanxin"
echo ""
echo "═══════════════════════════════════════════════════════════"

