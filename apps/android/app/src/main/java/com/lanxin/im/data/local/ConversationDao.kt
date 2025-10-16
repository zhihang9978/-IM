package com.lanxin.im.data.local

import androidx.room.*
import com.lanxin.im.data.model.Conversation
import kotlinx.coroutines.flow.Flow

/**
 * 会话数据访问对象
 */
@Dao
interface ConversationDao {
    
    @Query("SELECT * FROM conversations ORDER BY lastMessageAt DESC")
    fun observeAllConversations(): Flow<List<Conversation>>
    
    @Query("SELECT * FROM conversations ORDER BY lastMessageAt DESC")
    suspend fun getAllConversations(): List<Conversation>
    
    @Query("SELECT * FROM conversations WHERE id = :conversationId")
    suspend fun getConversationById(conversationId: Long): Conversation?
    
    @Query("SELECT * FROM conversations WHERE type = 'single' AND (user1Id = :userId OR user2Id = :userId)")
    suspend fun getSingleConversationWithUser(userId: Long): Conversation?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: Conversation)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(conversations: List<Conversation>)
    
    @Update
    suspend fun updateConversation(conversation: Conversation)
    
    @Query("UPDATE conversations SET unreadCount = :count WHERE id = :conversationId")
    suspend fun updateUnreadCount(conversationId: Long, count: Int)
    
    @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :conversationId")
    suspend fun clearUnreadCount(conversationId: Long)
    
    @Delete
    suspend fun deleteConversation(conversation: Conversation)
    
    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()
}

