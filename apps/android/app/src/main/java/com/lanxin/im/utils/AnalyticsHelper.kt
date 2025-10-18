package com.lanxin.im.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

/**
 * 埋点统计工具类
 * 运营级别特性：
 * - 用户行为追踪
 * - 性能监控
 * - 错误统计
 * - 活跃时段分析
 * - API调用监控
 */
object AnalyticsHelper {
    
    private const val PREFS_NAME = "analytics_prefs"
    private const val KEY_LAST_ACTIVE_TIME = "last_active_time"
    private const val KEY_TOTAL_SESSIONS = "total_sessions"
    private const val KEY_FEATURE_USAGE = "feature_usage_"
    private const val KEY_ERROR_COUNT = "error_count_"
    private const val KEY_API_CALL_COUNT = "api_call_count_"
    private const val KEY_PERFORMANCE_METRIC = "performance_"
    
    private lateinit var prefs: SharedPreferences
    
    // 性能计时器
    private val timers = mutableMapOf<String, Long>()
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 记录用户活跃
     */
    fun trackUserActive(context: Context) {
        if (!::prefs.isInitialized) {
            init(context)
        }
        
        val currentTime = System.currentTimeMillis()
        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        
        prefs.edit()
            .putLong(KEY_LAST_ACTIVE_TIME, currentTime)
            .putInt(KEY_TOTAL_SESSIONS, getTotalSessions() + 1)
            .apply()
        
        Log.d("Analytics", "User active at: ${timeFormat.format(Date(currentTime))}")
        logActiveTimeSlot(currentTime)
    }
    
    /**
     * 记录功能使用
     */
    fun trackFeatureUsage(context: Context, featureName: String) {
        if (!::prefs.isInitialized) {
            init(context)
        }
        
        val key = KEY_FEATURE_USAGE + featureName
        val currentCount = prefs.getInt(key, 0)
        
        prefs.edit()
            .putInt(key, currentCount + 1)
            .apply()
        
        Log.d("Analytics", "Feature used: $featureName (${currentCount + 1} times)")
    }
    
    /**
     * 记录活跃时段
     */
    private fun logActiveTimeSlot(timestamp: Long) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        
        val timeSlot = when (hour) {
            in 0..5 -> "凌晨(00-06)"
            in 6..11 -> "上午(06-12)"
            in 12..17 -> "下午(12-18)"
            else -> "晚上(18-24)"
        }
        
        val key = "active_time_slot_$timeSlot"
        val count = prefs.getInt(key, 0)
        prefs.edit().putInt(key, count + 1).apply()
        
