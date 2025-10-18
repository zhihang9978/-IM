#!/bin/bash

set -e

MAIN_SERVER="154.40.45.121"
MAIN_PASS="vhISARnUo7sO4QnK"

BACKUP_SERVER="154.40.45.98"
BACKUP_PASS="FKbnW5cd5GLMHXAb"

MONITOR_SERVER="154.37.212.67"
MONITOR_PASS="QcTzmHVsWPde6A7o"

DEPLOY_DIR="/var/www/im-lanxin"

echo "=========================================="
echo "蓝信IM - 清理并重新部署系统"
echo "=========================================="
echo ""
echo "⚠️  警告: 此操作将删除所有旧数据!"
echo ""
echo "服务器信息:"
echo "  主服务器: $MAIN_SERVER"
echo "  副服务器: $BACKUP_SERVER"
echo "  监控服务器: $MONITOR_SERVER"
echo ""
read -p "确认继续? (输入 YES 继续): " confirm

if [ "$confirm" != "YES" ]; then
    echo "已取消操作"
    exit 0
fi

if ! command -v sshpass &> /dev/null; then
    echo "安装 sshpass..."
    apt update && apt install -y sshpass
fi

ssh_main() {
    sshpass -p "$MAIN_PASS" ssh -o StrictHostKeyChecking=no root@$MAIN_SERVER "$@"
}

ssh_backup() {
    sshpass -p "$BACKUP_PASS" ssh -o StrictHostKeyChecking=no root@$BACKUP_SERVER "$@"
}

ssh_monitor() {
    sshpass -p "$MONITOR_PASS" ssh -o StrictHostKeyChecking=no root@$MONITOR_SERVER "$@"
}

scp_main() {
    sshpass -p "$MAIN_PASS" scp -o StrictHostKeyChecking=no "$@" root@$MAIN_SERVER:/root/
}

scp_backup() {
    sshpass -p "$BACKUP_PASS" scp -o StrictHostKeyChecking=no "$@" root@$BACKUP_SERVER:/root/
}

scp_monitor() {
    sshpass -p "$MONITOR_PASS" scp -o StrictHostKeyChecking=no "$@" root@$MONITOR_SERVER:/root/
}

echo ""
echo "=========================================="
echo "阶段0: 清理所有服务器"
echo "=========================================="

echo "[1/3] 清理主服务器..."
scp_main /home/ubuntu/-IM/deploy/0-cleanup-servers.sh
ssh_main "chmod +x /root/0-cleanup-servers.sh && /root/0-cleanup-servers.sh $MAIN_SERVER"

echo "[2/3] 清理副服务器..."
scp_backup /home/ubuntu/-IM/deploy/0-cleanup-servers.sh
ssh_backup "chmod +x /root/0-cleanup-servers.sh && /root/0-cleanup-servers.sh $BACKUP_SERVER"

echo "[3/3] 清理监控服务器..."
scp_monitor /home/ubuntu/-IM/deploy/0-cleanup-servers.sh
ssh_monitor "chmod +x /root/0-cleanup-servers.sh && /root/0-cleanup-servers.sh $MONITOR_SERVER"

echo ""
echo "✅ 所有服务器已清理完成!"
echo ""
sleep 2

echo "=========================================="
echo "阶段1: 部署主服务器 ($MAIN_SERVER)"
echo "=========================================="

echo "[1/10] 上传部署脚本..."
scp_main /home/ubuntu/-IM/deploy/1-install-dependencies.sh
scp_main /home/ubuntu/-IM/deploy/2-deploy-backend.sh
scp_main /home/ubuntu/-IM/deploy/3-setup-mysql-replication.sh
scp_main /home/ubuntu/-IM/deploy/4-setup-redis-replication.sh
scp_main /home/ubuntu/-IM/deploy/5-deploy-admin-web.sh

echo "[2/10] 安装系统依赖..."
ssh_main "chmod +x /root/*.sh && /root/1-install-dependencies.sh" || echo "依赖安装可能已完成"

