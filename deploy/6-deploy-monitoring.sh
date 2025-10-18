#!/bin/bash

set -e

MAIN_SERVER="154.40.45.121"
BACKUP_SERVER="154.40.45.98"

echo "=========================================="
echo "蓝信IM - 部署监控服务"
echo "=========================================="

mkdir -p /opt/lanxin-monitor

cat > /opt/lanxin-monitor/health-check.sh << 'EOF'
#!/bin/bash

MAIN_SERVER="154.40.45.121"
BACKUP_SERVER="154.40.45.98"
CHECK_INTERVAL=5
FAIL_THRESHOLD=3
LOG_FILE="/var/log/lanxin-monitor.log"

main_failures=0
current_active="main"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a $LOG_FILE
}

check_health() {
    local server=$1
    local url="http://$server:8080/health"
    
    if curl -s -f -m 5 $url > /dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

promote_backup() {
    log "!!! 主服务器故障，提升副服务器为主服务器 !!!"
    
    ssh root@$BACKUP_SERVER << 'REMOTE_EOF'
        mysql -e "STOP SLAVE; RESET SLAVE ALL; SET GLOBAL read_only = 0;"
        
        redis-cli -a LanXinRedis@2024 SLAVEOF NO ONE
        
        systemctl start lanxin-im
        systemctl enable lanxin-im
        
        echo "副服务器已提升为主服务器"
REMOTE_EOF
    
    current_active="backup"
    log "故障转移完成，当前活跃服务器: 副服务器"
    
    send_alert "主服务器故障，已自动切换到副服务器"
}

demote_backup() {
    log "主服务器恢复，将副服务器降级为从服务器"
    
    ssh root@$BACKUP_SERVER << 'REMOTE_EOF'
        systemctl stop lanxin-im
        systemctl disable lanxin-im
        
        mysql -e "SET GLOBAL read_only = 1; CHANGE MASTER TO MASTER_HOST='154.40.45.121', MASTER_USER='repl_user', MASTER_PASSWORD='Repl@2024!Secure', MASTER_AUTO_POSITION=1; START SLAVE;"
        
        redis-cli -a LanXinRedis@2024 SLAVEOF 154.40.45.121 6379
        
        echo "副服务器已恢复为从服务器"
REMOTE_EOF
    
    current_active="main"
    log "副服务器已降级，当前活跃服务器: 主服务器"
}

send_alert() {
    local message=$1
    log "告警: $message"
}

log "监控服务启动"

while true; do
    if check_health $MAIN_SERVER; then
        main_failures=0
        
        if [ "$current_active" = "backup" ]; then
            log "主服务器已恢复"
            demote_backup
        fi
    else
        main_failures=$((main_failures + 1))
        log "主服务器健康检查失败 ($main_failures/$FAIL_THRESHOLD)"
        
        if [ $main_failures -ge $FAIL_THRESHOLD ] && [ "$current_active" = "main" ]; then
            promote_backup
        fi
    fi
    
    sleep $CHECK_INTERVAL
done
EOF

chmod +x /opt/lanxin-monitor/health-check.sh

cat > /etc/systemd/system/lanxin-monitor.service << 'EOF'
[Unit]
Description=LanXin IM Health Monitor
After=network.target

[Service]
Type=simple
User=root
ExecStart=/opt/lanxin-monitor/health-check.sh
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable lanxin-monitor
systemctl start lanxin-monitor

echo "=========================================="
echo "✅ 监控服务部署完成!"
echo "日志文件: /var/log/lanxin-monitor.log"
echo "=========================================="