        Log.d("Analytics", "Active time slot: $timeSlot (${count + 1} times)")
    }
    
    /**
     * 获取总会话数
     */
    fun getTotalSessions(): Int {
        return if (::prefs.isInitialized) {
            prefs.getInt(KEY_TOTAL_SESSIONS, 0)
        } else {
            0
        }
    }
    
    /**
     * 获取功能使用次数
     */
    fun getFeatureUsageCount(featureName: String): Int {
        return if (::prefs.isInitialized) {
            prefs.getInt(KEY_FEATURE_USAGE + featureName, 0)
        } else {
            0
        }
    }
    
    /**
     * 获取最后活跃时间
     */
    fun getLastActiveTime(): Long {
        return if (::prefs.isInitialized) {
            prefs.getLong(KEY_LAST_ACTIVE_TIME, 0)
        } else {
            0
        }
    }
    
    /**
     * 获取所有统计数据（用于后台展示）
     */
    fun getAllAnalytics(): Map<String, Any> {
        if (!::prefs.isInitialized) {
            return emptyMap()
        }
        
        val analytics = mutableMapOf<String, Any>()
        
        analytics["total_sessions"] = getTotalSessions()
        analytics["last_active_time"] = getLastActiveTime()
        
        prefs.all.forEach { (key, value) ->
            if (key.startsWith(KEY_FEATURE_USAGE)) {
                val featureName = key.removePrefix(KEY_FEATURE_USAGE)
                analytics["feature_$featureName"] = value ?: 0
            } else if (key.startsWith("active_time_slot_")) {
                analytics[key] = value ?: 0
            }
        }
        
        return analytics
    }
    
    /**
     * 开始性能计时
     */
    fun startTimer(timerName: String) {
        timers[timerName] = System.currentTimeMillis()
    }
    
    /**
     * 结束性能计时并记录
     */
    fun endTimer(context: Context, timerName: String) {
        if (!::prefs.isInitialized) {
            init(context)
        }
        
        val startTime = timers[timerName] ?: return
        val duration = System.currentTimeMillis() - startTime
        timers.remove(timerName)
        
        val key = KEY_PERFORMANCE_METRIC + timerName
        val currentAvg = prefs.getLong(key, 0L)
        val count = prefs.getInt("${key}_count", 0)
        
        val newAvg = if (count == 0) {
            duration
        } else {
            (currentAvg * count + duration) / (count + 1)
        }
        
        prefs.edit()
            .putLong(key, newAvg)
            .putInt("${key}_count", count + 1)
            .apply()
        
        Log.d("Analytics", "Performance [$timerName]: ${duration}ms (avg: ${newAvg}ms)")
    }
    
    /**
     * 记录错误
     */
    fun trackError(context: Context, errorType: String, errorMessage: String? = null) {
        if (!::prefs.isInitialized) {
            init(context)
        }
        
        val key = KEY_ERROR_COUNT + errorType
        val count = prefs.getInt(key, 0)
        
        prefs.edit()
            .putInt(key, count + 1)
            .putLong("${key}_last_time", System.currentTimeMillis())
            .apply()
        
        Log.e("Analytics", "Error [$errorType]: ${errorMessage ?: "Unknown"} (count: ${count + 1})")
    }
    
    /**
     * 记录API调用
     */
    fun trackApiCall(context: Context, apiName: String, success: Boolean, duration: Long) {
        if (!::prefs.isInitialized) {
            init(context)
        }
        
        val key = KEY_API_CALL_COUNT + apiName
        val successKey = "${key}_success"
        val failKey = "${key}_fail"
        val durationKey = "${key}_duration"
        
        val totalCalls = prefs.getInt(key, 0)
        val successCalls = prefs.getInt(successKey, 0)
        val failCalls = prefs.getInt(failKey, 0)
        val avgDuration = prefs.getLong(durationKey, 0L)
        
        val newAvgDuration = if (totalCalls == 0) {
            duration
        } else {
            (avgDuration * totalCalls + duration) / (totalCalls + 1)
        }
        
        prefs.edit()
            .putInt(key, totalCalls + 1)
            .putInt(if (success) successKey else failKey, if (success) successCalls + 1 else failCalls + 1)
            .putLong(durationKey, newAvgDuration)
            .apply()
        
        val status = if (success) "SUCCESS" else "FAILED"
        Log.d("Analytics", "API [$apiName] $status: ${duration}ms (avg: ${newAvgDuration}ms)")
    }
    
    /**
     * 记录消息发送
     */
    fun trackMessageSent(context: Context, messageType: String) {
        trackFeatureUsage(context, "message_sent_$messageType")
    }
    
    /**
     * 记录消息接收
     */
    fun trackMessageReceived(context: Context, messageType: String) {
        trackFeatureUsage(context, "message_received_$messageType")
    }
    
    /**
     * 记录会话打开
     */
    fun trackConversationOpen(context: Context, conversationType: String) {
        trackFeatureUsage(context, "conversation_open_$conversationType")
    }
    
    /**
     * 记录语音/视频通话
     */
    fun trackCall(context: Context, callType: String, duration: Long) {
        trackFeatureUsage(context, "call_$callType")
        
        if (!::prefs.isInitialized) {
            init(context)
        }
        
        val key = "call_${callType}_total_duration"
        val totalDuration = prefs.getLong(key, 0L)
        
        prefs.edit()
            .putLong(key, totalDuration + duration)
            .apply()
        
        Log.d("Analytics", "Call [$callType]: ${duration}s (total: ${totalDuration + duration}s)")
    }
    
    /**
     * 打印统计报告（仅开发环境）
     */
    fun printAnalyticsReport() {
        if (!::prefs.isInitialized) {
            Log.d("Analytics", "Not initialized")
            return
        }
        
        Log.d("Analytics", "========== Analytics Report ==========")
        Log.d("Analytics", "Total Sessions: ${getTotalSessions()}")
        
        val lastActive = getLastActiveTime()
        if (lastActive > 0) {
            val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            Log.d("Analytics", "Last Active: ${timeFormat.format(Date(lastActive))}")
        }
        
        Log.d("Analytics", "\n--- Feature Usage ---")
        getAllAnalytics().forEach { (key, value) ->
            if (key.startsWith("feature_")) {
                Log.d("Analytics", "$key: $value")
            }
        }
        
        Log.d("Analytics", "\n--- Performance Metrics ---")
        getAllAnalytics().forEach { (key, value) ->
            if (key.startsWith(KEY_PERFORMANCE_METRIC) && !key.endsWith("_count")) {
                val count = prefs.getInt("${key}_count", 0)
                Log.d("Analytics", "$key: ${value}ms (count: $count)")
            }
        }
        
        Log.d("Analytics", "\n--- Error Statistics ---")
        getAllAnalytics().forEach { (key, value) ->
            if (key.startsWith(KEY_ERROR_COUNT) && !key.endsWith("_last_time")) {
                val lastTime = prefs.getLong("${key}_last_time", 0)
                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                Log.d("Analytics", "$key: $value (last: ${timeFormat.format(Date(lastTime))})")
            }
        }
        
        Log.d("Analytics", "\n--- API Calls ---")
        getAllAnalytics().forEach { (key, value) ->
            if (key.startsWith(KEY_API_CALL_COUNT) && 
                !key.endsWith("_success") && 
                !key.endsWith("_fail") && 
                !key.endsWith("_duration")) {
                val success = prefs.getInt("${key}_success", 0)
                val fail = prefs.getInt("${key}_fail", 0)
                val duration = prefs.getLong("${key}_duration", 0L)
                val successRate = if (value is Int && value > 0) {
                    (success.toFloat() / value * 100).toInt()
                } else 0
                Log.d("Analytics", "$key: total=$value, success=$success, fail=$fail, avg=${duration}ms, rate=${successRate}%")
            }
        }
        
        Log.d("Analytics", "\n--- Active Time Slots ---")
        getAllAnalytics().forEach { (key, value) ->
            if (key.startsWith("active_time_slot_")) {
                Log.d("Analytics", "$key: $value")
            }
        }
        
        Log.d("Analytics", "======================================")
    }
}