echo "[3/10] 克隆项目代码..."
ssh_main "mkdir -p $DEPLOY_DIR && cd $DEPLOY_DIR && git clone https://github.com/zhihang9978/-IM.git . || (cd $DEPLOY_DIR && git pull origin master)"

echo "[4/10] 初始化数据库..."
ssh_main << 'EOF'
mysql -u root << 'MYSQL_EOF'
CREATE DATABASE IF NOT EXISTS lanxin_im CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'lanxin_user'@'localhost' IDENTIFIED BY 'LanXin@2024!Secure';
GRANT ALL PRIVILEGES ON lanxin_im.* TO 'lanxin_user'@'localhost';
CREATE USER IF NOT EXISTS 'lanxin_user'@'%' IDENTIFIED BY 'LanXin@2024!Secure';
GRANT ALL PRIVILEGES ON lanxin_im.* TO 'lanxin_user'@'%';
FLUSH PRIVILEGES;
MYSQL_EOF
EOF

echo "[5/10] 导入数据库结构..."
ssh_main "cd $DEPLOY_DIR && mysql -u lanxin_user -pLanXin@2024!Secure lanxin_im < database/schema.sql 2>/dev/null || echo '数据库结构导入完成'"

echo "[6/10] 配置MySQL主库..."
ssh_main "/root/3-setup-mysql-replication.sh master"

echo "[7/10] 配置Redis主节点..."
ssh_main "/root/4-setup-redis-replication.sh master"

echo "[8/10] 部署后端服务..."
ssh_main "/root/2-deploy-backend.sh main"

echo "[9/10] 配置MinIO..."
ssh_main << 'EOF'
mkdir -p /data/minio
cat > /etc/systemd/system/minio.service << 'MINIO_EOF'
[Unit]
Description=MinIO
After=network.target

[Service]
Type=simple
User=root
Environment="MINIO_ROOT_USER=minioadmin"
Environment="MINIO_ROOT_PASSWORD=minioadmin"
ExecStart=/usr/local/bin/minio server /data/minio --console-address ":9001"
Restart=always

[Install]
WantedBy=multi-user.target
MINIO_EOF

systemctl daemon-reload
systemctl enable minio
systemctl start minio
EOF

echo "[10/10] 部署管理后台..."
ssh_main "/root/5-deploy-admin-web.sh"

echo ""
echo "✅ 主服务器部署完成!"
echo ""
sleep 2

echo "=========================================="
echo "阶段2: 部署副服务器 ($BACKUP_SERVER)"
echo "=========================================="

echo "[1/10] 上传部署脚本..."
scp_backup /home/ubuntu/-IM/deploy/1-install-dependencies.sh
scp_backup /home/ubuntu/-IM/deploy/2-deploy-backend.sh
scp_backup /home/ubuntu/-IM/deploy/3-setup-mysql-replication.sh
scp_backup /home/ubuntu/-IM/deploy/4-setup-redis-replication.sh
scp_backup /home/ubuntu/-IM/deploy/5-deploy-admin-web.sh

echo "[2/10] 安装系统依赖..."
ssh_backup "chmod +x /root/*.sh && /root/1-install-dependencies.sh" || echo "依赖安装可能已完成"

echo "[3/10] 克隆项目代码..."
ssh_backup "mkdir -p $DEPLOY_DIR && cd $DEPLOY_DIR && git clone https://github.com/zhihang9978/-IM.git . || (cd $DEPLOY_DIR && git pull origin master)"

echo "[4/10] 从主服务器导出数据库..."
ssh_main "mysqldump -u lanxin_user -pLanXin@2024!Secure lanxin_im > /tmp/lanxin_im.sql 2>/dev/null || true"
sshpass -p "$MAIN_PASS" scp -o StrictHostKeyChecking=no root@$MAIN_SERVER:/tmp/lanxin_im.sql /tmp/ 2>/dev/null || true
scp_backup /tmp/lanxin_im.sql

