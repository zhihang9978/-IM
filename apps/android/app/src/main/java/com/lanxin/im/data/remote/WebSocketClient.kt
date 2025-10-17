package com.lanxin.im.data.remote

import android.util.Log
import com.google.gson.Gson
import com.lanxin.im.data.model.Message
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit

/**
 * WebSocket客户端
 * 用于实时消息推送、通话邀请等
 */
class WebSocketClient(private val token: String) {
    
    companion object {
        private const val TAG = "WebSocketClient"
        private const val WS_URL = "wss://api.lanxin168.com/ws"
        private const val HEARTBEAT_INTERVAL = 30000L // 30秒心跳
    }
    
    private var webSocket: WebSocket? = null
    private val gson = Gson()
    private val listeners = mutableListOf<WebSocketListener>()
    private var isConnected = false
    
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // 长连接
        .build()
    
    private val wsListener = object : okhttp3.WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket connected")
            isConnected = true
            listeners.forEach { it.onConnected() }
            startHeartbeat()
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
                "message" -> {
                    // 新消息
                    val message = gson.fromJson(
                        gson.toJson(wsMessage.data),
                        Message::class.java
                    )
                    listeners.forEach { it.onNewMessage(message) }
                }
                "message_status" -> {
                    // 消息状态更新
                    val statusUpdate = gson.fromJson(
                        gson.toJson(wsMessage.data),
                        MessageStatusUpdate::class.java
                    )
                    listeners.forEach { it.onMessageStatusUpdate(statusUpdate) }
                }
                "call_invite" -> {
                    // 通话邀请
                    val callInvite = gson.fromJson(
                        gson.toJson(wsMessage.data),
                        CallInvite::class.java
                    )
                    listeners.forEach { it.onCallInvite(callInvite) }
                }
                "read_receipt" -> {
                    // 已读回执
                    val readReceipt = gson.fromJson(
                        gson.toJson(wsMessage.data),
                        ReadReceipt::class.java
                    )
                    listeners.forEach { it.onReadReceipt(readReceipt) }
                }
                else -> {
                    Log.w(TAG, "Unknown message type: ${wsMessage.type}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message", e)
        }
    }
    
    private fun startHeartbeat() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (isConnected) {
                    send("{\"type\":\"ping\"}")
                    handler.postDelayed(this, 30000)
                }
            }
        }, 30000)
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

// ==================== WebSocket数据类 ====================

data class WebSocketMessage(
    val type: String,
    val data: Any?
)

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

