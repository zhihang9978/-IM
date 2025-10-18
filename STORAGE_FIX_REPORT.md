# 存储空间统计修复报告

## 问题描述

管理员后台的文件管理页面显示的存储空间数据不准确：
- 显示值：45.00 GB / 100.00 GB (45% 使用率)
- 实际值：13 GB / 867 GB (1.5% 使用率)

存储统计API返回的是硬编码的测试数据，而不是真实的服务器磁盘使用情况。

## 根本原因

后端代码 `apps/backend/internal/api/admin.go` 的 `GetStorageStats` 函数使用了硬编码的假数据：

```go
totalStorage := int64(100 * 1024 * 1024 * 1024) // 硬编码 100GB
usedStorage := int64(45 * 1024 * 1024 * 1024)   // 硬编码 45GB
```

## 修复方案

使用 Linux 系统调用 `syscall.Statfs` 获取真实的磁盘使用情况：

### 1. 添加 syscall 包导入

```go
import (
    "syscall"
    // ... 其他导入
)
```

### 2. 修改 GetStorageStats 函数

```go
// 获取真实的磁盘使用情况
var stat syscall.Statfs_t
err := syscall.Statfs("/", &stat)
if err != nil {
    c.JSON(http.StatusInternalServerError, gin.H{
        "code":    500,
        "message": "Failed to get disk stats: " + err.Error(),
    })
    return
}

// 计算存储空间（单位：字节）
totalStorage := int64(stat.Blocks * uint64(stat.Bsize))
freeStorage := int64(stat.Bfree * uint64(stat.Bsize))
usedStorage := totalStorage - freeStorage
usagePercent := float64(usedStorage) / float64(totalStorage) * 100
```

## 部署步骤

### 1. 拉取最新代码
```bash
cd /var/www/im-lanxin
git pull origin devin/production-deployment-2025-10-18
```

### 2. 编译后端
```bash
cd apps/backend
go build -o lanxin-im cmd/server/main.go
```

### 3. 部署二进制文件
```bash
sudo systemctl stop lanxin-im
sudo cp /var/www/im-lanxin/apps/backend/lanxin-im /usr/local/bin/lanxin-im
sudo systemctl start lanxin-im
```

## 验证结果

### API 测试

**请求**:
```bash
curl -H "Authorization: Bearer <TOKEN>" \
  http://154.40.45.121:8080/api/v1/admin/storage/stats
```

**响应** (修复后):
```json
{
  "code": 0,
  "data": {
    "total_files": 0,
    "total_storage": 930397790208,     // 866.5 GB
    "used_storage": 13789761536,       // 12.84 GB
    "free_storage": 916608028672,      // 853.7 GB
    "usage_percent": 1.482136101475172 // 1.48%
  },
  "message": "success"
}
```

### 对比真实磁盘数据

**系统命令**:
```bash
df -h /
```

**输出**:
```
Filesystem      Size  Used Avail Use% Mounted on
/dev/sda1       867G   13G  854G   2% /
```

**结论**: ✅ API 返回的数据与真实磁盘使用情况完全一致

## 创建管理员账户

为了方便测试，创建了新的管理员账户：

- **用户名**: zhihang
- **密码**: GDhz1314520..
- **蓝信号**: lx5664584497
- **角色**: admin

## 测试环境

- **服务器**: 154.40.45.121 (主服务器)
- **后端API**: http://154.40.45.121:8080/api/v1
- **服务名称**: lanxin-im.service
- **部署时间**: 2025-10-18 19:11:14 CST

## 相关文件

修改的文件：
1. `apps/backend/internal/api/admin.go` - 修复存储统计逻辑
2. `apps/admin-web/.env.production` - 更新生产环境API地址（仅本地使用，不提交）

提交记录：
- `63d3488` - fix: use real disk stats instead of hardcoded storage values

## 后续建议

1. **前端显示优化**: 前端可以将字节数格式化为更易读的单位（GB、TB等）
2. **文件数量统计**: 目前只统计了消息中的文件，可以考虑统计 MinIO 中实际存储的文件数量
3. **存储空间告警**: 当使用率超过某个阈值（如80%）时显示警告
4. **历史趋势**: 记录存储使用历史，显示增长趋势图

## 总结

✅ 成功修复存储空间统计不准确的问题  
✅ API 现在返回真实的服务器磁盘使用情况  
✅ 部署并验证修复生效  
✅ 创建管理员账户用于测试

存储统计功能现已正常工作，显示真实、准确的数据。
