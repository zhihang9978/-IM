# 📱 TRTC SDK手动配置方案

**问题**: TRTC SDK无法从Maven自动下载  
**解决**: 手动下载AAR文件并本地引入  
**时间**: 10分钟

---

## 🎯 方案：使用本地AAR文件

### Step 1: 创建libs目录（1分钟）

```bash
# 在Devin的本地开发机器执行
cd /path/to/your/local/im-lanxin/apps/android/app

# 创建libs目录
mkdir -p libs

# 验证
ls -la libs
```

---

### Step 2: 下载TRTC SDK AAR文件（3分钟）

#### 方式1: 从腾讯云官网下载（推荐）

访问腾讯云TRTC SDK下载页面：
- 官方地址：https://cloud.tencent.com/document/product/647/32689
- 或GitHub：https://github.com/LiteAVSDK/TRTC_Android

下载文件：
- SDK类型：**LiteAVSDK_TRTC**（精简版，仅音视频通话）
- 版本：**11.5.0** 或最新稳定版
- 文件：`LiteAVSDK_TRTC_11.5.0.aar`（约30-40MB）

#### 方式2: 使用直接下载链接

```bash
# 腾讯云CDN直接下载（如果可用）
cd apps/android/app/libs

# 下载TRTC SDK（版本11.5或最新）
wget https://liteavsdk-1252463788.cosgz.myqcloud.com/TXLiteAVSDK_TRTC_Android_latest.zip

# 解压
unzip TXLiteAVSDK_TRTC_Android_latest.zip

# 找到AAR文件
find . -name "*.aar"
# 应该有: LiteAVSDK_TRTC_xxx.aar

# 重命名为标准名称
mv LiteAVSDK_TRTC_*.aar LiteAVSDK_TRTC_11.5.0.aar

# 清理
rm TXLiteAVSDK_TRTC_Android_latest.zip
```

#### 方式3: 从其他Maven仓库下载

```bash
# 访问Maven中央仓库搜索
# https://search.maven.org/
# 搜索: com.tencent.liteav

# 或使用Maven命令下载
mvn dependency:get \
  -Dartifact=com.tencent.liteav:LiteAVSDK_TRTC:11.5.0:aar \
  -Ddest=apps/android/app/libs/LiteAVSDK_TRTC_11.5.0.aar
```

---

### Step 3: 修改build.gradle.kts使用本地AAR（3分钟）

**文件**: `apps/android/app/build.gradle.kts`

#### 修改A: 修改dependencies部分

找到：
```kotlin
dependencies {
    // ...
    
    // 腾讯云 TRTC SDK（音视频通话）- 必须保留
    implementation("com.tencent.liteav:LiteAVSDK_TRTC:11.5.0")
    
    // ...
}
```

替换为：
```kotlin
dependencies {
    // ...
    
    // 腾讯云 TRTC SDK（音视频通话）- 使用本地AAR文件
    implementation(files("libs/LiteAVSDK_TRTC_11.5.0.aar"))
    
    // ...
}
```

#### 修改B: 或使用fileTree（如果有多个AAR）

```kotlin
dependencies {
    // ...
    
    // 腾讯云 TRTC SDK - 从libs目录加载所有AAR
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
    
    // ...
}
```

---

### Step 4: 同步和构建（3分钟）

```bash
cd apps/android

# 清理
./gradlew clean

# 构建
./gradlew assembleDebug

# 期望输出:
# > Task :app:assembleDebug
# BUILD SUCCESSFUL in 5m 32s
```

---

## 🔄 备选方案：使用腾讯云官方仓库地址

如果手动下载不方便，尝试这些Maven仓库地址：

### 修改settings.gradle.kts

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        
        // 尝试这些腾讯云仓库（逐个测试）
        maven { url = uri("https://mirrors.tencent.com/nexus/repository/maven-public/") }
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        maven { url = uri("https://sdk.cloudbase.net/android/") }
        
        // JCenter（已废弃但可能还有缓存）
        maven { url = uri("https://jcenter.bintray.com") }
    }
}
```

然后重新尝试：
```bash
./gradlew clean --refresh-dependencies
./gradlew assembleDebug
```

---

## 📦 最终推荐方案

### 方案选择建议

**如果Devin有良好的网络**:
- ✅ 尝试添加多个Maven仓库地址
- ✅ 使用 `--refresh-dependencies` 强制刷新

**如果网络不好或仍然失败**:
- ✅ 手动下载AAR文件（30-40MB）
- ✅ 放到libs目录
- ✅ 修改build.gradle使用本地文件

### 推荐：方案A（手动下载AAR）

**理由**:
1. 一次性下载，后续构建快速
2. 不依赖外部网络
3. 版本可控
4. 构建稳定

**步骤**:
1. 创建 `apps/android/app/libs/` 目录
2. 下载 `LiteAVSDK_TRTC_11.5.0.aar`（或最新版）
3. 修改build.gradle.kts使用本地AAR
4. 构建成功

---

## 📝 给Devin的明确指示

### 推荐执行（手动AAR方案）

```bash
# 1. 创建libs目录
cd /path/to/im-lanxin/apps/android/app
mkdir -p libs

# 2. 下载TRTC SDK
# 访问: https://cloud.tencent.com/document/product/647/32689
# 下载: LiteAVSDK_TRTC（Android）最新版
# 找到AAR文件，复制到libs目录

# 或使用命令（如果有直接链接）
cd libs
wget [TRTC_SDK_DOWNLOAD_URL]
# 重命名为: LiteAVSDK_TRTC_11.5.0.aar

# 3. 返回项目根目录
cd ../../..

# 4. 修改build.gradle.kts（见Step 3）

# 5. 构建
cd apps/android
./gradlew clean
./gradlew assembleDebug
```

---

## ✅ 构建成功标志

```
✅ TRTC SDK AAR文件在libs目录
✅ build.gradle.kts已修改使用本地AAR
✅ BUILD SUCCESSFUL
✅ APK生成（20-35MB）
```

---

## 🆘 如果找不到下载地址

Devin可以：
1. 访问腾讯云TRTC产品页
2. 查看"SDK下载"或"快速开始"
3. 选择Android平台
4. 下载精简版SDK（LiteAVSDK_TRTC）
5. 解压后找到AAR文件

或者，联系我们提供AAR文件的直接下载链接。

---

**文档版本**: 1.0  
**创建时间**: 2025-10-18  
**用途**: 解决TRTC SDK无法从Maven下载的问题

