package com.lanxin.im.data.remote

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.lanxin.im.data.model.Message
import kotlin.math.min
import kotlin.math.pow

/**
 * WebSocket连接管理器
 * 运营级别特性：
 * - 指数退避重连策略
 * - 心跳保活机制
 * - 网络状态监听
 * - 连接质量监控
 * 
 * 参考：OpenIM/野火IM WebSocket最佳实践
 */
class WebSocketManager(
    private val context: Context,
    private val token: String
) {
    
    companion object {
        private const val TAG = "WebSocketManager"
        
        // 心跳配置
        private const val HEARTBEAT_INTERVAL = 30000L // 30秒
        private const val HEARTBEAT_TIMEOUT = 5000L   // 5秒超时
        
        // 重连配置
        private const val INITIAL_RETRY_DELAY = 1000L      // 首次1秒
        private const val MAX_RETRY_DELAY = 60000L          // 最大60秒
        private const val MAX_RETRY_ATTEMPTS = Int.MAX_VALUE // 无限重试
        
        // 连接质量监控
        private const val POOR_CONNECTION_THRESHOLD = 3 // 3次心跳失败判定为弱网
    }
    
    private var webSocketClient: WebSocketClient? = null
    private val handler = Handler(Looper.getMainLooper())
    
    // 重连状态
    private var retryAttempt = 0
    private var isManuallyDisconnected = false
    
    // 心跳状态
    private var lastHeartbeatTime = 0L
    private var heartbeatFailureCount = 0
    private var isWaitingForPong = false
    
    // 监听器列表
    private val listeners = mutableListOf<ConnectionListener>()
    
    // 连接状态
    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        RECONNECTING,
        FAILED
    }
    
    private var currentState = ConnectionState.DISCONNECTED
    
    /**
     * 连接WebSocket
     */
    fun connect() {
        if (currentState == ConnectionState.CONNECTED || 
            currentState == ConnectionState.CONNECTING) {
            Log.w(TAG, "Already connected or connecting")
            return
        }
        
        isManuallyDisconnected = false
        retryAttempt = 0
        updateState(ConnectionState.CONNECTING)
        
        connectInternal()
    }
    
    /**
     * 断开连接
     */
    fun disconnect() {
        isManuallyDisconnected = true
        stopHeartbeat()
        cancelReconnect()
        
        webSocketClient?.disconnect()
        webSocketClient = null
        
        updateState(ConnectionState.DISCONNECTED)
    }
    
    /**
     * 内部连接逻辑
     */
    private fun connectInternal() {
        try {
            webSocketClient = WebSocketClient(context, token).apply {
                addListener(object : WebSocketClient.WebSocketListener {
                    override fun onConnected() {
                        handleConnected()
                    }
                    
                    override fun onDisconnected() {
                        handleDisconnected()
                    }
                    
                    override fun onError(error: String) {
                        handleError(error)
                    }
                    
                    override fun onNewMessage(message: Message) {
                        // 转发给外部监听器
                        listeners.forEach { it.onMessageReceived(message) }
                    }
                    
                    override fun onMessageStatusUpdate(update: MessageStatusUpdate) {
                        listeners.forEach { it.onMessageStatusUpdate(update) }
                    }
                    
                    override fun onCallInvite(invite: CallInvite) {
                        listeners.forEach { it.onCallInvite(invite) }
                    }
                    
                    override fun onReadReceipt(receipt: ReadReceipt) {
                        listeners.forEach { it.onReadReceipt(receipt) }
                    }
                })
                
                connect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create WebSocket client", e)
            scheduleReconnect()
        }
    }
    
    /**
     * 连接成功处理
     */
    private fun handleConnected() {
        Log.i(TAG, "WebSocket connected successfully")
        
        retryAttempt = 0
        heartbeatFailureCount = 0
        updateState(ConnectionState.CONNECTED)
        
        // 启动心跳
        startHeartbeat()
        
        // 通知监听器
        listeners.forEach { it.onConnected() }
    }
    
    /**
     * 断开连接处理
     */
    private fun handleDisconnected() {
        Log.w(TAG, "WebSocket disconnected")
        
        stopHeartbeat()
        
        if (!isManuallyDisconnected) {
            updateState(ConnectionState.RECONNECTING)
            scheduleReconnect()
        } else {
            updateState(ConnectionState.DISCONNECTED)
        }
        
        listeners.forEach { it.onDisconnected() }
    }
    
    /**
     * 错误处理
     */
    private fun handleError(error: String) {
        Log.e(TAG, "WebSocket error: $error")
        
        listeners.forEach { it.onError(error) }
        
        if (!isManuallyDisconnected) {
            scheduleReconnect()
        }
    }
    
    /**
     * 指数退避重连策略
     * 1s → 2s → 4s → 8s → 16s → 32s → 60s (最大)
     */
    private fun scheduleReconnect() {
        if (isManuallyDisconnected) {
            return
        }
        
        if (retryAttempt >= MAX_RETRY_ATTEMPTS) {
            updateState(ConnectionState.FAILED)
            Log.e(TAG, "Max retry attempts reached")
            return
        }
        
        // 指数退避计算
        val delay = min(
            INITIAL_RETRY_DELAY * (2.0.pow(retryAttempt.toDouble())).toLong(),
            MAX_RETRY_DELAY
        )
        
        retryAttempt++
        
        Log.i(TAG, "Scheduling reconnect attempt $retryAttempt in ${delay}ms")
        
        handler.postDelayed({
            if (!isManuallyDisconnected) {
                connectInternal()
            }
        }, delay)
    }
    
    /**
     * 取消重连
     */
    private fun cancelReconnect() {
        handler.removeCallbacksAndMessages(null)
    }
    
    /**
     * 启动心跳
     */
    private fun startHeartbeat() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                sendHeartbeat()
                handler.postDelayed(this, HEARTBEAT_INTERVAL)
            }
        }, HEARTBEAT_INTERVAL)
    }
    
    /**
     * 停止心跳
     */
    private fun stopHeartbeat() {
        handler.removeCallbacksAndMessages(null)
        isWaitingForPong = false
    }
    
    /**
     * 发送心跳
     */
    private fun sendHeartbeat() {
        if (currentState != ConnectionState.CONNECTED) {
            return
        }
        
        if (isWaitingForPong) {
            // 上一次心跳未收到响应
            heartbeatFailureCount++
            
            if (heartbeatFailureCount >= POOR_CONNECTION_THRESHOLD) {
                Log.w(TAG, "Poor connection detected (${heartbeatFailureCount} heartbeat failures)")
                listeners.forEach { it.onConnectionQualityChanged(ConnectionQuality.POOR) }
                
                // 主动重连
                webSocketClient?.disconnect()
                return
            }
        }
        
        try {
            webSocketClient?.sendHeartbeat()
            isWaitingForPong = true
            lastHeartbeatTime = System.currentTimeMillis()
            
            // 超时检测
            handler.postDelayed({
                if (isWaitingForPong) {
                    handleHeartbeatTimeout()
                }
            }, HEARTBEAT_TIMEOUT)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send heartbeat", e)
        }
    }
    
    /**
     * 心跳超时处理
     */
    private fun handleHeartbeatTimeout() {
        Log.w(TAG, "Heartbeat timeout")
        heartbeatFailureCount++
        
        if (heartbeatFailureCount >= POOR_CONNECTION_THRESHOLD) {
            // 连接质量差，主动重连
            webSocketClient?.disconnect()
        }
    }
    
    /**
     * 接收到pong响应
     */
    fun onPongReceived() {
        isWaitingForPong = false
        heartbeatFailureCount = 0
        
        val latency = System.currentTimeMillis() - lastHeartbeatTime
        
        // 评估连接质量
        val quality = when {
            latency < 100 -> ConnectionQuality.EXCELLENT
            latency < 300 -> ConnectionQuality.GOOD
            latency < 800 -> ConnectionQuality.FAIR
            else -> ConnectionQuality.POOR
        }
        
        listeners.forEach { it.onConnectionQualityChanged(quality) }
    }
    
    /**
     * 添加监听器
     */
    fun addListener(listener: ConnectionListener) {
        listeners.add(listener)
    }
    
    /**
     * 移除监听器
     */
    fun removeListener(listener: ConnectionListener) {
        listeners.remove(listener)
    }
    
    /**
     * 更新连接状态
     */
    private fun updateState(newState: ConnectionState) {
        if (currentState != newState) {
            currentState = newState
            listeners.forEach { it.onStateChanged(newState) }
        }
    }
    
    /**
     * 获取当前状态
     */
    fun getState(): ConnectionState = currentState
    
    /**
     * 连接质量枚举
     */
    enum class ConnectionQuality {
        EXCELLENT,  // <100ms
        GOOD,       // <300ms
        FAIR,       // <800ms
        POOR        // >=800ms or frequent failures
    }
    
    /**
     * 连接监听器接口
     */
    interface ConnectionListener {
        fun onConnected()
        fun onDisconnected()
        fun onError(error: String)
        fun onStateChanged(state: ConnectionState)
        fun onConnectionQualityChanged(quality: ConnectionQuality)
        fun onMessageReceived(message: Message)
        fun onMessageStatusUpdate(update: MessageStatusUpdate)
        fun onCallInvite(invite: CallInvite)
        fun onReadReceipt(receipt: ReadReceipt)
    }
}
