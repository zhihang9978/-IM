# 监控系统配置文档

## Grafana 中文化配置

**配置日期**: 2025-10-17  
**状态**: ✅ 已完成

### 配置步骤

1. **环境变量配置**
   - 在 `docker-compose.yml` 中添加: `GF_DEFAULT_LOCALE=zh-Hans`
   - 设置时区: `GF_DEFAULT_TIMEZONE=Asia/Shanghai`

2. **Grafana 配置文件**
   - 创建 `/opt/monitoring/grafana/grafana.ini`
   - 设置 `default_language = zh-Hans` 在 `[users]` 部分

3. **用户偏好设置**
   - 通过 API 更新管理员用户语言偏好为中文

### 访问方式

- **URL**: http://154.37.212.67:3000
- **用户名**: admin
- **密码**: Admin@2025
- **语言**: 简体中文 (zh-Hans)

### 中文化效果

✅ 所有菜单和界面元素已完全中文化：
- 首页、书签、已加星标
- 仪表板、探索、警报
- 连接、管理
- 所有按钮和提示文本

---

## Grafana 告警规则配置

**配置日期**: 2025-10-17  
**状态**: ✅ 已完成

### 告警规则列表

#### 1. CPU使用率过高
- **触发条件**: CPU使用率 > 80%
- **持续时间**: 5分钟
- **严重程度**: warning
- **描述**: 监控服务器CPU使用率，超过80%时触发告警

#### 2. 内存使用率过高
- **触发条件**: 内存使用率 > 85%
- **持续时间**: 5分钟
- **严重程度**: warning
- **描述**: 监控服务器内存使用率，超过85%时触发告警

#### 3. 磁盘使用率过高
- **触发条件**: 磁盘使用率 > 90%
- **持续时间**: 5分钟
- **严重程度**: critical
- **描述**: 监控服务器根分区磁盘使用率，超过90%时触发告警

#### 4. 服务器宕机
- **触发条件**: node_exporter 服务无响应
- **持续时间**: 1分钟
- **严重程度**: critical
- **描述**: 监控服务器状态，宕机或无法访问时立即告警

#### 5. 后端服务停止
- **触发条件**: backend_server 服务无响应
- **持续时间**: 2分钟
- **严重程度**: critical
- **描述**: 监控蓝信后端服务状态，停止或无响应时告警

### 配置文件

告警规则配置文件位于:
```
/opt/monitoring/grafana/provisioning/alerting/alerts.yaml
```

### 告警规则组织

- **文件夹**: 蓝信通讯系统监控
- **规则组**: 蓝信通讯系统告警
- **检查间隔**: 1分钟
- **规则数量**: 5个

### 告警状态

所有告警规则当前状态: **Normal** (正常)
- ✅ CPU使用率过高 - Normal
- ✅ 内存使用率过高 - Normal
- ✅ 磁盘使用率过高 - Normal
- ✅ 服务器宕机 - Normal
- ✅ 后端服务停止 - Normal

---

## 监控架构

### 服务器信息

**监控服务器**: 154.37.212.67  
**密码**: QcTzmHVsWPde6A7o

### 部署的服务

1. **Prometheus** (端口: 9090)
   - 时序数据库
   - 数据收集和存储
   - 告警规则评估

2. **Grafana** (端口: 3000)
   - 可视化界面
   - 告警管理
   - 仪表板展示

3. **Node Exporter** (端口: 9100)
   - 系统指标收集
   - CPU、内存、磁盘、网络监控

### 监控目标

当前监控的服务器:
- 主服务器: 154.40.45.121
- 备份服务器: 154.40.45.98
- 监控服务器: 154.37.212.67

---

## 维护指南

### 重启服务

```bash
# SSH登录监控服务器
ssh root@154.37.212.67

# 重启所有服务
cd /opt/monitoring
docker compose restart

# 重启单个服务
docker compose restart grafana
docker compose restart prometheus
```

### 查看日志

```bash
# 查看Grafana日志
docker logs grafana -f

# 查看Prometheus日志
docker logs prometheus -f

# 查看所有服务日志
cd /opt/monitoring
docker compose logs -f
```

### 更新告警规则

1. 编辑配置文件:
   ```bash
   vi /opt/monitoring/grafana/provisioning/alerting/alerts.yaml
   ```

2. 重启Grafana:
   ```bash
   cd /opt/monitoring
   docker compose restart grafana
   ```

3. 验证告警规则:
   - 访问: http://154.37.212.67:3000/alerting/list
   - 检查规则状态是否为"Normal"

---

## 故障排查

### Grafana 无法访问

1. 检查容器状态:
   ```bash
   docker ps | grep grafana
   ```

2. 检查端口:
   ```bash
   netstat -tlnp | grep 3000
   ```

3. 查看日志:
   ```bash
   docker logs grafana
   ```

### 告警规则不生效

1. 检查Prometheus数据源:
   - Grafana → 连接 → 数据源
   - 验证Prometheus连接状态

2. 检查告警配置:
   ```bash
   cat /opt/monitoring/grafana/provisioning/alerting/alerts.yaml
   ```

3. 重新加载配置:
   ```bash
   docker compose restart grafana
   ```

### 中文界面未生效

1. 清除浏览器缓存
2. 检查用户偏好设置:
   - Grafana → 个人资料 → 偏好设置
   - 语言应显示为"简体中文"

---

## 下一步计划

### 待配置功能

1. **告警通知渠道**
   - 邮件通知
   - 钉钉/企业微信webhook
   - 短信告警

2. **自定义仪表板**
   - 系统概览仪表板
   - 应用性能仪表板
   - 业务指标仪表板

3. **高级告警规则**
   - API响应时间监控
   - 数据库连接数监控
   - Redis缓存命中率监控
   - 消息队列积压监控

---

**最后更新**: 2025-10-17  
**维护人员**: Devin (AI Assistant)
