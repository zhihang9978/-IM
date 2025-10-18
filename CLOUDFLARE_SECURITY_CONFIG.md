# Cloudflare安全配置指南
## 蓝信IM专用 - 完全加密模式

**域名**: lanxin168.com  
**服务**: Cloudflare CDN + SSL/TLS  
**配置时间**: 2025-10-18

---

# 🔒 为什么选择Cloudflare TLS而非端到端加密

## 企业IM的安全需求对比

| 加密方案 | 适用场景 | 代表产品 | 优势 | 劣势 |
|---------|---------|---------|------|------|
| **TLS传输加密** | 企业内部协作 | 微信企业版、钉钉、飞书 | 便于管理、审计、监控 | 服务器可见明文 |
| **端到端加密(E2EE)** | 个人隐私通讯 | WhatsApp、Signal、Telegram | 极致隐私保护 | 无法审计、难以管理 |

## 蓝信IM的选择: Cloudflare TLS ✅

**理由**:
1. **企业级合规**: 符合企业内部审计要求
2. **管理便利**: 可以管理敏感信息、撤回消息
3. **成本效益**: 无需额外开发E2EE（节省5-7天工期）
4. **行业标准**: 微信企业版、钉钉、Slack等都采用此方案
5. **安全性充分**: Cloudflare提供企业级DDoS防护+WAF

---

# ✅ Cloudflare完全加密模式配置

## 1. SSL/TLS设置 (推荐配置)

### 1.1 加密模式选择

```
登录Cloudflare → 选择域名 lanxin168.com → SSL/TLS

【加密模式】
❌ Off - 不加密（危险）
❌ Flexible - 浏览器到Cloudflare加密，Cloudflare到源服务器不加密
⚠️ Full - 双向加密，但不验证源服务器证书
✅ Full (strict) - 双向加密，严格验证源服务器证书【推荐】

选择: Full (strict) ✅
```

**为什么选Full (strict)?**
- 浏览器 ↔ Cloudflare: TLS 1.3加密
- Cloudflare ↔ 源服务器: TLS 1.2+加密 + 证书验证
- 防止中间人攻击
- 企业级安全标准

### 1.2 Edge Certificates（边缘证书）

```
SSL/TLS → Edge Certificates

【Always Use HTTPS】
✅ 启用 - 自动将HTTP重定向到HTTPS

【HTTP Strict Transport Security (HSTS)】
✅ 启用
  - Max Age: 12 months (31536000秒)
  - Include subdomains: ✅
  - Preload: ✅

【Minimum TLS Version】
选择: TLS 1.2 (推荐)
说明: TLS 1.0/1.1已过时，存在安全漏洞

【Opportunistic Encryption】
✅ 启用 - 支持HTTP/2推送

【TLS 1.3】
✅ 启用 - 最新最安全的TLS版本

【Automatic HTTPS Rewrites】
✅ 启用 - 自动重写HTTP链接为HTTPS
```

### 1.3 Origin Server（源服务器证书）

```
SSL/TLS → Origin Server

如果后端服务器没有有效证书:
【Origin Certificates】
点击 "Create Certificate"
- 选择: Let Cloudflare generate a private key and a CSR
- Hostnames: 
  *.lanxin168.com
  lanxin168.com
- 有效期: 15 years (推荐)
- 点击 "Create"

下载:
- Origin Certificate (cert.pem)
- Private Key (key.pem)

安装到后端服务器:
/etc/ssl/certs/lanxin_cert.pem
/etc/ssl/private/lanxin_key.pem
```

---

## 2. Security安全设置

### 2.1 WAF（Web应用防火墙）

```
Security → WAF

【WAF Managed Rules】
✅ 启用 - Cloudflare Managed Ruleset

【OWASP Core Ruleset】
✅ 启用

【Custom Rules】（可选）
规则1: 限制API请求频率
- 表达式: (http.request.uri.path contains "/api/v1/auth/login")
- 动作: Rate Limit (5 requests per minute per IP)

规则2: 阻止常见攻击
- 表达式: (http.user_agent contains "sqlmap") or (http.user_agent contains "nikto")
- 动作: Block
```

