#!/bin/bash

set -e

SERVER_TYPE=${1:-main}  # main 或 backup
DEPLOY_DIR="/var/www/im-lanxin"
SERVICE_NAME="lanxin-im"

echo "=========================================="
echo "蓝信IM - 部署后端服务 (${SERVER_TYPE})"
echo "=========================================="

echo "[1/6] 创建部署目录..."
mkdir -p $DEPLOY_DIR
cd $DEPLOY_DIR

echo "[2/6] 获取最新代码..."
if [ -d ".git" ]; then
    git pull origin master
else
    git clone https://github.com/zhihang9978/-IM.git .
fi

echo "[3/6] 构建后端服务..."
cd apps/backend
export PATH=$PATH:/usr/local/go/bin
go mod tidy
go build -o /usr/local/bin/$SERVICE_NAME cmd/server/main.go

echo "[4/6] 创建配置文件..."
cat > /etc/lanxin-im.yaml << 'EOF'
server:
  port: 8080
  mode: release
  domain: lanxin168.com

database:
  mysql:
    host: localhost
    port: 3306
    user: lanxin_user
    password: LanXin@2024!Secure
    dbname: lanxin_im
    charset: utf8mb4
    
redis:
  addr: localhost:6379
  password: ""
  db: 0

jwt:
  secret: lanxin-jwt-secret-key-2024-production
  expiration: 7200

minio:
  endpoint: localhost:9000
  access_key: minioadmin
  secret_key: minioadmin
  bucket: lanxin-im
  use_ssl: false

trtc:
  sdk_app_id: 1600109367
  secret_key: "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgZeevqkdnxJKQyLbnjUm3YX8dNTPdzTp6gCCGl1B5V7OhRANCAATaOYqPzlXTTVBk+O4o/AQYMuBi3quZuGNAazQuICmGIzwET508Dvw09RTQFZwPiJ8VGInKDnzUotHo5sJ+FfmC"

security:
  cors:
    allowed_origins:
      - "https://app.lanxin168.com"
      - "https://admin.lanxin168.com"
    allowed_methods:
      - "GET"
      - "POST"
      - "PUT"
      - "DELETE"
    allowed_headers:
      - "Authorization"
      - "Content-Type"
  rate_limit:
    enabled: true
    requests_per_minute: 100

kafka:
  brokers:
    - "localhost:9092"
  topic:
    message: "lanxin-messages"
EOF

echo "[5/6] 创建systemd服务..."
cat > /etc/systemd/system/$SERVICE_NAME.service << EOF
[Unit]
Description=LanXin IM Backend Service
After=network.target mysql.service redis.service

[Service]
Type=simple
User=root
WorkingDirectory=$DEPLOY_DIR/apps/backend
ExecStart=/usr/local/bin/$SERVICE_NAME
Restart=always
RestartSec=10
Environment="GIN_MODE=release"
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

echo "[6/6] 启动服务..."
systemctl daemon-reload

if [ "$SERVER_TYPE" = "main" ]; then
    systemctl enable $SERVICE_NAME
    systemctl restart $SERVICE_NAME
    echo "✅ 主服务器后端服务已启动"
else
    systemctl disable $SERVICE_NAME
    systemctl stop $SERVICE_NAME 2>/dev/null || true
    echo "✅ 副服务器后端服务已部署（待机状态）"
fi

echo "=========================================="
echo "✅ 后端部署完成!"
echo "=========================================="
