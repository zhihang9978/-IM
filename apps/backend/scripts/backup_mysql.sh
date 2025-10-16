#!/bin/bash

#===============================================================================
# MySQL数据库自动备份脚本
# 用途: 定时备份蓝信通讯数据库
# 使用: 添加到crontab定时执行
#===============================================================================

# 配置变量
DB_HOST="localhost"
DB_PORT="3306"
DB_USER="lanxin"
DB_PASSWORD="${MYSQL_PASSWORD}"  # 从环境变量读取
DB_NAME="lanxin_im"

# 备份目录
BACKUP_DIR="/opt/lanxin/backups/mysql"
DATE=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/lanxin_im_${DATE}.sql"

# 保留天数（删除7天前的备份）
RETENTION_DAYS=7

# 创建备份目录
mkdir -p ${BACKUP_DIR}

# 执行备份
echo "[$(date)] 开始备份数据库: ${DB_NAME}"

mysqldump -h ${DB_HOST} \
  -P ${DB_PORT} \
  -u ${DB_USER} \
  -p${DB_PASSWORD} \
  --single-transaction \
  --routines \
  --triggers \
  --events \
  --databases ${DB_NAME} \
  > ${BACKUP_FILE}

# 检查备份是否成功
if [ $? -eq 0 ]; then
  echo "[$(date)] 备份成功: ${BACKUP_FILE}"
  
  # 压缩备份文件
  gzip ${BACKUP_FILE}
  echo "[$(date)] 压缩完成: ${BACKUP_FILE}.gz"
  
  # 删除旧备份
  find ${BACKUP_DIR} -name "*.sql.gz" -mtime +${RETENTION_DAYS} -delete
  echo "[$(date)] 清理旧备份完成（保留${RETENTION_DAYS}天）"
  
  # 记录到操作日志
  echo "[$(date)] 数据库备份成功" >> /opt/lanxin/logs/backup.log
else
  echo "[$(date)] 备份失败！" >&2
  echo "[$(date)] 数据库备份失败" >> /opt/lanxin/logs/backup.log
  exit 1
fi

# 可选：上传到远程存储（如MinIO）
# mc cp ${BACKUP_FILE}.gz minio/lanxin-backups/mysql/

echo "[$(date)] 备份流程完成"