### 2.2 DDoS Protection

```
Security → DDoS

【DDoS Protection】
✅ Automatic (默认启用,无需配置)

Cloudflare自动防御:
- L3/L4 DDoS攻击（网络层）
- L7 DDoS攻击（应用层）
- 每秒数百万请求
```

### 2.3 Bot Fight Mode

```
Security → Bots

【Bot Fight Mode】
✅ 启用

【Super Bot Fight Mode】(付费功能)
⚠️ 如果预算充足可升级

阻止:
- 恶意爬虫
- 暴力破解
- 垃圾邮件机器人
```

---

## 3. 防火墙规则配置

### 3.1 IP访问控制（可选）

```
Security → WAF → Custom rules

规则: 允许办公室IP
- 名称: Allow Office IPs
- 表达式: 
  (ip.src in {1.2.3.4 5.6.7.8}) 
  or (cf.client.bot = false)
- 动作: Allow

规则: 阻止已知恶意IP
- 名称: Block Bad IPs
- 表达式: (cf.threat_score > 50)
- 动作: Challenge (CAPTCHA)
```

### 3.2 地理位置限制（可选）

```
如果只服务中国用户:

规则: 仅允许中国访问
- 名称: China Only
- 表达式: (ip.geoip.country ne "CN")
- 动作: Challenge
```

---

## 4. Performance性能优化

### 4.1 Caching缓存策略

```
Caching → Configuration

【Caching Level】
选择: Standard

【Browser Cache TTL】
选择: 4 hours

【Cache Rules】
规则1: 静态资源缓存
- 表达式: 
  (http.request.uri.path contains ".jpg") or
  (http.request.uri.path contains ".png") or
  (http.request.uri.path contains ".css") or
  (http.request.uri.path contains ".js")
- 动作: Cache everything
- Edge TTL: 1 month

规则2: API不缓存
- 表达式: (http.request.uri.path contains "/api/")
- 动作: Bypass cache
```

### 4.2 Auto Minify（自动压缩）

```
Speed → Optimization

【Auto Minify】
✅ JavaScript
✅ CSS
✅ HTML
```

### 4.3 Brotli压缩

```
Speed → Optimization

【Brotli】
✅ 启用 - 比Gzip压缩率更高
```

---

## 5. Network网络配置

### 5.1 WebSocket支持

```
Network → WebSockets

【WebSockets】
✅ 启用

说明:
- Cloudflare默认支持WebSocket
- 自动升级ws://到wss://
- 无需额外配置
```

### 5.2 HTTP/2 & HTTP/3

```
Network

【HTTP/2】
✅ 启用 (默认)

【HTTP/3 (with QUIC)】
✅ 启用 - 更快的连接速度
```

---

## 6. 后端Nginx配置（源服务器）

### 6.1 Nginx SSL配置

```nginx
# /etc/nginx/sites-available/lanxin-im

server {
    listen 443 ssl http2;
    server_name api.lanxin168.com;

    # ✅ Cloudflare Origin证书
    ssl_certificate /etc/ssl/certs/lanxin_cert.pem;
    ssl_certificate_key /etc/ssl/private/lanxin_key.pem;

    # ✅ TLS配置
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # ✅ 信任Cloudflare IP
    set_real_ip_from 173.245.48.0/20;
    set_real_ip_from 103.21.244.0/22;
    set_real_ip_from 103.22.200.0/22;
    set_real_ip_from 103.31.4.0/22;
    set_real_ip_from 141.101.64.0/18;
    set_real_ip_from 108.162.192.0/18;
    set_real_ip_from 190.93.240.0/20;
    set_real_ip_from 188.114.96.0/20;
    set_real_ip_from 197.234.240.0/22;
    set_real_ip_from 198.41.128.0/17;
    set_real_ip_from 162.158.0.0/15;
    set_real_ip_from 104.16.0.0/13;
    set_real_ip_from 104.24.0.0/14;
    set_real_ip_from 172.64.0.0/13;
    set_real_ip_from 131.0.72.0/22;
    real_ip_header CF-Connecting-IP;

    # ✅ 安全header
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # ✅ API代理
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # ✅ WebSocket代理
    location /ws {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 86400;
    }
}

# ✅ HTTP→HTTPS重定向
server {
    listen 80;
    server_name api.lanxin168.com;
    return 301 https://$server_name$request_uri;
}
```

