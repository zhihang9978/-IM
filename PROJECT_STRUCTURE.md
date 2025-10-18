# 📁 蓝信IM项目结构说明

**重要**: 请仔细阅读，避免混淆项目目录

---

## 🎯 核心项目目录

### ✅ apps/ - 真实项目代码（所有开发工作在这里）

```
apps/
├── backend/                    ← Go后端服务（主要修改）
│   ├── cmd/server/main.go     ← 服务入口
│   ├── internal/              ← 业务代码
│   │   ├── api/              ← API层（10个handler）
│   │   ├── dao/              ← 数据访问层（8个DAO）
│   │   ├── service/          ← 业务逻辑层（5个service）
│   │   ├── model/            ← 数据模型（8个model）
│   │   ├── middleware/       ← 中间件
│   │   ├── websocket/        ← WebSocket
│   │   └── pkg/              ← 工具包
│   ├── migrations/           ← 数据库迁移（15个）
│   ├── config/               ← 配置文件
│   └── pkg/                  ← 外部包（Kafka、COS等）
│
├── android/                    ← Kotlin Android客户端（主要修改）
│   ├── app/src/main/java/com/lanxin/im/
│   │   ├── data/             ← 数据层
│   │   │   ├── local/       ← 本地数据库（Room）
│   │   │   ├── remote/      ← 网络API（Retrofit）
│   │   │   ├── model/       ← 数据模型
│   │   │   └── repository/  ← 数据仓库
│   │   ├── ui/               ← UI层（MVVM）
│   │   │   ├── chat/        ← 聊天界面
│   │   │   ├── conversation/← 会话列表
│   │   │   ├── contact/     ← 联系人
│   │   │   └── ...          ← 其他界面
│   │   ├── trtc/             ← 音视频通话
│   │   └── utils/            ← 工具类
│   ├── app/src/main/res/     ← 资源文件
│   └── build.gradle.kts      ← Gradle配置
│
└── admin-web/                  ← React管理后台
    ├── src/                   ← React组件
    └── package.json          ← NPM依赖
```

---

## ⚠️ 非项目目录（仅供参考）

### ❌ android-chat-master/ - 资源提取仓库（不要修改）

```
android-chat-master/           ← ⚠️ 不是实际项目！
├── uikit/                     ← UI组件库（参考）
├── emojilibrary/              ← 表情库（参考）
├── imagepicker/               ← 图片选择器（参考）
├── cameraview/                ← 相机组件（参考）
└── ...                        ← 其他UI组件

用途：
- 提取图标、UI组件等资源
- 参考UI设计和实现
- 不参与实际编译和运行

⚠️ 警告：
- 不要在这个目录修改代码
- 不要将这个目录当作Android项目
- 实际Android项目在 apps/android/
```

### ❌ temp-wildfire-ui/ - 临时文件（忽略）

```
temp-wildfire-ui/              ← 临时备份
└── (android-chat-master的副本)

用途：
- 临时备份
- 可以删除

⚠️ 不是项目代码
```

---

## 📋 阶段5修改的正确文件

### ✅ 已正确修改的Android文件（3个）

