package com.lanxin.im.utils

import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * 阅后即焚助手类
 * 处理阅后即焚消息的倒计时和自动删除
 */
object BurnAfterReadHelper {
    
    private const val TAG = "BurnAfterRead"
    private const val BURN_DELAY_MS = 5000L // 5秒后删除
    
    private val handler = Handler(Looper.getMainLooper())
    private val burnTimers = mutableMapOf<Long, Runnable>()
    
    /**
     * 开始倒计时删除
     * @param messageId 消息ID
     * @param onCountdown 倒计时回调 (剩余秒数)
     * @param onDelete 删除回调
     */
    fun startBurnCountdown(
        messageId: Long,
        onCountdown: ((Int) -> Unit)? = null,
        onDelete: () -> Unit
    ) {
        // 如果已经有倒计时，先取消
        cancelBurnCountdown(messageId)
        
        var remainingSeconds = 5
        
        val countdownRunnable = object : Runnable {
            override fun run() {
                if (remainingSeconds > 0) {
                    onCountdown?.invoke(remainingSeconds)
                    Log.d(TAG, "Message $messageId will burn in $remainingSeconds seconds")
                    remainingSeconds--
                    handler.postDelayed(this, 1000)
                } else {
                    Log.d(TAG, "Message $messageId burned!")
                    onDelete()
                    burnTimers.remove(messageId)
                }
            }
        }
        
        burnTimers[messageId] = countdownRunnable
        handler.post(countdownRunnable)
    }
    
    /**
     * 取消倒计时
     */
    fun cancelBurnCountdown(messageId: Long) {
        burnTimers[messageId]?.let { runnable ->
            handler.removeCallbacks(runnable)
            burnTimers.remove(messageId)
            Log.d(TAG, "Burn countdown cancelled for message $messageId")
        }
    }
    
    /**
     * 取消所有倒计时
     */
    fun cancelAllCountdowns() {
        burnTimers.forEach { (messageId, runnable) ->
            handler.removeCallbacks(runnable)
            Log.d(TAG, "Cancelled countdown for message $messageId")
        }
        burnTimers.clear()
    }
    
    /**
     * 检查消息是否正在倒计时
     */
    fun isCountingDown(messageId: Long): Boolean {
        return burnTimers.containsKey(messageId)
    }
    
    /**
     * 标记消息为已读（触发阅后即焚）
     */
    fun markAsRead(messageId: Long) {
        Log.d(TAG, "Message $messageId marked as read for burn after read")
        // 这里可以调用API标记消息已读
        // 实际的删除逻辑由startBurnCountdown处理
    }
}

