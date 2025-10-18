# 📱 Devin - Android APK构建指南

**任务**: 在你的**本地开发环境**构建Android APK  
**注意**: ⚠️ 不是在服务器上，是在你的本地机器上  
**目的**: 让用户测试Android客户端是否完善可用

---

## 🎯 任务概述

1. 删除旧的构建文件
2. 拉取最新的Android代码
3. 配置服务器地址
4. 构建APK
5. 提供APK给用户测试

**预计时间**: 15-20分钟  
**位置**: 你的本地开发机器（Windows/Mac/Linux）

---

## 📋 执行步骤

### Step 1: 在本地环境准备（5分钟）

```bash
# ⚠️ 在你的本地开发机器执行（不是服务器！）

# 进入项目目录（如果还没clone）
# 如果已有项目，直接进入
cd /path/to/your/local/im-lanxin

# 如果是第一次，先clone
# git clone https://github.com/zhihang9978/-IM.git im-lanxin
# cd im-lanxin
```

---

### Step 2: 删除旧的Android构建文件（2分钟）

```bash
# ⚠️ 确保你在项目根目录 im-lanxin/

# 删除旧的构建文件
cd apps/android
rm -rf app/build
rm -rf build
rm -rf .gradle

# 或者Windows上使用PowerShell:
# Remove-Item -Recurse -Force app/build, build, .gradle -ErrorAction SilentlyContinue

# 返回项目根目录
cd ../..
```

**验证**:
- [ ] apps/android/app/build/ 目录已删除
- [ ] apps/android/build/ 目录已删除
- [ ] apps/android/.gradle/ 目录已删除

---

### Step 3: 拉取最新代码（1分钟）

```bash
# 在项目根目录执行
git fetch origin
git pull origin master

# 验证版本
git log --oneline -3

# 应该看到最新的commit:
# a115463 docs: add final project status and structure clarification
# 026752b docs: final project status report - production ready!
# 8a1e602 fix: add group conversation auto-creation for group messages
```

**验证**:
- [ ] Git拉取成功
- [ ] 最新commit是 a115463 或更新
- [ ] apps/android/ 目录存在
- [ ] apps/android/app/src/main/java/com/lanxin/im/ 目录存在

---

### Step 4: 配置服务器地址（3分钟）⭐重要

#### 文件1: 配置API服务器地址

**文件**: `apps/android/app/src/main/java/com/lanxin/im/data/remote/RetrofitClient.kt`

查找并修改BASE_URL：

```kotlin
// 找到这一行
private const val BASE_URL = "https://api.lanxin168.com/api/v1/"

// 修改为生产服务器地址
private const val BASE_URL = "http://154.40.45.121:8080/api/v1/"
```

#### 文件2: 配置WebSocket地址

**文件**: `apps/android/app/src/main/java/com/lanxin/im/data/remote/WebSocketClient.kt`

查找并修改WS_URL：

```kotlin
// 找到这一行
private const val WS_URL = "wss://api.lanxin168.com/ws"

// 修改为生产服务器地址
private const val WS_URL = "ws://154.40.45.121:8080/ws"
```

**⚠️ 重要**:
- 注意是 `http://`（不是https）
- 注意是 `ws://`（不是wss）
- 端口是 `8080`
- IP是 `154.40.45.121`

**验证**:
- [ ] RetrofitClient.kt 已修改
- [ ] WebSocketClient.kt 已修改
- [ ] 地址格式正确（http://IP:8080）

---

### Step 5: 检查构建环境（2分钟）

```bash
# 检查JDK版本
java -version
# 期望: openjdk version "17" 或更高

# 如果没有JDK 17，需要安装
# Ubuntu/Debian:
# sudo apt install openjdk-17-jdk

# macOS:
# brew install openjdk@17

# Windows:
# 从 https://adoptium.net/ 下载安装

# 检查Android SDK（如果使用命令行）
echo $ANDROID_HOME
# 应该有值，如: /Users/you/Library/Android/sdk
```