### 6.2 重启Nginx

```bash
# 测试配置
sudo nginx -t

# 重启
sudo systemctl restart nginx

# 验证
sudo systemctl status nginx
```

---

## 7. 测试验证清单

### 7.1 SSL/TLS测试

```bash
# 1. 测试HTTPS
curl -I https://api.lanxin168.com/health

期望输出:
HTTP/2 200
strict-transport-security: max-age=31536000; includeSubDomains; preload
cf-ray: xxx-xxx
server: cloudflare

# 2. 测试TLS版本
openssl s_client -connect api.lanxin168.com:443 -tls1_3

期望输出:
Protocol  : TLSv1.3
Cipher    : TLS_AES_256_GCM_SHA384

# 3. SSL Labs评分
访问: https://www.ssllabs.com/ssltest/analyze.html?d=api.lanxin168.com

期望评分: A+

# 4. 测试HSTS
curl -I https://api.lanxin168.com/health | grep -i strict

期望输出:
strict-transport-security: max-age=31536000; includeSubDomains; preload

# 5. 测试HTTP重定向
curl -I http://api.lanxin168.com/health

期望输出:
HTTP/1.1 301 Moved Permanently
Location: https://api.lanxin168.com/health
```

### 7.2 WebSocket测试

```javascript
// Chrome DevTools Console

// 测试wss://连接
const ws = new WebSocket('wss://api.lanxin168.com/ws?token=test_token');

ws.onopen = () => {
    console.log('✅ WSS连接成功');
    console.log('加密协议:', ws.protocol);
};

ws.onerror = (error) => {
    console.error('❌ 连接失败:', error);
};

ws.onmessage = (event) => {
    console.log('📩 收到消息:', event.data);
};

// 发送心跳
ws.send(JSON.stringify({
    type: 'ping',
    timestamp: Date.now()
}));
```

### 7.3 安全header测试

```bash
curl -I https://api.lanxin168.com/health

✅ 必须包含:
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
X-Frame-Options: DENY
X-Content-Type-Options: nosniff
X-XSS-Protection: 1; mode=block
CF-Ray: xxx-xxx  # Cloudflare标识

✅ 不应该包含:
Server: Apache/2.4.41  # 不暴露服务器版本
X-Powered-By: PHP/7.4  # 不暴露技术栈
```

### 7.4 DDoS防护测试

```bash
# 模拟高并发请求（慎用！）
# 注意: 仅在测试环境使用，不要攻击生产环境

ab -n 10000 -c 100 https://api.lanxin168.com/health

# Cloudflare会自动:
# 1. 检测异常流量
# 2. 触发Challenge
# 3. 限制请求速率
# 4. 显示5秒盾（Checking your browser...）
```

---

## 8. 监控和日志

### 8.1 Cloudflare Analytics

```
Analytics → Traffic

监控指标:
- Requests (请求数)
- Bandwidth (带宽)
- Unique Visitors (独立访客)
- Threats Blocked (阻止的威胁)
- Status Codes (状态码分布)

✅ 每天检查一次
```

### 8.2 Security Events

```
Security → Events

查看:
- WAF阻止的请求
- 触发的规则
- 威胁IP地址
- 攻击类型

✅ 出现异常时查看
```

### 8.3 API日志

```bash
# 后端服务器日志
tail -f /var/log/lanxin-im/app.log | grep -i error

# Nginx访问日志
tail -f /var/log/nginx/access.log

# Nginx错误日志
tail -f /var/log/nginx/error.log
```

---

## 9. 成本分析

### Cloudflare免费版 vs 付费版

