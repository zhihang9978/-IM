# 蓝信IM高可用部署指南

## 部署架构

```
                    ┌─────────────────┐
                    │  监控服务器      │
                    │  154.37.212.67  │
                    │  - 健康检查      │
                    │  - 自动故障转移  │
                    └────────┬────────┘
                             │
              ┌──────────────┴──────────────┐
              │                             │
    ┌─────────▼─────────┐         ┌────────▼──────────┐
    │   主服务器 (主)    │         │   副服务器 (从)    │
    │  154.40.45.121    │◄────────┤  154.40.45.98     │
    │                   │  数据同步 │                   │
    │  - 后端服务 ✓     │         │  - 后端服务 (待机)│
    │  - MySQL (主库)   │────────►│  - MySQL (从库)   │
    │  - Redis (主节点) │         │  - Redis (从节点) │
    │  - MinIO         │         │  - MinIO (镜像)   │
    │  - 管理后台      │         │  - 管理后台       │
    └───────────────────┘         └───────────────────┘
            ▲
            │
            │
    ┌───────┴────────┐
    │  Android客户端  │
    │  - 自动切换     │
    │  - 无感知故障转移│
    └────────────────┘
```

## 部署步骤

### 准备工作

1. 确保三台服务器可以互相SSH访问
2. 确保防火墙开放必要端口：
   - 8080: 后端API
   - 3306: MySQL
   - 6379: Redis
   - 9000: MinIO
   - 80/443: Nginx

### 第一阶段：主服务器部署 (154.40.45.121)

```bash
# 1. 上传部署脚本到主服务器
scp -r deploy/* root@154.40.45.121:/tmp/

# 2. SSH登录主服务器
ssh root@154.40.45.121

# 3. 安装依赖
cd /tmp
chmod +x *.sh
./1-install-dependencies.sh

# 4. 初始化数据库
mysql -u root -p << EOF
CREATE DATABASE IF NOT EXISTS lanxin_im CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'lanxin_user'@'localhost' IDENTIFIED BY 'LanXin@2024!Secure';
GRANT ALL PRIVILEGES ON lanxin_im.* TO 'lanxin_user'@'localhost';
CREATE USER IF NOT EXISTS 'lanxin_user'@'%' IDENTIFIED BY 'LanXin@2024!Secure';
GRANT ALL PRIVILEGES ON lanxin_im.* TO 'lanxin_user'@'%';
FLUSH PRIVILEGES;
EOF

# 导入数据库结构
cd /var/www/im-lanxin
mysql -u root -p lanxin_im < database/schema.sql

# 5. 配置MySQL主库
./3-setup-mysql-replication.sh master

# 6. 配置Redis主节点
./4-setup-redis-replication.sh master

# 7. 部署后端服务
./2-deploy-backend.sh main

# 8. 启动MinIO
mkdir -p /data/minio
cat > /etc/systemd/system/minio.service << 'EOF'
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
EOF

systemctl daemon-reload
systemctl enable minio
systemctl start minio

# 9. 部署管理后台
./5-deploy-admin-web.sh

# 10. 验证服务
systemctl status lanxin-im
systemctl status mysql
systemctl status redis-server
systemctl status minio
systemctl status nginx

curl http://localhost:8080/health
```

### 第二阶段：副服务器部署 (154.40.45.98)