**验证**:
- [ ] Java版本 >= 17
- [ ] ANDROID_HOME已设置（如果用命令行）

---

### Step 6: 构建APK（5分钟）⭐

```bash
# 进入Android目录
cd apps/android

# 给gradlew执行权限（Linux/Mac）
chmod +x gradlew

# 清理旧构建
./gradlew clean

# 构建Debug APK
./gradlew assembleDebug

# Windows上使用:
# gradlew.bat clean
# gradlew.bat assembleDebug
```

**构建过程**（预计3-5分钟）:
```
> Task :app:preBuild
> Task :app:compileDebugKotlin
> Task :app:mergeDebugResources
> Task :app:processDebugManifest
> Task :app:compileDebugJavaWithJavac
> Task :app:dexBuilderDebug
> Task :app:mergeDebugNativeLibs
> Task :app:packageDebug
> Task :app:assembleDebug

BUILD SUCCESSFUL in 3m 42s
```

**验证**:
- [ ] 看到 "BUILD SUCCESSFUL"
- [ ] 无编译错误
- [ ] 无严重警告

---

### Step 7: 找到生成的APK（1分钟）

```bash
# APK路径
ls -lh app/build/outputs/apk/debug/

# 应该看到:
# app-debug.apk

# 查看APK大小
ls -lh app/build/outputs/apk/debug/app-debug.apk

# 期望: 大小约 15-30 MB

# 完整路径
pwd
# 例如: /Users/devin/projects/im-lanxin/apps/android

# APK完整路径:
# /Users/devin/projects/im-lanxin/apps/android/app/build/outputs/apk/debug/app-debug.apk
```

**验证**:
- [ ] APK文件存在
- [ ] 文件大小 > 10MB
- [ ] 文件扩展名是 .apk

---

### Step 8: 提供APK给用户（2分钟）

#### 方式1: 上传到文件分享服务

```bash
# 使用任何文件传输方式:
# - Google Drive
# - Dropbox
# - WeTransfer
# - 百度网盘
# - 或直接发送文件（如果<50MB）

# APK位置:
# apps/android/app/build/outputs/apk/debug/app-debug.apk
```

#### 方式2: 直接在服务器上托管

```bash
# 复制APK到服务器的静态文件目录
scp apps/android/app/build/outputs/apk/debug/app-debug.apk \
    user@154.40.45.121:/var/www/downloads/lanxin-im-v1.0.apk

# 然后用户可以下载:
# http://154.40.45.121/downloads/lanxin-im-v1.0.apk
```

---

## 📦 APK信息

**文件名**: app-debug.apk  
**版本**: 1.0 (Debug版)  
**包名**: com.lanxin.im  
**最小SDK**: API 26 (Android 8.0)  
**目标SDK**: API 34 (Android 14)  
**大小**: 约15-30MB  
**签名**: Debug签名（开发测试用）

---

## 🧪 APK功能清单（用户将测试）

### 基础功能
- [ ] APP能安装
- [ ] APP能启动
- [ ] 能看到登录界面

### 登录注册
- [ ] 能使用testuser1登录
- [ ] 能记住登录状态

### 单聊功能（核心）
- [ ] 能发送文本消息
- [ ] 能接收实时消息
- [ ] 会话列表能显示
- [ ] 能加载历史消息
- [ ] 消息不重复显示

### 群聊功能（新增）
- [ ] 能创建群组
- [ ] 能查看群成员
- [ ] 能发送群消息
- [ ] 能接收群消息

### 离线消息（新增）
- [ ] 关闭APP后发送消息
- [ ] 重新打开APP
- [ ] 自动拉取离线消息
- [ ] 消息正确显示

### 其他功能
- [ ] WebSocket连接正常
- [ ] 消息状态更新（已读/已送达）
- [ ] 联系人功能
- [ ] 文件发送（如果支持）

---

## ⚠️ 常见问题

### 问题1: gradlew: command not found