| 功能 | 免费版 | Pro ($20/月) | Business ($200/月) |
|------|-------|-------------|-------------------|
| SSL/TLS | ✅ | ✅ | ✅ |
| DDoS防护 | ✅ 基础 | ✅ 增强 | ✅ 高级 |
| WAF | ✅ 基础规则 | ✅ 更多规则 | ✅ 全部规则 |
| 全球CDN | ✅ | ✅ | ✅ |
| 带宽 | 无限 | 无限 | 无限 |
| 缓存 | ✅ | ✅ | ✅ |
| Bot防护 | ⚠️ 基础 | ✅ 增强 | ✅ 超级 |
| 最低TTL | 2小时 | 1小时 | 30秒 |

**推荐**:
- **开发/测试**: 免费版 ✅
- **小型企业(<1000用户)**: 免费版 ✅
- **中型企业(<10000用户)**: Pro版
- **大型企业(>10000用户)**: Business版

---

## 10. 故障排查

### 10.1 常见问题

**问题1: SSL证书错误**
```
错误: ERR_SSL_PROTOCOL_ERROR

原因:
- Cloudflare设置为Full (strict)但源服务器没有有效证书

解决:
1. Cloudflare生成Origin Certificate
2. 安装到源服务器
3. 或改为Full模式（不推荐）
```

**问题2: WebSocket连接失败**
```
错误: WebSocket connection failed

原因:
- Network → WebSockets未启用
- Nginx未正确配置WebSocket代理

解决:
1. Cloudflare启用WebSockets
2. Nginx添加Upgrade头
3. 确认使用wss://而非ws://
```

**问题3: 真实IP获取失败**
```
问题: 后端获取的IP都是Cloudflare IP

原因:
- 未配置真实IP头

解决:
Nginx添加:
set_real_ip_from 173.245.48.0/20;
real_ip_header CF-Connecting-IP;
```

**问题4: 429 Too Many Requests**
```
错误: 429 Too Many Requests

原因:
- Rate Limit规则过于严格

解决:
Security → WAF → 调整Rate Limit阈值
```

---

## 11. 最佳实践

### ✅ DO (应该做)

1. **使用Full (strict)模式** - 最安全
2. **启用HSTS** - 防止降级攻击
3. **启用TLS 1.3** - 最新标准
4. **配置WAF规则** - 防止攻击
5. **监控Security Events** - 及时发现威胁
6. **定期更新Origin Certificate** - 保持有效
7. **使用wss://协议** - WebSocket加密
8. **配置真实IP头** - 正确获取客户端IP

### ❌ DON'T (不应该做)

1. **不要用Flexible模式** - 源服务器不加密
2. **不要禁用WAF** - 失去防护
3. **不要暴露服务器信息** - 隐藏Server头
4. **不要在生产环境用Development Mode** - 绕过缓存和安全
5. **不要忽略安全警告** - 及时处理
6. **不要用HTTP** - 全部HTTPS
7. **不要禁用Bot防护** - 防止恶意爬虫

---

## 12. 总结

### 安全等级评估

**传输安全**: ⭐⭐⭐⭐⭐ 5/5
- Cloudflare TLS 1.3
- 全球CDN加速
- 自动证书管理

**防护能力**: ⭐⭐⭐⭐⭐ 5/5
- DDoS自动防护
- WAF规则防护
- Bot攻击防护

**认证安全**: ⭐⭐⭐⭐⭐ 5/5
- JWT Token
- bcrypt密码
- Token黑名单

**存储安全**: ⭐⭐⭐☆☆ 3/5
- 明文存储（企业标准）
- 适合审计监管

### 综合评价

**蓝信IM + Cloudflare = 企业级安全标准 ✅**

与以下产品同级别:
- 微信企业版
- 钉钉
- 飞书
- Slack

**无需端到端加密**:
- 便于企业管理
- 符合审计要求
- 降低开发成本
- 保持消息可搜索

---

**文档版本**: 1.0  
**最后更新**: 2025-10-18  
**维护者**: 蓝信IM开发团队

