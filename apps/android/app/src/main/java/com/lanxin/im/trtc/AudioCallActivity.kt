package com.lanxin.im.trtc

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.lanxin.im.R
import com.lanxin.im.data.remote.RetrofitClient

/**
 * 音频通话Activity
 * 使用TRTCManager的纯数据流接口，不依赖TRTC UI组件
 */
class AudioCallActivity : AppCompatActivity(), TRTCManager.TRTCEventListener {
    
    companion object {
        private const val TAG = "AudioCallActivity"
    }
    
    private lateinit var trtcManager: TRTCManager
    private lateinit var tvStatus: TextView
    private lateinit var btnMute: Button
    private lateinit var btnHangup: Button
    
    private var isMuted = false
    private var roomId: String? = null
    private var peerId: Long = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_call)
        
        initViews()
        initTRTC()
        startCall()
    }
    
    private fun initViews() {
        tvStatus = findViewById(R.id.tv_status)
        btnMute = findViewById(R.id.btn_mute)
        btnHangup = findViewById(R.id.btn_hangup)
        
        btnMute.setOnClickListener {
            isMuted = !isMuted
            trtcManager.muteLocalAudio(isMuted)
            btnMute.text = if (isMuted) "取消静音" else "静音"
        }
        
        btnHangup.setOnClickListener {
            endCall()
        }
    }
    
    private fun initTRTC() {
        trtcManager = TRTCManager.getInstance(this)
        trtcManager.addEventListener(this)
    }
    
    private fun startCall() {
        peerId = intent.getLongExtra("peer_id", 0)
        
        val sdkAppId = 1400000000
        val userId = "user_123"
        val userSig = "user_sig_from_server"
        roomId = "room_123_456"
        
        trtcManager.enterRoom(sdkAppId, userId, userSig, roomId!!)
        trtcManager.startLocalAudio()
        
        updateStatus("正在连接...")
    }
    
    private fun endCall() {
        trtcManager.stopLocalAudio()
        trtcManager.exitRoom()
        
        finish()
    }
    
    private fun updateStatus(status: String) {
        runOnUiThread {
            tvStatus.text = status
        }
    }
    
    // ==================== TRTC事件回调（数据流事件） ====================
    
    override fun onEnterRoom(success: Boolean) {
        if (success) {
            updateStatus("通话中...")
        } else {
            updateStatus("连接失败")
        }
    }
    
    override fun onExitRoom(reason: Int) {
        updateStatus("通话已结束")
    }
    
    override fun onRemoteUserEnter(userId: String) {
        updateStatus("对方已接听")
    }
    
    override fun onRemoteUserLeave(userId: String, reason: Int) {
        updateStatus("对方已挂断")
        endCall()
    }
    
    override fun onUserVideoAvailable(userId: String, available: Boolean) {
        // 音频通话不处理视频流
    }
    
    override fun onUserAudioAvailable(userId: String, available: Boolean) {
        Log.d(TAG, "远程音频流: userId=$userId, available=$available")
    }
    
    override fun onError(errCode: Int, errMsg: String) {
        updateStatus("错误: $errMsg")
    }
    
    override fun onNetworkQuality(quality: Int) {
        // 可以根据网络质量显示信号强度
    }
    
    override fun onDestroy() {
        super.onDestroy()
        trtcManager.removeEventListener(this)
        trtcManager.stopLocalAudio()
        trtcManager.exitRoom()
    }
}

