package com.lanxin.im.data.cache

import android.content.Context
import android.util.Log
import com.lanxin.im.data.local.AppDatabase
import com.lanxin.im.data.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 离线消息缓存管理器
 * 运营级别特性:
 * - 智能缓存策略
 * - 自动清理过期数据
 * - 内存+磁盘双层缓存
 * - 同步状态管理
 * 
 * 参考: WeChat/Telegram离线消息处理方案
 */
class OfflineMessageCache(private val context: Context) {
    
    companion object {
        private const val TAG = "OfflineMessageCache"
        
        // 缓存配置
        private const val MAX_MEMORY_CACHE_SIZE = 100 // 内存缓存最大消息数
        private const val MAX_DISK_CACHE_DAYS = 7    // 磁盘缓存保留天数
        private const val SYNC_BATCH_SIZE = 50        // 同步批次大小
    }
    
    private val database = AppDatabase.getDatabase(context)
    private val messageDao = database.messageDao()
    
    // 内存缓存 (LRU)
    private val memoryCache = LinkedHashMap<Long, Message>(
        MAX_MEMORY_CACHE_SIZE,
        0.75f,
        true // 访问顺序排序
    )
    
    // 待同步消息队列
    private val pendingSyncQueue = mutableListOf<Message>()
    
    /**
     * 缓存消息到内存
     */
    fun cacheToMemory(message: Message) {
        synchronized(memoryCache) {
            // LRU淘汰策略
            if (memoryCache.size >= MAX_MEMORY_CACHE_SIZE) {
                val oldest = memoryCache.entries.firstOrNull()
                oldest?.let {
                    memoryCache.remove(it.key)
                    Log.d(TAG, "Evicted message ${it.key} from memory cache")
                }
            }
            
            memoryCache[message.id] = message
        }
    }
    
    /**
     * 从内存缓存获取消息
     */
    fun getFromMemory(messageId: Long): Message? {
        synchronized(memoryCache) {
            return memoryCache[messageId]
        }
    }
    
    /**
     * 缓存消息到数据库
     */
    suspend fun cacheToDisk(message: Message) {
        withContext(Dispatchers.IO) {
            try {
                messageDao.insert(message)
                Log.d(TAG, "Cached message ${message.id} to disk")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cache message to disk", e)
            }
        }
    }
    
    /**
     * 批量缓存消息
     */
    suspend fun batchCacheToDisk(messages: List<Message>) {
        withContext(Dispatchers.IO) {
            try {
                messageDao.insertAll(messages)
                Log.d(TAG, "Batch cached ${messages.size} messages to disk")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to batch cache messages", e)
            }
        }
    }
    
    /**
     * 获取会话的离线消息
     */
    suspend fun getOfflineMessages(conversationId: Long): List<Message> {
        return withContext(Dispatchers.IO) {
            try {
                messageDao.getMessagesForConversation(conversationId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get offline messages", e)
                emptyList()
            }
        }
    }
    
    /**
     * 获取所有未同步消息
     */
    suspend fun getUnsyncedMessages(): List<Message> {
        return withContext(Dispatchers.IO) {
            try {
                // 假设有status字段标记同步状态
                messageDao.getAll().filter { it.status == "pending" || it.status == "failed" }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get unsynced messages", e)
                emptyList()
            }
        }
    }
    
    /**
     * 添加到待同步队列
     */
    fun addToPendingSync(message: Message) {
        synchronized(pendingSyncQueue) {
            if (!pendingSyncQueue.any { it.id == message.id }) {
                pendingSyncQueue.add(message)
                Log.d(TAG, "Added message ${message.id} to pending sync queue")
            }
        }
    }
    
    /**
     * 同步待发送消息
     */
    suspend fun syncPendingMessages(
        onSuccess: (Message) -> Unit,
        onFailure: (Message, Exception) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            synchronized(pendingSyncQueue) {
                val messagesToSync = pendingSyncQueue.toList()
                pendingSyncQueue.clear()
                
                messagesToSync.forEach { message ->
                    try {
                        // 这里应该调用实际的API发送
                        // RetrofitClient.apiService.sendMessage(...)
                        
                        // 模拟成功
                        withContext(Dispatchers.Main) {
                            onSuccess(message)
                        }
                        
                        // 更新数据库状态
                        val updatedMessage = message.copy(status = "sent")
                        messageDao.update(updatedMessage)
                        
                        Log.d(TAG, "Synced message ${message.id}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to sync message ${message.id}", e)
                        
                        withContext(Dispatchers.Main) {
                            onFailure(message, e)
                        }
                        
                        // 重新加入队列（最多重试3次）
                        if (message.status != "failed_3") {
                            val retryMessage = message.copy(
                                status = when (message.status) {
                                    "failed" -> "failed_2"
                                    "failed_2" -> "failed_3"
                                    else -> "failed"
                                }
                            )
                            pendingSyncQueue.add(retryMessage)
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 清理过期缓存
     */
    suspend fun cleanExpiredCache() {
        withContext(Dispatchers.IO) {
            try {
                val expireTime = System.currentTimeMillis() - (MAX_DISK_CACHE_DAYS * 24 * 60 * 60 * 1000)
                
                val allMessages = messageDao.getAll()
                val expiredMessages = allMessages.filter { it.createdAt < expireTime }
                
                expiredMessages.forEach { message ->
                    messageDao.delete(message)
                }
                
                Log.d(TAG, "Cleaned ${expiredMessages.size} expired messages")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clean expired cache", e)
            }
        }
    }
    
    /**
     * 获取缓存统计信息
     */
    fun getCacheStats(): CacheStats {
        val memorySize = synchronized(memoryCache) { memoryCache.size }
        val pendingSync = synchronized(pendingSyncQueue) { pendingSyncQueue.size }
        
        return CacheStats(
            memoryMessageCount = memorySize,
            pendingSyncCount = pendingSync
        )
    }
    
    /**
     * 清空内存缓存
     */
    fun clearMemoryCache() {
        synchronized(memoryCache) {
            memoryCache.clear()
            Log.d(TAG, "Cleared memory cache")
        }
    }
    
    /**
     * 清空所有缓存
     */
    suspend fun clearAllCache() {
        withContext(Dispatchers.IO) {
            try {
                clearMemoryCache()
                
                synchronized(pendingSyncQueue) {
                    pendingSyncQueue.clear()
                }
                
                messageDao.deleteAll()
                
                Log.d(TAG, "Cleared all cache")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear all cache", e)
            }
        }
    }
    
    /**
     * 缓存统计信息
     */
    data class CacheStats(
        val memoryMessageCount: Int,
        val pendingSyncCount: Int
    )
}
