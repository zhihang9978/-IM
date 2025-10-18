# 阶段5: Android客户端集成
## 单一目标 - 前端对接 - 完整测试

**阶段编号**: Phase 5 of 5  
**预计时间**: 1小时  
**前置条件**: 阶段1-4全部完成  
**成功标准**: Android能正常使用所有后端功能

---

# 🎯 本阶段唯一目标

**集成**: Android客户端对接新增的后端功能

**需要添加**:
1. 群组API定义
2. 离线消息拉取
3. 消息去重逻辑
4. 上线自动拉取

---

# 📋 修复步骤

## Step 1: 添加群组API定义 (20分钟)

### 文件: `apps/android/app/src/main/java/com/lanxin/im/data/remote/ApiService.kt`

### 操作: 在文件末尾添加

在`interface ApiService { }` 的最后一个方法后面添加:

```kotlin
    // ==================== 群组模块 ====================
    
    @POST("groups")
    suspend fun createGroup(@Body request: CreateGroupRequest): ApiResponse<GroupResponse>
    
    @GET("groups/{id}")
    suspend fun getGroupInfo(@Path("id") groupId: Long): ApiResponse<GroupResponse>
    
    @GET("groups/{id}/members")
    suspend fun getGroupMembers(@Path("id") groupId: Long): ApiResponse<GroupMembersResponse>
    
    @POST("groups/{id}/members")
    suspend fun addGroupMembers(
        @Path("id") groupId: Long,
        @Body request: Map<String, List<Long>>
    ): ApiResponse<Any?>
    
    @DELETE("groups/{id}/members/{user_id}")
    suspend fun removeGroupMember(
        @Path("id") groupId: Long,
        @Path("user_id") userId: Long
    ): ApiResponse<Any?>
    
    @POST("groups/{id}/messages")
    suspend fun sendGroupMessage(
        @Path("id") groupId: Long,
        @Body request: SendMessageRequest
    ): ApiResponse<MessageResponse>
    
    @PUT("groups/{id}")
    suspend fun updateGroup(
        @Path("id") groupId: Long,
        @Body request: UpdateGroupRequest
    ): ApiResponse<Any?>
    
    @DELETE("groups/{id}")
    suspend fun disbandGroup(@Path("id") groupId: Long): ApiResponse<Any?>
    
    // ==================== 离线消息 ====================
    
    @GET("messages/offline")
    suspend fun getOfflineMessages(): ApiResponse<OfflineMessagesResponse>
}

// 在文件末尾添加数据类

// ==================== 群组请求数据类 ====================

data class CreateGroupRequest(
    val name: String,
    val avatar: String?,
    val member_ids: List<Long>
)

data class UpdateGroupRequest(
    val name: String?,
    val avatar: String?
)

// ==================== 群组响应数据类 ====================

data class GroupResponse(
    val group: Group
)

data class Group(
    val id: Long,
    val name: String,
    val avatar: String?,
    val owner_id: Long,
    val type: String,
    val member_count: Int,
    val status: String,
    val created_at: Long
)

data class GroupMembersResponse(
    val members: List<GroupMemberItem>
)

data class GroupMemberItem(
    val id: Long,
    val group_id: Long,
    val user_id: Long,
    val role: String,
    val nickname: String?,
    val joined_at: Long,
    val user: User?
)

data class OfflineMessagesResponse(
    val messages: List<Message>,
    val count: Int
)
```

---

## Step 2: 添加消息去重逻辑 (10分钟)

### 文件: `apps/android/app/src/main/java/com/lanxin/im/ui/chat/ChatViewModel.kt`

### 操作: 修改onNewMessage方法

找到WebSocket监听器中的:
```kotlin
override fun onNewMessage(message: Message) {
    // 收到新消息，更新UI
    if (message.conversationId == conversationId) {
        val currentList = _messages.value.orEmpty().toMutableList()
        currentList.add(message)
        _messages.postValue(currentList)
        
        // 标记已读
        markAsRead()
    }
}
```

替换为:
```kotlin
override fun onNewMessage(message: Message) {
    // 收到新消息，更新UI
    if (message.conversationId == conversationId) {
        val currentList = _messages.value.orEmpty().toMutableList()
        
        // ✅ 新增: 检查消息是否已存在(去重)
        val exists = currentList.any { it.id == message.id }
        if (exists) {
            android.util.Log.d("ChatViewModel", "Message ${message.id} already exists, skipping")
            return
        }
        
        currentList.add(message)
        _messages.postValue(currentList)
        
        // 标记已读
        markAsRead()
    }
}
```

---

## Step 3: 添加上线拉取离线消息 (15分钟)

### 文件: `apps/android/app/src/main/java/com/lanxin/im/data/remote/WebSocketClient.kt`

### 操作: 修改onOpen方法

