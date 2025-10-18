# 蓝信IM高可用部署测试指南

## 快速开始

### 方式1: 自动化一键部署（推荐）

```bash
# 1. 克隆项目
git clone https://github.com/zhihang9978/-IM.git
cd -IM

# 2. 进入部署目录
cd deploy

# 3. 执行一键部署脚本
chmod +x deploy-all.sh
./deploy-all.sh
```

这个脚本会自动完成：
- 主服务器 (154.40.45.121) 的完整部署
- 副服务器 (154.40.45.98) 的完整部署和数据同步
- 监控服务器 (154.37.212.67) 的监控服务部署
- MySQL主从复制配置
- Redis主从复制配置
- 后端服务、管理后台、MinIO的部署

**预计时间**: 20-30分钟

### 方式2: 分步手动部署

如果需要更细粒度的控制，可以分步执行：

#### 第一步: 主服务器部署

```bash
# 1. SSH登录主服务器
ssh root@154.40.45.121

# 2. 下载部署脚本
curl -O https://raw.githubusercontent.com/zhihang9978/-IM/master/deploy/1-install-dependencies.sh
curl -O https://raw.githubusercontent.com/zhihang9978/-IM/master/deploy/2-deploy-backend.sh
curl -O https://raw.githubusercontent.com/zhihang9978/-IM/master/deploy/3-setup-mysql-replication.sh
curl -O https://raw.githubusercontent.com/zhihang9978/-IM/master/deploy/4-setup-redis-replication.sh
curl -O https://raw.githubusercontent.com/zhihang9978/-IM/master/deploy/5-deploy-admin-web.sh

chmod +x *.sh

# 3. 安装依赖
./1-install-dependencies.sh

# 4. 部署后端
./2-deploy-backend.sh main

# 5. 配置MySQL主库
./3-setup-mysql-replication.sh master

# 6. 配置Redis主节点
./4-setup-redis-replication.sh master

# 7. 部署管理后台
./5-deploy-admin-web.sh
```

#### 第二步: 副服务器部署

```bash
# 1. SSH登录副服务器
ssh root@154.40.45.98

# 2. 执行相同的依赖安装
./1-install-dependencies.sh
./2-deploy-backend.sh backup

# 3. 配置MySQL从库
./3-setup-mysql-replication.sh slave

# 4. 配置Redis从节点
./4-setup-redis-replication.sh slave

# 5. 部署管理后台
./5-deploy-admin-web.sh
```

#### 第三步: 监控服务器部署

```bash
# 1. SSH登录监控服务器
ssh root@154.37.212.67

# 2. 下载并执行监控部署
curl -O https://raw.githubusercontent.com/zhihang9978/-IM/master/deploy/6-deploy-monitoring.sh
chmod +x 6-deploy-monitoring.sh
./6-deploy-monitoring.sh
```

## 验证部署

### 1. 验证主服务器

```bash
# 检查后端服务
curl http://154.40.45.121:8080/health
# 期望: {"status":"ok"}

# 检查服务状态
ssh root@154.40.45.121 "systemctl status lanxin-im"

# 访问管理后台
# 浏览器打开: http://154.40.45.121
```

### 2. 验证副服务器

```bash
# 检查MySQL复制
ssh root@154.40.45.98 "mysql -e 'SHOW SLAVE STATUS\G' | grep Running"
# 期望: Slave_IO_Running: Yes, Slave_SQL_Running: Yes

# 检查Redis复制
ssh root@154.40.45.98 "redis-cli -a LanXinRedis@2024 INFO replication | grep role"
# 期望: role:slave
```

### 3. 验证监控服务器

```bash
# 查看监控日志
ssh root@154.37.212.67 "tail -f /var/log/lanxin-monitor.log"
# 期望: 看到定期的健康检查日志
```

## 测试故障转移

### 自动故障转移测试

