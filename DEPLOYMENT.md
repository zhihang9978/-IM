# 蓝信通讯系统部署文档

## 部署概览

**部署日期**: 2025-10-16  
**部署架构**: 三服务器高可用架构  
**域名**: lanxin168.com

## 服务器配置

### 主服务器 (154.40.45.121)
**角色**: 生产主服务器  
**系统**: Ubuntu 22.04.5 LTS  
**已安装服务**:
- Go 1.21.5
- Node.js 20.19.5 + pnpm 10.18.3
- MySQL 8.0 (主库)
- Redis 6.0.16
- Nginx 1.18.0
- Lsyncd (文件同步)
- Keepalived (待配置)

**部署内容**:
- 后端服务: `/opt/lanxin/bin/lanxin-server`
- 前端应用: `/var/www/lanxin-admin`
- 配置文件: `/opt/lanxin/config/config.yaml`
- 日志目录: `/opt/lanxin/logs/`

**数据库**:
- 数据库名: `lanxin_im`
- 用户名: `lanxin`
- 表结构: 7张表 (users, messages, conversations, contacts, groups, group_members, operation_logs)

### 备份服务器 (154.40.45.98)
**角色**: 热备份服务器  
**系统**: Ubuntu 22.04.5 LTS  
**已安装服务**:
- Go 1.21.5
- MySQL 8.0 (从库 - 主从复制已配置)
- Redis 6.0.16
- Nginx 1.18.0
- Keepalived (待配置)

**MySQL 主从复制**:
- Master: 154.40.45.121:3306
- Slave: 154.40.45.98:3306
- Binlog Position: mysql-bin.000002:859
- 状态: Slave_SQL_Running: Yes

### 监控服务器 (154.37.212.67)
**角色**: 监控与告警  
**系统**: Ubuntu 22.04.1 LTS  
**待部署**: Prometheus + Grafana + Node Exporter

## 网络架构

### 端口分配
- **80**: HTTP (Nginx)
- **443**: HTTPS (待配置 SSL)
- **8080**: 后端 API 服务
- **3306**: MySQL
- **6379**: Redis
- **9092**: Kafka (待配置)

### 域名配置
**注意**: DNS 记录尚未生效，需要等待传播

预计配置的子域名:
- `admin.lanxin168.com` - 管理后台
- `api.lanxin168.com` - API 接口
- `im.lanxin168.com` - 即时通讯主域名
- `files.lanxin168.com` - 文件存储
- `rtc.lanxin168.com` - 实时音视频
- `bot.lanxin168.com` - 机器人服务
- `cdn.lanxin168.com` - CDN 加速
- `core.lanxin168.com` - 核心服务
- `storage.lanxin168.com` - 存储服务

## 服务配置

### 后端服务
**服务名称**: lanxin.service  
**服务文件**: `/etc/systemd/system/lanxin.service`

```bash
# 启动服务
systemctl start lanxin

# 停止服务
systemctl stop lanxin

# 重启服务
systemctl restart lanxin

# 查看状态
systemctl status lanxin

# 查看日志
tail -f /opt/lanxin/logs/server.log
```

**健康检查**:
```bash
curl http://localhost:8080/health
# 预期返回: {"status":"ok","message":"LanXin IM Server is running","online_users":0}
```

### Nginx 配置
**配置文件**: `/etc/nginx/sites-available/lanxin`

**路由规则**:
- `/` - 前端静态文件 (React Admin)
- `/api/*` - 反向代理到后端 :8080
- `/ws` - WebSocket 连接
- `/health` - 健康检查端点

```bash
# 测试配置
nginx -t

# 重载配置
systemctl reload nginx
```

### 文件同步 (Lsyncd)
**配置文件**: `/etc/lsyncd/lsyncd.conf.lua`

**同步目录**:
1. `/var/www/lanxin-admin` → 备份服务器
2. `/opt/lanxin/bin` → 备份服务器
3. `/opt/lanxin/logs` → 备份服务器

```bash
# 查看同步状态
systemctl status lsyncd
cat /var/log/lsyncd/lsyncd.status
```

## 安全配置

