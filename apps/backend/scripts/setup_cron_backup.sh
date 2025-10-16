#!/bin/bash

#===============================================================================
# 设置MySQL自动备份定时任务
# 用途: 配置crontab定时执行数据库备份
#===============================================================================

SCRIPT_DIR="/opt/lanxin/bin"
BACKUP_SCRIPT="${SCRIPT_DIR}/backup_mysql.sh"

# 确保备份脚本可执行
chmod +x ${BACKUP_SCRIPT}

# 添加crontab任务
# 每天凌晨2点执行全量备份
CRON_JOB="0 2 * * * ${BACKUP_SCRIPT}"

# 检查是否已存在
if crontab -l 2>/dev/null | grep -q "${BACKUP_SCRIPT}"; then
  echo "定时备份任务已存在"
else
  # 添加到crontab
  (crontab -l 2>/dev/null; echo "${CRON_JOB}") | crontab -
  echo "定时备份任务已添加: 每天凌晨2点执行"
fi

# 显示当前crontab
echo "当前定时任务:"
crontab -l

