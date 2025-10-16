package com.lanxin.im.data.repository

import com.lanxin.im.data.local.MessageDao
import com.lanxin.im.data.local.ConversationDao
import com.lanxin.im.data.remote.ApiService
import com.lanxin.im.data.remote.SendMessageRequest
import com.lanxin.im.data.model.Message
import com.lanxin.im.data.model.Conversation
import kotlinx.coroutines.flow.Flow

/**
 * 聊天仓库
 * 统一管理本地和远程数据
 */
class ChatRepository(
    private val apiService: ApiService,
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao
) {
    
    // ==================== 会话相关 ====================
    
    fun observeConversations(): Flow<List<Conversation>> {
        return conversationDao.observeAllConversations()
    }
    
    suspend fun refreshConversations() {
        try {
            val response = apiService.getConversations()
            if (response.code == 0 && response.data != null) {
                // TODO: 转换并保存到本地数据库
            }
        } catch (e: Exception) {
            // 网络错误，使用本地缓存
            e.printStackTrace()
        }
    }
    
    suspend fun getConversation(conversationId: Long): Conversation? {
        return conversationDao.getConversationById(conversationId)
    }
    
    suspend fun clearUnreadCount(conversationId: Long) {
        conversationDao.clearUnreadCount(conversationId)
        // 同步到服务器
        try {
            apiService.markAsRead(conversationId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // ==================== 消息相关 ====================
    
    fun observeMessages(conversationId: Long): Flow<List<Message>> {
        return messageDao.observeMessagesByConversation(conversationId)
    }
    
    suspend fun getMessages(conversationId: Long, page: Int = 1): List<Message> {
        // 先从本地获取
        val localMessages = messageDao.getMessagesByConversation(
            conversationId,
            limit = 50,
            offset = (page - 1) * 50
        )
        
        // 再从服务器同步
        try {
            val response = apiService.getMessages(conversationId, page, 50)
            if (response.code == 0 && response.data != null) {
                val remoteMessages = response.data.messages
                messageDao.insertMessages(remoteMessages)
                return remoteMessages
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return localMessages
    }
    
    suspend fun sendMessage(
        receiverId: Long,
        content: String,
        type: String = "text",
        fileUrl: String? = null,
        fileSize: Long? = null,
        duration: Int? = null
    ): Result<Message> {
        return try {
            val request = SendMessageRequest(
                receiver_id = receiverId,
                content = content,
                type = type,
                file_url = fileUrl,
                file_size = fileSize,
                duration = duration
            )
            
            val response = apiService.sendMessage(request)
            if (response.code == 0 && response.data != null) {
                val message = response.data.message
                // 保存到本地
                messageDao.insertMessage(message)
                Result.success(message)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun recallMessage(messageId: Long): Result<Unit> {
        return try {
            val response = apiService.recallMessage(messageId)
            if (response.code == 0) {
                // 更新本地消息状态
                messageDao.updateMessageStatus(messageId, "recalled")
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun insertMessage(message: Message) {
        messageDao.insertMessage(message)
    }
    
    suspend fun updateMessageStatus(messageId: Long, status: String) {
        messageDao.updateMessageStatus(messageId, status)
    }
}