### 防火墙规则 (待配置)
```bash
# 允许 HTTP/HTTPS
ufw allow 80/tcp
ufw allow 443/tcp

# 允许内网访问 MySQL
ufw allow from 154.40.45.98 to any port 3306

# 启用防火墙
ufw enable
```

### JWT 配置
- **密钥**: lanxin-jwt-secret-key-2025-production
- **过期时间**: 24小时
- **刷新令牌**: 168小时 (7天)

### 密码策略
- **Bcrypt Cost**: 12
- **限流**: 60 请求/分钟

## 第三方服务

### 腾讯云 TRTC (实时音视频)
- **SDKAppID**: 1600109367
- **私钥**: 已配置到 config.yaml
- **状态**: 已配置

### 对象存储 (COS)
- **类型**: 自建存储
- **基础URL**: https://files.lanxin168.com
- **状态**: 待配置

## 数据库信息

### MySQL 主库配置
```ini
[mysqld]
server-id=1
log_bin=/var/log/mysql/mysql-bin.log
binlog_do_db=lanxin_im
bind-address=0.0.0.0
```

### MySQL 从库配置
```ini
[mysqld]
server-id=2
relay-log=/var/log/mysql/mysql-relay-bin
log_bin=/var/log/mysql/mysql-bin.log
binlog_do_db=lanxin_im
read_only=1
```

### 数据库迁移
所有迁移文件位于: `apps/backend/migrations/`

```bash
# 执行迁移
mysql lanxin_im < migrations/001_create_users_table.up.sql
mysql lanxin_im < migrations/002_create_messages_table.up.sql
# ... 等等
```

## 当前访问方式

### HTTP 访问 (临时)
- **管理后台**: http://154.40.45.121
- **API健康检查**: http://154.40.45.121/health
- **API端点**: http://154.40.45.121/api/v1/*

### 待配置的 HTTPS 访问
一旦 DNS 生效，将配置:
- **管理后台**: https://admin.lanxin168.com
- **API**: https://api.lanxin168.com
- **即时通讯**: https://im.lanxin168.com

## 待完成任务

### 高优先级
1. **SSL 证书获取**: 等待 DNS 传播后使用 Let's Encrypt
2. **Keepalived 虚拟IP**: 配置故障自动切换
3. **监控系统**: 部署 Prometheus + Grafana
4. **防火墙规则**: 配置 UFW 安全策略

### 中优先级
5. **Kafka 部署**: 消息队列服务
6. **对象存储**: 配置文件上传服务
7. **日志聚合**: ELK Stack 或 Loki
8. **备份策略**: 数据库自动备份脚本

### 测试任务
9. **功能测试**: 用户注册、登录、消息发送
10. **故障转移测试**: 模拟主服务器宕机
11. **压力测试**: 并发用户和消息吞吐量
12. **安全测试**: SQL注入、XSS、CSRF等

## 故障排查

### 后端服务无法启动
```bash
# 检查日志
tail -50 /opt/lanxin/logs/server.log

# 检查端口占用
ss -tlnp | grep :8080

# 检查配置文件
cat /opt/lanxin/config/config.yaml
```

### 数据库连接失败
```bash
# 测试MySQL连接
mysql -h localhost -u lanxin -p lanxin_im

# 检查MySQL状态
systemctl status mysql

# 查看MySQL错误日志
tail -50 /var/log/mysql/error.log
```

### 主从复制异常
```bash
# 在从库检查复制状态
mysql -e "SHOW SLAVE STATUS\G"

# 查看关键指标
# Slave_IO_Running: Yes
# Slave_SQL_Running: Yes
# Seconds_Behind_Master: 0 (或小数字)
```

### Nginx 502 错误
```bash
# 检查后端服务是否运行
systemctl status lanxin

# 检查后端端口
curl http://localhost:8080/health

# 检查Nginx错误日志
tail -50 /var/log/nginx/error.log
```

## 性能优化建议

### MySQL 优化
```ini
# 在 my.cnf 中添加
max_connections = 200
innodb_buffer_pool_size = 2G
query_cache_size = 64M
```

### Redis 优化
```ini
# 在 redis.conf 中设置
maxmemory 1gb
maxmemory-policy allkeys-lru
```

### Nginx 优化
```nginx
# worker 进程数
worker_processes auto;

# 连接数
worker_connections 2048;

# 启用 gzip 压缩
gzip on;
gzip_types text/plain text/css application/json application/javascript;
```

