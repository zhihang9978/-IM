package com.lanxin.im.trtc

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.lanxin.im.R

/**
 * 屏幕共享Activity
 * 使用TRTCManager的纯数据流接口进行屏幕采集和共享
 */
class ScreenShareActivity : AppCompatActivity(), TRTCManager.TRTCEventListener {
    
    private lateinit var trtcManager: TRTCManager
    private lateinit var tvStatus: TextView
    private lateinit var tvDuration: TextView
    private lateinit var tvPeerName: TextView
    private lateinit var btnEndShare: Button
    
    private var shareDuration = 0
    private val handler = Handler(Looper.getMainLooper())
    private var roomId: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen_share)
        
        initViews()
        initTRTC()
        startScreenShare()
        startDurationTimer()
    }
    
    private fun initViews() {
        tvStatus = findViewById(R.id.tv_status)
        tvDuration = findViewById(R.id.tv_duration)
        tvPeerName = findViewById(R.id.tv_peer_name)
        btnEndShare = findViewById(R.id.btn_end_share)
        
        val peerName = intent.getStringExtra("peer_name") ?: "对方"
        tvPeerName.text = "与 $peerName 共享中"
        
        btnEndShare.setOnClickListener {
            endScreenShare()
        }
    }
    
    private fun initTRTC() {
        trtcManager = TRTCManager.getInstance(this)
        trtcManager.addEventListener(this)
    }
    
    private fun startScreenShare() {
        roomId = intent.getStringExtra("room_id")
        
        // 开启屏幕共享（纯数据流操作）
        trtcManager.startScreenCapture()
        
        // TODO: 通知服务器屏幕共享开始，记录操作日志
        
        updateStatus("屏幕共享中...")
    }
    
    private fun startDurationTimer() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!isFinishing) {
                    shareDuration++
                    updateDuration()
                    handler.postDelayed(this, 1000)
                }
            }
        }, 1000)
    }
    
    private fun endScreenShare() {
        handler.removeCallbacksAndMessages(null)
        trtcManager.stopScreenCapture()
        
        // TODO: 通知服务器屏幕共享结束，记录时长
        
        finish()
    }
    
    private fun updateStatus(status: String) {
        runOnUiThread {
            tvStatus.text = status
        }
    }
    
    private fun updateDuration() {
        val minutes = shareDuration / 60
        val seconds = shareDuration % 60
        runOnUiThread {
            tvDuration.text = String.format("%02d:%02d", minutes, seconds)
        }
    }
    
    // ==================== TRTC事件回调 ====================
    
    override fun onEnterRoom(success: Boolean) {
        if (success) {
            updateStatus("屏幕共享中...")
        }
    }
    
    override fun onExitRoom(reason: Int) {
        updateStatus("共享已结束")
    }
    
    override fun onRemoteUserEnter(userId: String) {
        // 对方加入房间
    }
    
    override fun onRemoteUserLeave(userId: String, reason: Int) {
        updateStatus("对方已离开")
        handler.postDelayed({ endScreenShare() }, 2000)
    }
    
    override fun onUserVideoAvailable(userId: String, available: Boolean) {
        // 屏幕共享不处理
    }
    
    override fun onUserAudioAvailable(userId: String, available: Boolean) {
        // 屏幕共享不处理
    }
    
    override fun onError(errCode: Int, errMsg: String) {
        updateStatus("错误: $errMsg")
    }
    
    override fun onNetworkQuality(quality: Int) {
        // 网络质量监控
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        trtcManager.removeEventListener(this)
        trtcManager.stopScreenCapture()
    }
}

