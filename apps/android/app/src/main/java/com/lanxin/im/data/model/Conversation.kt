package com.lanxin.im.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey
    val id: Long,
    val type: String, // single, group
    val user1Id: Long?,
    val user2Id: Long?,
    val groupId: Long?,
    val lastMessageId: Long?,
    val lastMessageAt: Long?,
    val unreadCount: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
    // UI展示字段
    val avatar: String? = null,
    val name: String? = null,
    val lastMessage: String? = null,
    val lastMessageTime: Long = updatedAt,
    val lastMessageType: String = "text",
    val lastMessageContent: String = lastMessage ?: "",
    val isMuted: Boolean = false,
    val isTop: Boolean = false,
    val draft: String? = null
)

object ConversationType {
    const val SINGLE = "single"
    const val GROUP = "group"
}