## 联系方式

**部署工程师**: Devin  
**项目负责人**: 陈俊杰  
**部署时间**: 2025-10-16  
**Devin运行链接**: https://app.devin.ai/sessions/42eb7240458c48298838ad1a8631daeb

## 附录

### Android 密钥库
- **位置**: `/home/ubuntu/lanxin-test.keystore`
- **别名**: lanxin
- **密码**: lanxin123
- **有效期**: 10000天
- **用途**: 测试环境APK签名

### 服务器密码
**重要**: 生产环境密码应该通过密钥管理系统管理，不应明文存储在文档中。

### 备份策略
建议配置:
1. MySQL 每日全量备份
2. 增量备份每4小时
3. 保留最近7天的备份
4. 异地备份到云存储

### 监控指标
建议监控:
- CPU、内存、磁盘使用率
- 网络流量
- API 响应时间
- 在线用户数
- 消息吞吐量
- 数据库连接数
- Redis 命中率
- 主从复制延迟

## SSL/HTTPS 配置

### 证书类型
- **类型**: 自签名证书（10年有效期）
- **通配符域名**: *.lanxin168.com
- **证书位置**: 
  - 证书文件: /etc/ssl/certs/lanxin.crt
  - 私钥文件: /etc/ssl/private/lanxin.key

### Cloudflare 集成
**当前配置**: Cloudflare Flexible SSL 模式
- 用户 ↔ Cloudflare: HTTPS (由 Cloudflare 提供证书)
- Cloudflare ↔ 源服务器: HTTP (内网通信)

**访问地址**:
- 管理后台: https://admin.lanxin168.com
- API 接口: https://api.lanxin168.com
- 即时通讯: https://im.lanxin168.com
- 所有域名已启用 Cloudflare 代理

**建议升级**: 
可以考虑升级到 "完全(严格)" 模式以提高安全性：
1. 在 Cloudflare SSL/TLS 设置中
2. 选择 "完全(严格)" 模式
3. 服务器已配置 SSL，可以支持此模式

### Nginx HTTPS 配置
```nginx
# HTTP - Cloudflare Flexible Mode
server {
    listen 80;
    server_name *.lanxin168.com;
    # 接受来自 Cloudflare 的 HTTP 请求
}

# HTTPS - 直接访问
server {
    listen 443 ssl http2;
    server_name *.lanxin168.com;
    ssl_certificate /etc/ssl/certs/lanxin.crt;
    ssl_certificate_key /etc/ssl/private/lanxin.key;
}
```

### WebSocket Secure (WSS)
- WebSocket 端点: wss://im.lanxin168.com/ws
- 通过 Cloudflare 自动升级为安全连接
- Nginx 配置支持 WebSocket 代理

## 最终SSL配置 (Cloudflare Origin Certificate)

### 证书详情
- **类型**: Cloudflare Origin Certificate (PEM格式)
- **有效期**: 15年 (2025-10-16 至 2040-10-12)
- **覆盖域名**: *.lanxin168.com 和 lanxin168.com
- **证书位置**:
  - Main Server: /etc/ssl/certs/cloudflare-origin.crt
  - Private Key: /etc/ssl/private/cloudflare-origin.key
- **备份服务器**: 证书已同步

### Cloudflare SSL 模式
**推荐设置**: 完全(严格) Full (Strict)
- 用户 ↔ Cloudflare: HTTPS (由 Cloudflare Universal SSL 提供)
- Cloudflare ↔ 源服务器: HTTPS (由 Cloudflare Origin Certificate 验证)
- 提供端到端加密保护

### 已验证的HTTPS访问
```bash
# API健康检查
curl https://api.lanxin168.com/health
# 返回: {"status":"ok","message":"LanXin IM Server is running","online_users":0}

# 管理后台
https://admin.lanxin168.com

# 即时通讯
https://im.lanxin168.com

# WebSocket连接
wss://im.lanxin168.com/ws
```

### 安全配置
- SSL协议: TLSv1.2, TLSv1.3
- 密码套件: ECDHE-RSA-AES256-GCM-SHA384 等强加密算法
- Session Cache: 10MB 共享缓存
- Session Timeout: 10分钟