所有修改都在 **apps/android/** 目录下：

1. **apps/android/app/src/main/java/com/lanxin/im/data/remote/ApiService.kt**
   - ✅ 添加9个群组API定义
   - ✅ 添加1个离线消息API定义
   - ✅ 添加所有数据类

2. **apps/android/app/src/main/java/com/lanxin/im/data/remote/WebSocketClient.kt**
   - ✅ 添加离线消息拉取逻辑
   - ✅ 修改构造函数支持依赖注入
   - ✅ 添加fetchOfflineMessages方法

3. **apps/android/app/src/main/java/com/lanxin/im/ui/chat/ChatViewModel.kt**
   - ✅ 添加消息去重逻辑
   - ✅ 防止重复显示

**验证**: ✅ 所有修改在正确位置，无lint错误

---

## 🎯 项目编译和运行

### 后端（apps/backend）

```bash
cd apps/backend

# 编译
go build -o lanxin-im cmd/server/main.go

# 运行
./lanxin-im

# 或直接运行
go run cmd/server/main.go
```

### Android（apps/android）

```bash
cd apps/android

# 编译
./gradlew assembleDebug

# 生成APK路径
# app/build/outputs/apk/debug/app-debug.apk
```

### 管理后台（apps/admin-web）

```bash
cd apps/admin-web

# 安装依赖
npm install

# 运行
npm run dev
```

---

## 📊 项目统计

### apps/backend（Go后端）

```
文件统计:
- API层: 10个handler
- Service层: 5个service
- DAO层: 8个DAO
- Model层: 8个model
- 迁移文件: 15个（up）+ 11个（down）

代码行数:
- 总计: ~15,000行
- 本次新增: ~2,500行
- 本次修改: ~600行
```

### apps/android（Kotlin客户端）

```
文件统计:
- 总文件: ~60个Kotlin文件
- Activity/Fragment: ~15个
- ViewModel: ~8个
- Repository: ~3个
- DAO: ~5个

代码行数:
- 总计: ~8,000行
- 本次新增: ~150行
- 本次修改: ~50行
```

### apps/admin-web（React后台）

```
文件统计:
- 组件: ~15个
- 页面: ~10个
- 服务: ~3个

代码行数:
- 总计: ~3,000行
- 未修改
```

---

## ⚠️ 重要提醒

### 不要混淆的目录

| 目录 | 用途 | 是否修改 |
|------|------|---------|
| **apps/android/** | ✅ 真实Android项目 | ✅ 是 |
| **android-chat-master/** | ❌ 仅供参考 | ❌ 否 |
| **apps/backend/** | ✅ 真实后端项目 | ✅ 是 |
| **temp-wildfire-ui/** | ❌ 临时文件 | ❌ 否 |

### 开发时的正确做法

**✅ 正确**:
```bash
# 修改Android代码
cd apps/android
code .

# 修改后端代码
cd apps/backend
code .
```

**❌ 错误**:
```bash
# ❌ 不要在这里改
cd android-chat-master
code .

# ❌ 不要在这里改
cd temp-wildfire-ui
code .
```

---

## 📦 Git仓库结构

### 版本控制的目录

```
✅ apps/backend/          ← 后端代码，已推送
✅ apps/android/          ← Android代码，已推送
✅ apps/admin-web/        ← 管理后台，已推送
✅ *.md 文档              ← 所有文档，已推送
⚠️ android-chat-master/  ← 参考库，未推送（.gitignore）
⚠️ temp-wildfire-ui/     ← 临时文件，未推送
```

---

## 🎯 Devin部署时的正确路径

### 在服务器上

**后端部署路径**:
```bash
/var/www/im-lanxin/apps/backend/        ← ✅ 正确
/var/www/im-lanxin/android-chat-master/ ← ❌ 错误（这不是项目）
```

**编译和运行**:
```bash
# ✅ 正确
cd /var/www/im-lanxin/apps/backend
go build cmd/server/main.go

# ❌ 错误
cd /var/www/im-lanxin/android-chat-master
# 这里没有Go代码！
```

---

## 📝 项目架构总结

### 三个子项目

1. **apps/backend** - Go后端
   - 技术栈: Go + Gin + GORM + Redis + Kafka
   - 功能: 45个API，WebSocket，音视频
   - 状态: ✅ 已修复并部署

2. **apps/android** - Kotlin客户端
   - 技术栈: Kotlin + MVVM + Retrofit + Room
   - 功能: IM聊天，群组，通话
   - 状态: ✅ 已修改并推送

3. **apps/admin-web** - React管理后台
   - 技术栈: React + TypeScript + Vite
   - 功能: 用户管理，数据统计
   - 状态: ✅ 未修改（功能完整）

### 辅助目录

- **android-chat-master/** - UI资源参考库（不参与编译）
- **temp-wildfire-ui/** - 临时备份（可删除）
- **packages/** - Monorepo配置（未使用）

---

## 🎉 总结

### ✅ 项目结构清晰

**实际项目**（3个）:
- ✅ apps/backend - 后端
- ✅ apps/android - Android
- ✅ apps/admin-web - 管理后台

**参考资源**（2个）:
- ⚠️ android-chat-master - UI参考
- ⚠️ temp-wildfire-ui - 临时备份

### ✅ 所有修改在正确位置

本次修复涉及的所有文件都在 `apps/` 目录下：
- ✅ 后端21个文件（apps/backend/）
- ✅ Android 3个文件（apps/android/）
- ✅ 无误修改其他目录

### ✅ 项目已正确部署

Devin在服务器上部署的是 `apps/backend/`，这是正确的！

---

**文档版本**: 1.0  
**创建时间**: 2025-10-18  
**用途**: 明确项目结构，避免目录混淆

