package com.lanxin.im.data.remote

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.lanxin.im.BuildConfig
import com.lanxin.im.data.model.Message
import com.lanxin.im.data.local.MessageDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit

/**
 * WebSocket客户端
 * 用于实时消息推送、通话邀请等
 * 
 * 增强功能：
 * - 支持广播Intent通知UI更新
 * - 处理多种WebSocket消息类型
 * - 自动重连机制
 * - 使用BuildConfig进行多环境配置
 */
class WebSocketClient(
    private val context: Context,
    private val token: String,
    private val apiService: ApiService? = null,
    private val messageDao: MessageDao? = null
) {
    
    companion object {
        private const val TAG = "WebSocketClient"
        private val WS_URL = BuildConfig.WS_BASE_URL
        private const val HEARTBEAT_INTERVAL = 30000L // 30秒心跳
    }
    
    private var webSocket: WebSocket? = null
    private val gson = Gson()
    private val listeners = mutableListOf<WebSocketListener>()
    private var isConnected = false
    private val handler = Handler(Looper.getMainLooper())
    
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // 长连接
        .build()
    
    private val wsListener = object : okhttp3.WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket connected")
            isConnected = true
            listeners.forEach { it.onConnected() }
            startHeartbeat()
            
            // 上线后立即拉取离线消息
            fetchOfflineMessages()
        }
        
        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d(TAG, "Received message: $text")
            handleMessage(text)
        }
        
        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            Log.d(TAG, "Received bytes: ${bytes.hex()}")
        }
        
        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closing: $code / $reason")
            isConnected = false
        }
        
        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closed: $code / $reason")
            isConnected = false
            listeners.forEach { it.onDisconnected() }
        }
        
        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket error", t)
            isConnected = false
            listeners.forEach { it.onError(t.message ?: "Unknown error") }
        }
    }
    
    fun connect() {
        val url = "$WS_URL?token=$token"
        val request = Request.Builder()
            .url(url)
            .build()
        
        webSocket = client.newWebSocket(request, wsListener)
    }
    
    fun disconnect() {
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        isConnected = false
    }
    
    fun addListener(listener: WebSocketListener) {
        listeners.add(listener)
    }
    
    fun removeListener(listener: WebSocketListener) {
        listeners.remove(listener)
    }
    
    private fun handleMessage(text: String) {
        try {
            val wsMessage = gson.fromJson(text, WebSocketMessage::class.java)
            
            when (wsMessage.type) {
                "pong" -> {
                    // 心跳响应
                    Log.d(TAG, "Heartbeat pong received")
                }
                "new_message", "message" -> {
                    // 新消息通知
                    val data = gson.fromJson(gson.toJson(wsMessage.data), WSNewMessage::class.java)
                    handleNewMessage(data.message)
                    listeners.forEach { it.onNewMessage(data.message) }
                }
                "message_read" -> {
                    // 消息已读通知
                    val data = gson.fromJson(gson.toJson(wsMessage.data), WSMessageRead::class.java)
                    handleMessageRead(data.messageId, data.conversationId)
                }
                "message_recalled" -> {
                    // 消息撤回通知
                    val data = gson.fromJson(gson.toJson(wsMessage.data), WSMessageRecalled::class.java)
                    handleMessageRecalled(data.messageId, data.conversationId)
                }
                "user_online", "user_offline" -> {
                    // 用户在线状态变化
                    val data = gson.fromJson(gson.toJson(wsMessage.data), WSUserStatus::class.java)
                    handleUserStatus(data.userId, data.status)
                }
                "read_receipt" -> {
                    // 已读回执
                    val data = gson.fromJson(gson.toJson(wsMessage.data), WSReadReceipt::class.java)
                    handleReadReceipt(data.conversationId, data.readerId, data.readAt)
                    listeners.forEach { it.onReadReceipt(ReadReceipt(data.conversationId, data.readerId, data.readAt)) }
                }
                "call_invite" -> {
                    // 通话邀请
                    val callInvite = gson.fromJson(gson.toJson(wsMessage.data), CallInvite::class.java)
                    listeners.forEach { it.onCallInvite(callInvite) }
                }
                "message_status" -> {
                    // 消息状态更新
                    val statusUpdate = gson.fromJson(gson.toJson(wsMessage.data), MessageStatusUpdate::class.java)
                    listeners.forEach { it.onMessageStatusUpdate(statusUpdate) }
                }
                else -> {
                    Log.w(TAG, "Unknown message type: ${wsMessage.type}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message", e)
        }
    }
    
    /**
     * 处理新消息通知
     * 发送本地广播通知UI更新
     */
    private fun handleNewMessage(message: Message) {
        val intent = Intent("com.lanxin.im.NEW_MESSAGE")
        intent.putExtra("message", message)
        intent.putExtra("conversation_id", message.conversationId)
        context.sendBroadcast(intent)
    }
    
    /**
     * 处理消息已读通知
     */
    private fun handleMessageRead(messageId: Long, conversationId: Long) {
        val intent = Intent("com.lanxin.im.MESSAGE_READ")
        intent.putExtra("message_id", messageId)
        intent.putExtra("conversation_id", conversationId)
        context.sendBroadcast(intent)
    }
    
    /**
     * 处理消息撤回通知
     */
    private fun handleMessageRecalled(messageId: Long, conversationId: Long) {
        val intent = Intent("com.lanxin.im.MESSAGE_RECALLED")
        intent.putExtra("message_id", messageId)
        intent.putExtra("conversation_id", conversationId)
        context.sendBroadcast(intent)
    }
    
    /**
     * 处理用户在线状态变化
     */
    private fun handleUserStatus(userId: Long, status: String) {
        val intent = Intent("com.lanxin.im.USER_STATUS")
        intent.putExtra("user_id", userId)
        intent.putExtra("status", status)
        context.sendBroadcast(intent)
    }
    
    /**
     * 处理已读回执
     */
    private fun handleReadReceipt(conversationId: Long, readerId: Long, readAt: String) {
        val intent = Intent("com.lanxin.im.READ_RECEIPT")
        intent.putExtra("conversation_id", conversationId)
        intent.putExtra("reader_id", readerId)
        intent.putExtra("read_at", readAt)
        context.sendBroadcast(intent)
    }
    
    private fun startHeartbeat() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (isConnected) {
                    webSocket?.send("{\"type\":\"ping\"}")
                    handler.postDelayed(this, HEARTBEAT_INTERVAL)
                }
            }
        }, HEARTBEAT_INTERVAL)
    }
    
    fun sendHeartbeat() {
        val heartbeat = WebSocketMessage(
            type = "ping",
            data = mapOf("timestamp" to System.currentTimeMillis())
        )
        sendMessage(heartbeat)
    }
    
    private fun sendMessage(message: WebSocketMessage) {
        if (isConnected) {
            val json = gson.toJson(message)
            webSocket?.send(json)
        } else {
            Log.w(TAG, "WebSocket not connected, cannot send message")
        }
    }
    
    /**
     * 拉取离线消息
     * 在WebSocket连接成功后自动调用
     */
    private fun fetchOfflineMessages() {
        // 检查依赖是否存在
        if (apiService == null || messageDao == null) {
            Log.w(TAG, "ApiService or MessageDao not injected, skipping offline messages fetch")
            return
        }
        
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
    
    interface WebSocketListener {
        fun onConnected()
        fun onDisconnected()
        fun onError(error: String)
        fun onNewMessage(message: Message)
        fun onMessageStatusUpdate(update: MessageStatusUpdate)
        fun onCallInvite(invite: CallInvite)
        fun onReadReceipt(receipt: ReadReceipt)
    }
}

// ==================== WebSocket辅助数据类 ====================

data class MessageStatusUpdate(
    val message_id: Long,
    val status: String, // sent, delivered, read
    val timestamp: String
)

data class CallInvite(
    val caller_id: Long,
    val caller_username: String,
    val room_id: String,
    val call_type: String // audio, video
)

data class ReadReceipt(
    val conversation_id: Long,
    val reader_id: Long,
    val read_at: String
)