```bash
# 确保在 apps/android 目录
pwd
# 应该显示: .../im-lanxin/apps/android

# 给权限
chmod +x gradlew

# 重新执行
./gradlew assembleDebug
```

### 问题2: SDK not found

```bash
# 设置ANDROID_HOME环境变量
export ANDROID_HOME=/path/to/android/sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools

# 或在 local.properties 中配置
echo "sdk.dir=/path/to/android/sdk" > local.properties
```

### 问题3: JDK版本不对

```bash
# 检查版本
java -version

# 如果<17，需要升级
# 推荐使用 JDK 17
```

### 问题4: 编译错误

```bash
# 清理后重试
./gradlew clean
./gradlew assembleDebug --stacktrace

# 查看详细错误
```

### 问题5: 缺少依赖

```bash
# 下载依赖
./gradlew --refresh-dependencies
```

---

## 📊 构建成功标志

### 终端输出
```
✅ BUILD SUCCESSFUL in 3m 42s
✅ 47 actionable tasks: 47 executed
```

### 文件存在
```
✅ app/build/outputs/apk/debug/app-debug.apk
✅ 文件大小: 15-30 MB
```

### APK可安装
```
✅ adb install app/build/outputs/apk/debug/app-debug.apk
   或
✅ 传输到手机直接安装
```

---

## 📝 提供给用户的信息

当APK构建完成后，告诉用户：

```
✅ Android APK已构建完成

APK信息:
- 版本: 1.0 (Debug)
- 大小: XX MB
- 包名: com.lanxin.im
- 服务器: 154.40.45.121:8080

测试账号:
- testuser1 / password123
- testuser2 / password123
- testuser3 / password123

下载地址:
[提供下载链接或发送文件]

请测试以下功能:
1. 登录是否正常
2. 单聊是否可用（发送/接收消息）
3. 会话列表是否显示
4. 群聊是否可用（创建群/发送群消息）
5. 离线消息是否能拉取
6. 消息是否去重（不重复显示）

如有问题，请提供:
- 具体操作步骤
- 错误截图
- Logcat日志
```

---

## 🔧 如果需要生成Release APK（可选）

```bash
# 生成Release APK（用于正式发布）
./gradlew assembleRelease

# 需要签名配置，如果没有配置会失败
# Release APK路径:
# app/build/outputs/apk/release/app-release-unsigned.apk
```

**注意**: Debug APK足够测试使用，Release APK用于正式发布。

---

## 📱 APK安装测试（Devin自己测试）

### 使用模拟器测试（推荐）

```bash
# 启动Android模拟器（如果有）
# Android Studio -> AVD Manager -> 启动模拟器

# 安装APK
adb install app/build/outputs/apk/debug/app-debug.apk

# 期望: Success

# 启动APP
adb shell am start -n com.lanxin.im/.MainActivity

# 查看日志
adb logcat | grep -i lanxin
```

### 使用真机测试

```bash
# 连接手机，开启USB调试

# 检查设备
adb devices
# 应该看到你的设备

# 安装APK
adb install app/build/outputs/apk/debug/app-debug.apk

# 在手机上打开APP测试
```

### 快速功能测试

1. **登录**: 使用 testuser1 / password123
2. **发消息**: 给testuser2发送"测试"
3. **检查会话**: 会话列表应该显示
4. **检查实时**: 消息应该实时推送（如果testuser2在线）

---

## ✅ 构建完成检查清单

- [ ] 本地环境已配置（JDK 17, Android SDK）
- [ ] 旧构建文件已删除
- [ ] 代码已拉取到最新（commit a115463）
- [ ] 服务器地址已配置（154.40.45.121:8080）
- [ ] APK构建成功（BUILD SUCCESSFUL）
- [ ] APK文件存在且大小正常（>10MB）
- [ ] （可选）在模拟器/真机上测试成功
- [ ] APK已提供给用户

---

## 📊 项目Android客户端信息

### 正确的Android项目位置

