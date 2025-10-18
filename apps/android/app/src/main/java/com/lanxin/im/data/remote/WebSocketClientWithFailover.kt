package com.lanxin.im.data.remote

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.lanxin.im.data.model.Message
import com.lanxin.im.data.local.MessageDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit

/**
 * 支持故障转移的WebSocket客户端
 * 当主服务器连接失败时自动切换到副服务器
 */
class WebSocketClientWithFailover(
    private val context: Context,
    private val token: String,
    private val apiService: ApiService? = null,
    private val messageDao: MessageDao? = null
) {
    
    companion object {
        private const val TAG = "WSFailover"
        private const val HEARTBEAT_INTERVAL = 30000L // 30秒心跳
        private const val RECONNECT_INTERVAL = 5000L // 5秒重连间隔
        private const val MAX_RECONNECT_ATTEMPTS = 3 // 最大重连次数
    }
    
    private var webSocket: WebSocket? = null
    private val gson = Gson()
    private val listeners = mutableListOf<WebSocketListener>()
    private var isConnected = false
    private val handler = Handler(Looper.getMainLooper())
    private var reconnectAttempts = 0
    private var isReconnecting = false
    
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(30, TimeUnit.SECONDS) // OkHttp自动ping/pong
        .build()
    
    private val wsListener = object : okhttp3.WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket connected to ${ServerConfig.getCurrentServer().name}")
            isConnected = true
            reconnectAttempts = 0
            isReconnecting = false
            listeners.forEach { it.onConnected() }
            startHeartbeat()
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
            attemptReconnect()
        }
        
        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket error: ${t.message}", t)
            isConnected = false
            listeners.forEach { it.onError(t.message ?: "Unknown error") }
            attemptReconnect()
        }
    }
    
    /**
     * 连接WebSocket
     */
    fun connect() {
        if (isConnected || isReconnecting) {
            Log.w(TAG, "Already connected or reconnecting, skipping")
            return
        }
        
        val url = "${ServerConfig.getWsBaseUrl()}?token=$token"
        Log.d(TAG, "Connecting to WebSocket: $url")
        
        val request = Request.Builder()
            .url(url)
            .build()
        
        webSocket = client.newWebSocket(request, wsListener)
    }
    
    /**
     * 断开WebSocket连接
     */
    fun disconnect() {
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        isConnected = false
        reconnectAttempts = 0
        isReconnecting = false
        handler.removeCallbacksAndMessages(null)
    }
    
    /**
     * 尝试重连
     */
    private fun attemptReconnect() {
        if (isReconnecting) {
            return
        }
        
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.w(TAG, "Max reconnect attempts reached on ${ServerConfig.getCurrentServer().name}")
            
            // 切换到下一个服务器
            if (ServerConfig.switchToNextServer()) {
                Log.i(TAG, "Switched to ${ServerConfig.getCurrentServer().name}, resetting reconnect attempts")
                reconnectAttempts = 0
                
                // 使用新服务器重连
                handler.postDelayed({
                    connect()
                }, RECONNECT_INTERVAL)
            } else {
                Log.e(TAG, "Failed to switch server, giving up")
                listeners.forEach { it.onError("All servers unreachable") }
            }
            return
        }
        
        isReconnecting = true
        reconnectAttempts++
        
        Log.i(TAG, "Reconnecting to ${ServerConfig.getCurrentServer().name} (attempt $reconnectAttempts/$MAX_RECONNECT_ATTEMPTS)")
        
        handler.postDelayed({
            isReconnecting = false
            connect()
        }, RECONNECT_INTERVAL)
    }
    
    /**
     * 手动重连到主服务器
     */
    fun reconnectToPrimary() {
        Log.i(TAG, "Manually reconnecting to primary server")
        disconnect()
        ServerConfig.switchToPrimary()
        reconnectAttempts = 0
        connect()
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
                    Log.d(TAG, "Heartbeat pong received")
                }
                "new_message", "message" -> {
                    val data = gson.fromJson(gson.toJson(wsMessage.data), WSNewMessage::class.java)
                    handleNewMessage(data.message)
                    listeners.forEach { it.onNewMessage(data.message) }
                }
                "message_read" -> {
                    val data = gson.fromJson(gson.toJson(wsMessage.data), WSMessageRead::class.java)
                    handleMessageRead(data.messageId, data.conversationId)
                }
                "message_recalled" -> {
                    val data = gson.fromJson(gson.toJson(wsMessage.data), WSMessageRecalled::class.java)
                    handleMessageRecalled(data.messageId, data.conversationId)
                }
                "user_online", "user_offline" -> {
                    val data = gson.fromJson(gson.toJson(wsMessage.data), WSUserStatus::class.java)
                    handleUserStatus(data.userId, data.status)
                }
                "read_receipt" -> {
                    val data = gson.fromJson(gson.toJson(wsMessage.data), WSReadReceipt::class.java)
                    handleReadReceipt(data.conversationId, data.readerId, data.readAt)
                    listeners.forEach { it.onReadReceipt(ReadReceipt(data.conversationId, data.readerId, data.readAt)) }
                }
                "call_invite" -> {
                    val callInvite = gson.fromJson(gson.toJson(wsMessage.data), CallInvite::class.java)
                    listeners.forEach { it.onCallInvite(callInvite) }
                }
                "message_status" -> {
                    val statusUpdate = gson.fromJson(gson.toJson(wsMessage.data), MessageStatusUpdate::class.java)
                    listeners.forEach { it.onMessageStatusUpdate(statusUpdate) }
                }
                "server_switch" -> {
                    // 服务器切换通知
                    Log.i(TAG, "Received server switch notification")
                    handleServerSwitch()
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
     * 处理服务器切换通知
     */
    private fun handleServerSwitch() {
        Log.i(TAG, "Server is switching, will reconnect")
        disconnect()
        handler.postDelayed({
            connect()
        }, 2000) // 等待2秒再重连
    }
    
    private fun handleNewMessage(message: Message) {
        val intent = Intent("com.lanxin.im.NEW_MESSAGE")
        intent.putExtra("message", message)
        intent.putExtra("conversation_id", message.conversationId)
        context.sendBroadcast(intent)
    }
    
    private fun handleMessageRead(messageId: Long, conversationId: Long) {
        val intent = Intent("com.lanxin.im.MESSAGE_READ")
        intent.putExtra("message_id", messageId)
        intent.putExtra("conversation_id", conversationId)
        context.sendBroadcast(intent)
    }
    
    private fun handleMessageRecalled(messageId: Long, conversationId: Long) {
        val intent = Intent("com.lanxin.im.MESSAGE_RECALLED")
        intent.putExtra("message_id", messageId)
        intent.putExtra("conversation_id", conversationId)
        context.sendBroadcast(intent)
    }
    
    private fun handleUserStatus(userId: Long, status: String) {
        val intent = Intent("com.lanxin.im.USER_STATUS")
        intent.putExtra("user_id", userId)
        intent.putExtra("status", status)
        context.sendBroadcast(intent)
    }
    
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
    
    private fun fetchOfflineMessages() {
        if (apiService == null || messageDao == null) {
            Log.w(TAG, "ApiService or MessageDao not injected, skipping offline messages fetch")
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.getOfflineMessages()
                if (response.code == 0 && response.data != null) {
                    val offlineMessages = response.data.messages
                    Log.d(TAG, "Fetched ${offlineMessages.size} offline messages")
                    
                    offlineMessages.forEach { message ->
                        messageDao.insertMessage(message)
                    }
                    
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
    
    /**
     * 获取连接状态
     */
    fun isConnected(): Boolean = isConnected
    
    /**
     * 获取当前服务器信息
     */
    fun getCurrentServerName(): String = ServerConfig.getCurrentServer().name
    
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
