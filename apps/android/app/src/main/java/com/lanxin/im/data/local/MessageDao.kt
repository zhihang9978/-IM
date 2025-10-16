package com.lanxin.im.data.local

import androidx.room.*
import com.lanxin.im.data.model.Message
import kotlinx.coroutines.flow.Flow

/**
 * 消息数据访问对象
 */
@Dao
interface MessageDao {
    
    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: Long): Message?
    
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getMessagesByConversation(conversationId: Long, limit: Int, offset: Int): List<Message>
    
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt DESC")
    fun observeMessagesByConversation(conversationId: Long): Flow<List<Message>>
    
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId AND status != 'recalled' ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLastMessage(conversationId: Long): Message?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<Message>)
    
    @Update
    suspend fun updateMessage(message: Message)
    
    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: Long, status: String)
    
    @Delete
    suspend fun deleteMessage(message: Message)
    
    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesByConversation(conversationId: Long)
    
    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()
}

