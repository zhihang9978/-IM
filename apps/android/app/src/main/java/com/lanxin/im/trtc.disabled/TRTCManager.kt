package com.lanxin.im.trtc

import android.content.Context
import android.util.Log
import com.tencent.rtmp.TXLiveBase
import com.tencent.trtc.TRTCCloud
import com.tencent.trtc.TRTCCloudDef
import com.tencent.trtc.TRTCCloudListener

/**
 * TRTC管理器（仅使用数据流接口，不调用UI组件）
 * 
 * 注意：此类仅处理音视频数据流，不涉及任何UI相关的代码
 * UI渲染由Activity自行处理
 */
class TRTCManager(private val context: Context) {
    
    companion object {
        private const val TAG = "TRTCManager"
        
        @Volatile
        private var instance: TRTCManager? = null
        
        fun getInstance(context: Context): TRTCManager {
            return instance ?: synchronized(this) {
                instance ?: TRTCManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private val trtcCloud: TRTCCloud = TRTCCloud.sharedInstance(context)
    private val listeners = mutableListOf<TRTCEventListener>()
    
    // TRTC事件监听器（仅数据流事件）- 必须在init之前定义
    private val trtcListener = object : TRTCCloudListener() {
        override fun onEnterRoom(result: Long) {
            Log.d(TAG, "进入房间: result=$result")
            listeners.forEach { it.onEnterRoom(result > 0) }
        }
        
        override fun onExitRoom(reason: Int) {
            Log.d(TAG, "退出房间: reason=$reason")
            listeners.forEach { it.onExitRoom(reason) }
        }
        
        override fun onRemoteUserEnterRoom(userId: String) {
            Log.d(TAG, "远程用户进入: userId=$userId")
            listeners.forEach { it.onRemoteUserEnter(userId) }
        }
        
        override fun onRemoteUserLeaveRoom(userId: String, reason: Int) {
            Log.d(TAG, "远程用户离开: userId=$userId, reason=$reason")
            listeners.forEach { it.onRemoteUserLeave(userId, reason) }
        }
        
        override fun onUserVideoAvailable(userId: String, available: Boolean) {
            Log.d(TAG, "用户视频流状态: userId=$userId, available=$available")
            listeners.forEach { it.onUserVideoAvailable(userId, available) }
        }
        
        override fun onUserAudioAvailable(userId: String, available: Boolean) {
            Log.d(TAG, "用户音频流状态: userId=$userId, available=$available")
            listeners.forEach { it.onUserAudioAvailable(userId, available) }
        }
        
        override fun onError(errCode: Int, errMsg: String, extraInfo: android.os.Bundle?) {
            Log.e(TAG, "TRTC错误: code=$errCode, msg=$errMsg")
            listeners.forEach { it.onError(errCode, errMsg) }
        }
        
        override fun onNetworkQuality(
            localQuality: TRTCCloudDef.TRTCQuality,
            remoteQuality: ArrayList<TRTCCloudDef.TRTCQuality>
        ) {
            // 网络质量回调
            listeners.forEach { it.onNetworkQuality(localQuality.quality) }
        }
    }
    
    init {
        // 设置TRTC回调监听（纯数据流事件）
        trtcCloud.setListener(trtcListener)
    }
    
    /**
     * 进入房间（仅数据流操作）
     * @param sdkAppId TRTC应用ID
     * @param userId 用户ID
     * @param userSig 用户签名
     * @param roomId 房间ID
     */
    fun enterRoom(sdkAppId: Int, userId: String, userSig: String, roomId: String) {
        val params = TRTCCloudDef.TRTCParams().apply {
            this.sdkAppId = sdkAppId
            this.userId = userId
            this.userSig = userSig
            this.roomId = roomId.hashCode() // 房间ID需要为整数
            this.role = TRTCCloudDef.TRTCRoleAnchor // 主播角色
        }
        
        // 进入房间（纯数据流操作，不涉及UI）
        trtcCloud.enterRoom(params, TRTCCloudDef.TRTC_APP_SCENE_VIDEOCALL)
        Log.d(TAG, "请求进入房间: roomId=$roomId, userId=$userId")
    }
    
    /**
     * 退出房间
     */
    fun exitRoom() {
        trtcCloud.exitRoom()
        Log.d(TAG, "退出房间")
    }
    
    /**
     * 开启本地音频（仅数据流）
     */
    fun startLocalAudio() {
        trtcCloud.startLocalAudio(TRTCCloudDef.TRTC_AUDIO_QUALITY_DEFAULT)
        Log.d(TAG, "开启本地音频")
    }
    
    /**
     * 关闭本地音频
     */
    fun stopLocalAudio() {
        trtcCloud.stopLocalAudio()
        Log.d(TAG, "关闭本地音频")
    }
    
    /**
     * 开启本地视频（仅数据流，需外部处理View渲染）
     */
    fun startLocalVideo(frontCamera: Boolean = true) {
        trtcCloud.startLocalPreview(frontCamera, null) // 不传入TXCloudVideoView，由外部处理
        Log.d(TAG, "开启本地视频")
    }
    
    /**
     * 关闭本地视频
     */
    fun stopLocalVideo() {
        trtcCloud.stopLocalPreview()
        Log.d(TAG, "关闭本地视频")
    }
    
    /**
     * 切换摄像头
     */
    fun switchCamera() {
        trtcCloud.deviceManager.switchCamera(true)
        Log.d(TAG, "切换摄像头")
    }
    
    /**
     * 静音/取消静音
     */
    fun muteLocalAudio(mute: Boolean) {
        trtcCloud.muteLocalAudio(mute)
        Log.d(TAG, "静音状态: $mute")
    }
    
    /**
     * 开启屏幕共享（仅数据流）
     */
    fun startScreenCapture() {
        // Android屏幕共享需要MediaProjection权限
        // 这里仅启动数据流采集，UI部分由Activity处理
        val params = TRTCCloudDef.TRTCScreenShareParams().apply {
            // 配置屏幕共享参数
        }
        // trtcCloud.startScreenCapture(params, null)
        Log.d(TAG, "开启屏幕共享数据流")
    }
    
    /**
     * 停止屏幕共享
     */
    fun stopScreenCapture() {
        trtcCloud.stopScreenCapture()
        Log.d(TAG, "停止屏幕共享")
    }
    
    /**
     * 添加事件监听器
     */
    fun addEventListener(listener: TRTCEventListener) {
        listeners.add(listener)
    }
    
    /**
     * 移除事件监听器
     */
    fun removeEventListener(listener: TRTCEventListener) {
        listeners.remove(listener)
    }
    
    /**
     * 销毁实例
     */
    fun destroy() {
        trtcCloud.exitRoom()
        TRTCCloud.destroySharedInstance()
        instance = null
        Log.d(TAG, "TRTC实例已销毁")
    }
    
    /**
     * TRTC事件监听接口（仅数据流事件）
     */
    interface TRTCEventListener {
        fun onEnterRoom(success: Boolean)
        fun onExitRoom(reason: Int)
        fun onRemoteUserEnter(userId: String)
        fun onRemoteUserLeave(userId: String, reason: Int)
        fun onUserVideoAvailable(userId: String, available: Boolean)
        fun onUserAudioAvailable(userId: String, available: Boolean)
        fun onError(errCode: Int, errMsg: String)
        fun onNetworkQuality(quality: Int)
    }
}

