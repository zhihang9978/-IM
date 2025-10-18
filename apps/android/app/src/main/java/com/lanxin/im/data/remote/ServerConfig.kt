package com.lanxin.im.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * 服务器配置管理
 * 支持主从双服务器架构，自动故障转移
 */
object ServerConfig {
    private const val TAG = "ServerConfig"
    private const val PREFS_NAME = "server_config"
    private const val KEY_CURRENT_SERVER_INDEX = "current_server_index"
    private const val KEY_LAST_SWITCH_TIME = "last_switch_time"
    
    /**
     * 服务器列表
     * servers[0]: 主服务器 (154.40.45.121)
     * servers[1]: 副服务器 (154.40.45.98)
     */
    private val servers = listOf(
        ServerInfo(
            name = "主服务器",
            apiBaseUrl = "http://154.40.45.121:8080/api/v1/",
            wsBaseUrl = "ws://154.40.45.121:8080/ws",
            isPrimary = true
        ),
        ServerInfo(
            name = "副服务器",
            apiBaseUrl = "http://154.40.45.98:8080/api/v1/",
            wsBaseUrl = "ws://154.40.45.98:8080/ws",
            isPrimary = false
        )
    )
    
    private var currentServerIndex = 0
    private var lastSwitchTime = 0L
    private val minSwitchInterval = 10000L // 10秒内不重复切换
    
    private lateinit var prefs: SharedPreferences
    
    /**
     * 初始化配置
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        currentServerIndex = prefs.getInt(KEY_CURRENT_SERVER_INDEX, 0)
        lastSwitchTime = prefs.getLong(KEY_LAST_SWITCH_TIME, 0L)
        Log.d(TAG, "Initialized with server: ${getCurrentServer().name}")
    }
    
    /**
     * 获取当前服务器
     */
    fun getCurrentServer(): ServerInfo {
        return servers[currentServerIndex]
    }
    
    /**
     * 获取当前API基础URL
     */
    fun getApiBaseUrl(): String {
        return getCurrentServer().apiBaseUrl
    }
    
    /**
     * 获取当前WebSocket URL
     */
    fun getWsBaseUrl(): String {
        return getCurrentServer().wsBaseUrl
    }
    
    /**
     * 切换到下一个服务器
     * @return 是否成功切换
     */
    fun switchToNextServer(): Boolean {
        val now = System.currentTimeMillis()
        
        // 防止频繁切换
        if (now - lastSwitchTime < minSwitchInterval) {
            Log.w(TAG, "Server switch too frequent, skipping")
            return false
        }
        
        val oldServer = getCurrentServer()
        currentServerIndex = (currentServerIndex + 1) % servers.size
        lastSwitchTime = now
        
        // 保存到SharedPreferences
        prefs.edit().apply {
            putInt(KEY_CURRENT_SERVER_INDEX, currentServerIndex)
            putLong(KEY_LAST_SWITCH_TIME, lastSwitchTime)
            apply()
        }
        
        val newServer = getCurrentServer()
        Log.i(TAG, "Switched from ${oldServer.name} to ${newServer.name}")
        
        return true
    }
    
    /**
     * 切换到主服务器
     */
    fun switchToPrimary() {
        val primaryIndex = servers.indexOfFirst { it.isPrimary }
        if (primaryIndex >= 0 && primaryIndex != currentServerIndex) {
            currentServerIndex = primaryIndex
            lastSwitchTime = System.currentTimeMillis()
            prefs.edit().apply {
                putInt(KEY_CURRENT_SERVER_INDEX, currentServerIndex)
                putLong(KEY_LAST_SWITCH_TIME, lastSwitchTime)
                apply()
            }
            Log.i(TAG, "Switched to primary server")
        }
    }
    
    /**
     * 获取所有服务器列表
     */
    fun getAllServers(): List<ServerInfo> {
        return servers
    }
    
    /**
     * 重置为主服务器
     */
    fun reset() {
        currentServerIndex = 0
        lastSwitchTime = 0L
        prefs.edit().apply {
            putInt(KEY_CURRENT_SERVER_INDEX, 0)
            putLong(KEY_LAST_SWITCH_TIME, 0L)
            apply()
        }
        Log.i(TAG, "Reset to primary server")
    }
}

/**
 * 服务器信息
 */
data class ServerInfo(
    val name: String,
    val apiBaseUrl: String,
    val wsBaseUrl: String,
    val isPrimary: Boolean
)
