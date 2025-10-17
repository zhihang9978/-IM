package com.lanxin.im.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

/**
 * 埋点统计工具类
 * 用于统计用户行为和活跃时段
 */
object AnalyticsHelper {
    
    private const val PREFS_NAME = "analytics_prefs"
    private const val KEY_LAST_ACTIVE_TIME = "last_active_time"
    private const val KEY_TOTAL_SESSIONS = "total_sessions"
    private const val KEY_FEATURE_USAGE = "feature_usage_"
    
    private lateinit var prefs: SharedPreferences
    
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
     * 打印统计报告（仅开发环境）
     */
    fun printAnalyticsReport() {
        if (!::prefs.isInitialized) {
            Log.d("Analytics", "Not initialized")
            return
        }
        
        Log.d("Analytics", "=== Analytics Report ===")
        Log.d("Analytics", "Total Sessions: ${getTotalSessions()}")
        
        val lastActive = getLastActiveTime()
        if (lastActive > 0) {
            val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            Log.d("Analytics", "Last Active: ${timeFormat.format(Date(lastActive))}")
        }
        
        getAllAnalytics().forEach { (key, value) ->
            if (key.startsWith("feature_")) {
                Log.d("Analytics", "$key: $value")
            } else if (key.startsWith("active_time_slot_")) {
                Log.d("Analytics", "$key: $value")
            }
        }
        
        Log.d("Analytics", "=====================")
    }
}

