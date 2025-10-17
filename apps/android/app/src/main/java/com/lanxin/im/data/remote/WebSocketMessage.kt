package com.lanxin.im.data.remote

import com.google.gson.annotations.SerializedName
import com.lanxin.im.data.model.Message

/**
 * WebSocket消息类型定义
 * 参考：WildFireChat WebSocket协议 (Apache 2.0)
 * 适配：蓝信IM
 */
data class WebSocketMessage(
    val type: String,  // new_message, message_read, message_recalled, user_online, user_offline, read_receipt
    val data: Any?
)

/**
 * 新消息通知
 */
data class WSNewMessage(
    val message: Message
)

/**
 * 消息已读通知
 */
data class WSMessageRead(
    @SerializedName("message_id")
    val messageId: Long,
    @SerializedName("conversation_id")
    val conversationId: Long
)

/**
 * 消息撤回通知
 */
data class WSMessageRecalled(
    @SerializedName("message_id")
    val messageId: Long,
    @SerializedName("conversation_id")
    val conversationId: Long
)

/**
 * 用户在线状态通知
 */
data class WSUserStatus(
    @SerializedName("user_id")
    val userId: Long,
    val status: String  // online, offline
)

/**
 * 已读回执通知
 */
data class WSReadReceipt(
    @SerializedName("conversation_id")
    val conversationId: Long,
    @SerializedName("reader_id")
    val readerId: Long,
    @SerializedName("read_at")
    val readAt: String
)