```
✅ apps/android/              ← 这是实际项目
   ├── app/
   │   ├── src/main/java/com/lanxin/im/
   │   │   ├── data/         ← 我们修改的位置
   │   │   │   ├── remote/
   │   │   │   │   ├── ApiService.kt      ✅ 已修改
   │   │   │   │   ├── WebSocketClient.kt ✅ 已修改
   │   │   │   └── ...
   │   │   ├── ui/chat/
   │   │   │   ├── ChatViewModel.kt       ✅ 已修改
   │   │   └── ...
   │   └── build.gradle.kts
   └── build.gradle.kts

❌ android-chat-master/       ← 这不是项目，只是资源参考
   └── 仅供参考，不编译
```

### 修改的文件（阶段5）

所有修改都在 `apps/android/` 下：

1. **ApiService.kt** (+120行)
   - 添加9个群组API
   - 添加1个离线消息API
   - 添加7个数据类

2. **WebSocketClient.kt** (+35行)
   - 添加离线消息拉取
   - 添加依赖注入支持

3. **ChatViewModel.kt** (+8行)
   - 添加消息去重逻辑

**总新增代码**: ~160行

---

## 🎯 给用户的测试建议

当APK提供给用户后，建议测试：

### 核心功能测试（必测）

1. **登录注册**
   - 使用 testuser1 / password123 登录
   - 验证能成功进入主界面

2. **单聊功能**（阶段1修复验证）
   - 发送消息给 testuser2
   - 检查会话列表是否显示
   - 检查消息是否正确显示

3. **群聊功能**（阶段2新功能验证）
   - 创建新群组（添加testuser2、testuser3）
   - 在群里发送消息
   - 检查群成员是否正确

4. **离线消息**（阶段4新功能验证）
   - 关闭APP
   - 让另一个账号发消息
   - 重新打开APP
   - 检查是否自动拉取到消息

5. **消息去重**（阶段5优化验证）
   - 发送消息
   - 检查消息是否只显示一次（不重复）

### 性能测试（建议）

- [ ] APP启动速度
- [ ] 消息发送速度
- [ ] 图片加载速度
- [ ] 内存占用
- [ ] 电池消耗

### UI/UX测试（建议）

- [ ] 界面是否美观
- [ ] 操作是否流畅
- [ ] 提示信息是否清晰
- [ ] 错误处理是否友好

---

## 📞 如遇问题

### 编译问题
1. 检查JDK版本（必须>=17）
2. 检查网络（下载依赖）
3. 删除.gradle重试
4. 查看完整错误日志

### 配置问题
1. 检查服务器地址是否正确
2. 检查端口是否正确
3. 检查是http不是https
4. 检查是ws不是wss

### 安装问题
1. 手机需要开启"允许安装未知来源"
2. 如果提示签名错误，卸载旧版本
3. 如果安装失败，检查手机存储空间

---

## 🎯 成功标志

### Devin完成的标志
```
✅ 本地环境构建成功
✅ APK文件生成
✅ APK大小正常（15-30MB）
✅ （可选）在模拟器测试通过
✅ APK已提供给用户
```

### 用户测试通过的标志
```
✅ APP能安装启动
✅ 能登录
✅ 单聊功能正常
✅ 群聊功能正常
✅ 离线消息正常
✅ 无崩溃，无严重bug
```

---

## 📋 总结

**Devin的任务**:
1. 在**本地开发机器**（不是服务器）
2. 拉取最新代码（apps/android）
3. 配置服务器地址（154.40.45.121:8080）
4. 构建Debug APK
5. 提供APK给用户测试

**用户的任务**:
1. 安装APK
2. 测试所有功能
3. 反馈问题（如果有）
4. 确认客户端是否完善可用

**预计时间**: 
- Devin构建: 15-20分钟
- 用户测试: 30-60分钟

---

**文档版本**: 1.0  
**创建时间**: 2025-10-18  
**适用环境**: Devin的本地开发机器  
**目标**: 构建Android APK供用户测试