找到:
```kotlin
override fun onOpen(webSocket: WebSocket, response: Response) {
    Log.d(TAG, "WebSocket connected")
    isConnected = true
    listeners.forEach { it.onConnected() }
    startHeartbeat()
}
```

替换为:
```kotlin
override fun onOpen(webSocket: WebSocket, response: Response) {
    Log.d(TAG, "WebSocket connected")
    isConnected = true
    listeners.forEach { it.onConnected() }
    startHeartbeat()
    
    // ✅ 新增: 上线后立即拉取离线消息
    fetchOfflineMessages()
}
```

### 操作: 在类中添加fetchOfflineMessages方法

在WebSocketClient类中添加:
```kotlin
private fun fetchOfflineMessages() {
    // 使用协程拉取离线消息
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = apiService.getOfflineMessages()
            if (response.code == 0 && response.data != null) {
                val offlineMessages = response.data.messages
                Log.d(TAG, "Fetched ${offlineMessages.size} offline messages")
                
                // 保存到本地数据库
                offlineMessages.forEach { message ->
                    messageDao.insertMessage(message)
                }
                
                // 通知UI更新
                offlineMessages.forEach { message ->
                    listeners.forEach { listener ->
                        listener.onNewMessage(message)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch offline messages", e)
        }
    }
}
```

### 操作: 添加必要的import

确保文件顶部有:
```kotlin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
```

---

## Step 4: 验证Android代码 (10分钟)

### 检查依赖注入

确保WebSocketClient能访问:
- `apiService`
- `messageDao`

如果没有,需要通过构造函数注入:
```kotlin
class WebSocketClient(
    private val apiService: ApiService,
    private val messageDao: MessageDao,
    token: String
) {
    // ...
}
```

---

## Step 5: 端到端测试 (30分钟)

### 测试环境

1. 后端服务器运行
2. Android APP安装到设备/模拟器

### 测试用例1: 离线消息接收

```
步骤1: 关闭Android APP (用户2离线)
步骤2: 网页/Postman发送消息给用户2
步骤3: 打开Android APP (用户2上线)

✅ 期望:
- APP启动后自动连接WebSocket
- WebSocket连接成功后自动拉取离线消息
- 聊天界面显示离线消息
- 会话列表显示未读数
```

### 测试用例2: 群组创建(APP)

```
步骤1: 在APP中点击"创建群组"
步骤2: 选择成员并输入群名
步骤3: 点击"创建"

✅ 期望:
- 创建成功
- 群组列表显示新群组
- 所有成员收到通知
```

### 测试用例3: 发送群消息(APP)

```
步骤1: 打开群聊界面
步骤2: 输入消息并发送

✅ 期望:
- 消息发送成功
- 群内所有在线成员实时收到
- 群内离线成员上线后能看到
```

---

## Step 6: 提交代码 (5分钟)

```bash
git status

# 期望看到:
# modified:   apps/android/app/src/main/java/com/lanxin/im/data/remote/ApiService.kt
# modified:   apps/android/app/src/main/java/com/lanxin/im/data/remote/WebSocketClient.kt
# modified:   apps/android/app/src/main/java/com/lanxin/im/ui/chat/ChatViewModel.kt

git add apps/android/app/src/main/java/com/lanxin/im/data/remote/ApiService.kt
git add apps/android/app/src/main/java/com/lanxin/im/data/remote/WebSocketClient.kt
git add apps/android/app/src/main/java/com/lanxin/im/ui/chat/ChatViewModel.kt

git commit -m "feat: Android集成群聊和离线消息功能

新增功能:
- 群组API定义(8个接口)
- 离线消息API定义
- WebSocket上线自动拉取离线消息
- 消息去重逻辑

API列表:
- 创建群组/获取群信息/群成员管理
- 发送群消息/更新群信息/解散群组
- 拉取离线消息

优化:
- 消息去重,防止重复显示
- 上线自动拉取,用户体验好

测试通过:
- 群组API调用正常
- 离线消息能正确拉取
- 消息不重复显示"
```

---

# ✅ 阶段5验收

## 必须全部通过 (10/10)

```
[ ] 1. Android代码修改完成
[ ] 2. 群组API定义添加
[ ] 3. 离线消息API添加
[ ] 4. WebSocket拉取逻辑添加
[ ] 5. 消息去重逻辑添加
[ ] 6. Android APP能编译(如果JDK 17已安装)
[ ] 7. APP能创建群组
[ ] 8. APP能发送群消息
[ ] 9. APP上线能拉取离线消息
[ ] 10. Git提交完成
```

---

# 📊 阶段5完成标志

```
✅ Android完全对接后端
✅ 群聊功能完整可用
✅ 离线消息自动拉取
✅ 整个IM系统完全可用
```

**本阶段完成后,所有5个阶段修复完成!**

---

**文档版本**: 1.0  
**创建时间**: 2025-10-18  
**预计完成时间**: 1小时  
**实际完成时间**: ________  
**验收结果**: ⬜ 通过 / ⬜ 失败