```bash
# 1. 上传部署脚本到副服务器
scp -r deploy/* root@154.40.45.98:/tmp/

# 2. SSH登录副服务器
ssh root@154.40.45.98

# 3. 安装依赖
cd /tmp
chmod +x *.sh
./1-install-dependencies.sh

# 4. 初始化数据库（从主库导入）
# 在主服务器上导出数据
ssh root@154.40.45.121 "mysqldump -u root -p lanxin_im > /tmp/lanxin_im.sql"
scp root@154.40.45.121:/tmp/lanxin_im.sql /tmp/

# 导入数据
mysql -u root -p << EOF
CREATE DATABASE IF NOT EXISTS lanxin_im CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'lanxin_user'@'localhost' IDENTIFIED BY 'LanXin@2024!Secure';
GRANT ALL PRIVILEGES ON lanxin_im.* TO 'lanxin_user'@'localhost';
FLUSH PRIVILEGES;
EOF

mysql -u root -p lanxin_im < /tmp/lanxin_im.sql

# 5. 配置MySQL从库
./3-setup-mysql-replication.sh slave

# 6. 配置Redis从节点
./4-setup-redis-replication.sh slave

# 7. 部署后端服务（待机模式）
./2-deploy-backend.sh backup

# 8. 启动MinIO（与主服务器同步）
mkdir -p /data/minio
# 配置MinIO站点复制（在主服务器上执行）
ssh root@154.40.45.121 << 'EOF'
mc alias set main http://localhost:9000 minioadmin minioadmin
mc alias set backup http://154.40.45.98:9000 minioadmin minioadmin
mc admin replicate add main backup
EOF

# 9. 部署管理后台
./5-deploy-admin-web.sh

# 10. 验证复制状态
mysql -e "SHOW SLAVE STATUS\G" | grep "Slave_IO_Running\|Slave_SQL_Running"
redis-cli -a LanXinRedis@2024 INFO replication
```

### 第三阶段：监控服务器部署 (154.37.212.67)

```bash
# 1. 上传部署脚本到监控服务器
scp deploy/6-deploy-monitoring.sh root@154.37.212.67:/tmp/

# 2. SSH登录监控服务器
ssh root@154.37.212.67

# 3. 配置SSH免密登录（用于自动故障转移）
ssh-keygen -t rsa -N "" -f /root/.ssh/id_rsa
ssh-copy-id root@154.40.45.121
ssh-copy-id root@154.40.45.98

# 4. 部署监控服务
cd /tmp
chmod +x 6-deploy-monitoring.sh
./6-deploy-monitoring.sh

# 5. 查看监控日志
tail -f /var/log/lanxin-monitor.log
```

## 测试故障转移

### 模拟主服务器故障

```bash
# 在主服务器上停止服务
ssh root@154.40.45.121 "systemctl stop lanxin-im"

# 观察监控日志（在监控服务器上）
tail -f /var/log/lanxin-monitor.log

# 应该看到：
# - 检测到主服务器故障
# - 自动提升副服务器
# - 副服务器开始接管服务

# 测试副服务器是否正常工作
curl http://154.40.45.98:8080/health
```

### 恢复主服务器

```bash
# 启动主服务器
ssh root@154.40.45.121 "systemctl start lanxin-im"

# 监控服务会自动检测到主服务器恢复
# 并将副服务器降级回从服务器
```

## Android客户端配置

需要修改客户端代码以支持多服务器：

```kotlin
// RetrofitClient.kt
object ServerConfig {
    val SERVERS = listOf(
        "http://154.40.45.121:8080/api/v1/",
        "http://154.40.45.98:8080/api/v1/"
    )
    
    var currentServerIndex = 0
    
    fun getCurrentServer(): String = SERVERS[currentServerIndex]
    
    fun switchToNextServer() {
        currentServerIndex = (currentServerIndex + 1) % SERVERS.size
    }
}

// 在网络请求失败时自动切换
class RetryInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var response: Response? = null
        var lastException: IOException? = null
        
        for (i in 0 until ServerConfig.SERVERS.size) {
            try {
                response = chain.proceed(chain.request())
                if (response.isSuccessful) {
                    return response
                }
            } catch (e: IOException) {
                lastException = e
                ServerConfig.switchToNextServer()
            }
        }
        
        throw lastException ?: IOException("All servers failed")
    }
}
```

## 监控面板

访问监控面板：
- 管理后台: http://154.40.45.121 或 http://154.40.45.98
- 系统监控: 登录后点击"系统监控"

## 常见问题

### Q: 如何检查主从复制状态？
```bash
# MySQL
mysql -e "SHOW SLAVE STATUS\G"

# Redis
redis-cli -a LanXinRedis@2024 INFO replication
```

### Q: 如何手动切换主从？
```bash
# 在监控服务器上
/opt/lanxin-monitor/health-check.sh
```

### Q: 如何重置同步？
```bash
# 在副服务器上重新同步
./3-setup-mysql-replication.sh slave
./4-setup-redis-replication.sh slave
```

## 安全建议

1. 修改所有默认密码
2. 配置防火墙规则，限制访问
3. 启用SSL/TLS加密
4. 定期备份数据
5. 监控异常登录
