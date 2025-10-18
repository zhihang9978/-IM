#!/bin/bash

set -e

ROLE=${1:-master}  # master 或 slave
MASTER_IP="154.40.45.121"
SLAVE_IP="154.40.45.98"
REPL_USER="repl_user"
REPL_PASS="Repl@2024!Secure"

echo "=========================================="
echo "MySQL主从复制配置 - $ROLE"
echo "=========================================="

if [ "$ROLE" = "master" ]; then
    echo "[主库配置]"
    
    cat > /etc/mysql/mysql.conf.d/replication.cnf << EOF
[mysqld]
server-id = 1
log_bin = /var/log/mysql/mysql-bin.log
binlog_format = ROW
binlog_do_db = lanxin_im
gtid_mode = ON
enforce_gtid_consistency = ON
sync_binlog = 1
innodb_flush_log_at_trx_commit = 1
EOF

    systemctl restart mysql
    
    mysql -e "CREATE USER IF NOT EXISTS '${REPL_USER}'@'${SLAVE_IP}' IDENTIFIED BY '${REPL_PASS}';"
    mysql -e "GRANT REPLICATION SLAVE ON *.* TO '${REPL_USER}'@'${SLAVE_IP}';"
    mysql -e "FLUSH PRIVILEGES;"
    
    mysql -e "SHOW MASTER STATUS\G"
    
    echo "✅ MySQL主库配置完成"
    
elif [ "$ROLE" = "slave" ]; then
    echo "[从库配置]"
    
    cat > /etc/mysql/mysql.conf.d/replication.cnf << EOF
[mysqld]
server-id = 2
relay-log = /var/log/mysql/mysql-relay-bin.log
log_bin = /var/log/mysql/mysql-bin.log
binlog_format = ROW
read_only = 1
gtid_mode = ON
enforce_gtid_consistency = ON
EOF

    systemctl restart mysql
    
    mysql -e "STOP SLAVE;"
    mysql -e "CHANGE MASTER TO 
        MASTER_HOST='${MASTER_IP}',
        MASTER_USER='${REPL_USER}',
        MASTER_PASSWORD='${REPL_PASS}',
        MASTER_AUTO_POSITION=1;"
    mysql -e "START SLAVE;"
    
    mysql -e "SHOW SLAVE STATUS\G"
    
    echo "✅ MySQL从库配置完成"
fi

echo "=========================================="
echo "配置完成!"
echo "=========================================="