```bash
# 1. 在主服务器上停止服务（模拟宕机）
ssh root@154.40.45.121 "systemctl stop lanxin-im"

# 2. 观察监控日志（应该在15秒内检测到故障）
ssh root@154.37.212.67 "tail -f /var/log/lanxin-monitor.log"

# 3. 确认副服务器已被提升
ssh root@154.40.45.98 "systemctl status lanxin-im"
# 期望: active (running)

ssh root@154.40.45.98 "mysql -e 'SHOW VARIABLES LIKE \"read_only\"'"
# 期望: read_only = OFF

# 4. 测试副服务器API
curl http://154.40.45.98:8080/health
# 期望: {"status":"ok"}

# 5. 恢复主服务器
ssh root@154.40.45.121 "systemctl start lanxin-im"
# 监控服务会自动将副服务器降级回从服务器
```

### Android客户端故障转移测试

客户端已内置自动切换功能：

1. **启动应用** - 默认连接主服务器
2. **模拟主服务器故障** - 关闭主服务器或断网
3. **观察自动切换** - 客户端会在3次重试后自动切换到副服务器
4. **恢复主服务器** - 客户端可选择手动切回主服务器

客户端切换逻辑：
- 初始连接：主服务器 (154.40.45.121)
- 连接失败：自动重试3次（每次10秒超时）
- 重试失败：自动切换到副服务器 (154.40.45.98)
- 切换间隔：最小10秒（防止频繁切换）

## 性能指标

### 故障转移时间

| 阶段 | 时间 |
|------|------|
| 故障检测 | 5-15秒 |
| 副服务器提升 | 2-5秒 |
| 客户端切换 | 10-30秒 |
| **总计** | **17-50秒** |

### 数据同步延迟

| 组件 | 延迟 |
|------|------|
| MySQL复制 | < 1秒 |
| Redis复制 | < 100ms |
| MinIO同步 | 1-5秒 |

## 监控面板

访问管理后台的系统监控页面：

```
http://154.40.45.121/system-monitor
```

可以看到：
- CPU、内存、磁盘使用率
- 服务健康状态
- WebSocket连接数
- 消息吞吐量
- 错误率统计

## 常见问题

### Q: 如何查看部署日志？

```bash
# 后端服务日志
ssh root@154.40.45.121 "journalctl -u lanxin-im -f"

# MySQL日志
ssh root@154.40.45.121 "tail -f /var/log/mysql/error.log"

# Redis日志
ssh root@154.40.45.121 "tail -f /var/log/redis/redis-server.log"

# Nginx日志
ssh root@154.40.45.121 "tail -f /var/log/nginx/error.log"
```

### Q: 如何重新同步副服务器？

```bash
# 在主服务器导出数据
ssh root@154.40.45.121 "mysqldump -u lanxin_user -pLanXin@2024!Secure lanxin_im > /tmp/lanxin_im.sql"

# 传输到副服务器
scp root@154.40.45.121:/tmp/lanxin_im.sql /tmp/
scp /tmp/lanxin_im.sql root@154.40.45.98:/tmp/

# 在副服务器导入
ssh root@154.40.45.98 "mysql -u lanxin_user -pLanXin@2024!Secure lanxin_im < /tmp/lanxin_im.sql"

# 重新配置复制
ssh root@154.40.45.98 "/root/3-setup-mysql-replication.sh slave"
```

### Q: 如何手动触发故障转移？

```bash
# SSH到监控服务器
ssh root@154.37.212.67

# 手动执行提升脚本（在监控脚本中定义）
/opt/lanxin-monitor/manual-failover.sh
```

## 安全注意事项

⚠️ **重要**: 部署脚本中包含默认密码，生产环境请务必修改：

```bash
# MySQL密码: LanXin@2024!Secure
# Redis密码: LanXinRedis@2024
# MinIO密钥: minioadmin
```

修改位置：
- MySQL: `/etc/lanxin-im.yaml`
- Redis: `/etc/redis/redis.conf`
- MinIO: `/etc/systemd/system/minio.service`

## 回滚步骤

如果部署出现问题，可以快速回滚：

```bash
# 1. 停止新服务
ssh root@154.40.45.121 "systemctl stop lanxin-im"
ssh root@154.40.45.98 "systemctl stop lanxin-im"

# 2. 恢复数据库备份
# (假设有备份文件)

# 3. 重新部署旧版本
# 从git检出旧版本并重新执行部署脚本
```

## 联系支持

如遇到部署问题：
1. 查看部署日志
2. 检查 `/var/log/lanxin-monitor.log`
3. 确认防火墙规则
4. 验证网络连接
