#!/bin/bash

MAIN_SERVER="154.40.45.121"
BACKUP_SERVER="154.40.45.98"
CHECK_INTERVAL=10  # 秒
FAILURE_THRESHOLD=3  # 连续失败次数阈值

main_failures=0
backup_failures=0

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a /var/log/lanxin-monitor.log
}

check_server() {
    local server=$1
    local endpoint="http://$server:8080/health"
    
    response=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 5 --max-time 10 "$endpoint")
    
    if [ "$response" = "200" ]; then
        return 0
    else
        return 1
    fi
}

send_alert() {
    local message=$1
    log "⚠️  ALERT: $message"
}

switch_to_backup() {
    log "🔄 切换到副服务器..."
    send_alert "主服务器不可用，切换到副服务器 $BACKUP_SERVER"
    
}

switch_to_main() {
    log "🔄 恢复到主服务器..."
    send_alert "主服务器已恢复，切换回主服务器 $MAIN_SERVER"
}

log "=========================================="
log "蓝信IM健康监控服务启动"
log "主服务器: $MAIN_SERVER"
log "副服务器: $BACKUP_SERVER"
log "检查间隔: ${CHECK_INTERVAL}秒"
log "失败阈值: $FAILURE_THRESHOLD次"
log "=========================================="

current_server="main"

while true; do
    if check_server "$MAIN_SERVER"; then
        if [ $main_failures -gt 0 ]; then
            log "✅ 主服务器恢复正常"
            main_failures=0
            
            if [ "$current_server" = "backup" ]; then
                switch_to_main
                current_server="main"
            fi
        fi
    else
        main_failures=$((main_failures + 1))
        log "❌ 主服务器健康检查失败 ($main_failures/$FAILURE_THRESHOLD)"
        
        if [ $main_failures -ge $FAILURE_THRESHOLD ] && [ "$current_server" = "main" ]; then
            switch_to_backup
            current_server="backup"
        fi
    fi
    
    if check_server "$BACKUP_SERVER"; then
        if [ $backup_failures -gt 0 ]; then
            log "✅ 副服务器恢复正常"
            backup_failures=0
        fi
    else
        backup_failures=$((backup_failures + 1))
        log "⚠️  副服务器健康检查失败 ($backup_failures/$FAILURE_THRESHOLD)"
        
        if [ $backup_failures -ge $FAILURE_THRESHOLD ] && [ $main_failures -ge $FAILURE_THRESHOLD ]; then
            send_alert "严重告警：主副服务器均不可用！"
        fi
    fi
    
    if [ $((SECONDS % 60)) -eq 0 ]; then
        log "状态: 当前使用=${current_server}, 主服务器失败=${main_failures}, 副服务器失败=${backup_failures}"
    fi
    
    sleep $CHECK_INTERVAL
done
