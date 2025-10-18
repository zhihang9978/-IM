#!/bin/bash

set -e

DEPLOY_DIR="/var/www/im-lanxin"
WEB_DIR="/var/www/lanxin-admin"

echo "=========================================="
echo "蓝信IM - 部署管理后台"
echo "=========================================="

cd $DEPLOY_DIR/apps/admin-web

echo "[1/4] 安装依赖..."
npm install

echo "[2/4] 构建生产版本..."
npm run build

echo "[3/4] 部署到Nginx..."
rm -rf $WEB_DIR
mkdir -p $WEB_DIR
cp -r dist/* $WEB_DIR/

echo "[4/4] 配置Nginx..."
cat > /etc/nginx/sites-available/lanxin-admin << 'EOF'
server {
    listen 80;
    server_name admin.lanxin168.com 154.40.45.121;
    
    root /var/www/lanxin-admin;
    index index.html;
    
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
    
    location /ws {
        proxy_pass http://localhost:8080/ws;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "Upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
EOF

ln -sf /etc/nginx/sites-available/lanxin-admin /etc/nginx/sites-enabled/
nginx -t
systemctl reload nginx

echo "=========================================="
echo "✅ 管理后台部署完成!"
echo "访问地址: http://154.40.45.121"
echo "=========================================="
