package com.lanxin.im.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey
    val id: Long,
    val conversationId: Long,
    val senderId: Long,
    val receiverId: Long,
    val content: String,
    val type: String, // text, image, voice, video, file
    val fileUrl: String?,
    val fileSize: Long?,
    val duration: Int?, // 语音/视频时长（秒）
    val status: String, // sent, delivered, read, recalled
    val createdAt: Long,
    val updatedAt: Long
)

// 消息类型常量
object MessageType {
    const val TEXT = "text"
    const val IMAGE = "image"
    const val VOICE = "voice"
    const val VIDEO = "video"
    const val FILE = "file"
}

// 消息状态常量
object MessageStatus {
    const val SENT = "sent"
    const val DELIVERED = "delivered"
    const val READ = "read"
    const val RECALLED = "recalled"
}