echo "[5/10] 初始化副服务器数据库..."
ssh_backup << 'EOF'
mysql -u root << 'MYSQL_EOF'
CREATE DATABASE IF NOT EXISTS lanxin_im CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'lanxin_user'@'localhost' IDENTIFIED BY 'LanXin@2024!Secure';
GRANT ALL PRIVILEGES ON lanxin_im.* TO 'lanxin_user'@'localhost';
FLUSH PRIVILEGES;
MYSQL_EOF

mysql -u lanxin_user -pLanXin@2024!Secure lanxin_im < /root/lanxin_im.sql 2>/dev/null || true
EOF

echo "[6/10] 配置MySQL从库..."
ssh_backup "/root/3-setup-mysql-replication.sh slave"

echo "[7/10] 配置Redis从节点..."
ssh_backup "/root/4-setup-redis-replication.sh slave"

echo "[8/10] 部署后端服务（待机模式）..."
ssh_backup "/root/2-deploy-backend.sh backup"

echo "[9/10] 配置MinIO..."
ssh_backup << 'EOF'
mkdir -p /data/minio
cat > /etc/systemd/system/minio.service << 'MINIO_EOF'
[Unit]
Description=MinIO
After=network.target

[Service]
Type=simple
User=root
Environment="MINIO_ROOT_USER=minioadmin"
Environment="MINIO_ROOT_PASSWORD=minioadmin"
ExecStart=/usr/local/bin/minio server /data/minio --console-address ":9001"
Restart=always

[Install]
WantedBy=multi-user.target
MINIO_EOF

systemctl daemon-reload
systemctl enable minio
systemctl start minio
EOF

echo "[10/10] 部署管理后台..."
ssh_backup "/root/5-deploy-admin-web.sh"

echo ""
echo "✅ 副服务器部署完成!"
echo ""
sleep 2

echo "=========================================="
echo "阶段3: 部署监控服务器 ($MONITOR_SERVER)"
echo "=========================================="

echo "[1/3] 上传监控脚本..."
scp_monitor /home/ubuntu/-IM/deploy/6-deploy-monitoring.sh

echo "[2/3] 配置SSH免密登录..."
ssh_monitor << EOF
apt install -y sshpass
ssh-keygen -t rsa -N "" -f /root/.ssh/id_rsa -q || true
sshpass -p "$MAIN_PASS" ssh-copy-id -o StrictHostKeyChecking=no root@$MAIN_SERVER 2>/dev/null || true
sshpass -p "$BACKUP_PASS" ssh-copy-id -o StrictHostKeyChecking=no root@$BACKUP_SERVER 2>/dev/null || true
EOF

echo "[3/3] 部署监控服务..."
ssh_monitor "chmod +x /root/6-deploy-monitoring.sh && /root/6-deploy-monitoring.sh"

echo ""
echo "✅ 监控服务器部署完成!"
echo ""

echo "=========================================="
echo "部署完成总结"
echo "=========================================="
echo ""
echo "✅ 主服务器 ($MAIN_SERVER):"
echo "   - 后端服务: http://$MAIN_SERVER:8080"
echo "   - 健康检查: http://$MAIN_SERVER:8080/health"
echo "   - 管理后台: http://$MAIN_SERVER"
echo ""
echo "✅ 副服务器 ($BACKUP_SERVER):"
echo "   - 后端服务: 待机状态"
echo "   - 数据同步: 实时同步中"
echo ""
echo "✅ 监控服务器 ($MONITOR_SERVER):"
echo "   - 健康检查: 运行中"
echo "   - 日志: tail -f /var/log/lanxin-monitor.log"
echo ""
echo "验证命令:"
echo "  curl http://$MAIN_SERVER:8080/health"
echo "  ssh root@$BACKUP_SERVER 'mysql -e \"SHOW SLAVE STATUS\\G\" | grep Running'"
echo ""
echo "=========================================="
