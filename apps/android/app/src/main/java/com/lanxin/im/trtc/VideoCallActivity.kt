package com.lanxin.im.trtc

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.lanxin.im.R
import com.tencent.rtmp.ui.TXCloudVideoView

/**
 * 视频通话Activity
 * 使用TRTCManager的纯数据流接口，自定义UI渲染
 */
class VideoCallActivity : AppCompatActivity(), TRTCManager.TRTCEventListener {
    
    private lateinit var trtcManager: TRTCManager
    private lateinit var tvStatus: TextView
    private lateinit var tvDuration: TextView
    private lateinit var localVideoView: TXCloudVideoView
    private lateinit var remoteVideoView: TXCloudVideoView
    private lateinit var btnMute: Button
    private lateinit var btnCamera: Button
    private lateinit var btnSwitchCamera: Button
    private lateinit var btnHangup: Button
    
    private var isMuted = false
    private var isCameraOff = false
    private var roomId: String? = null
    private var peerId: Long = 0
    private var callDuration = 0
    private val handler = Handler(Looper.getMainLooper())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_call)
        
        initViews()
        initTRTC()
        startCall()
        startDurationTimer()
    }
    
    private fun initViews() {
        tvStatus = findViewById(R.id.tv_status)
        tvDuration = findViewById(R.id.tv_duration)
        localVideoView = findViewById(R.id.local_video_view)
        remoteVideoView = findViewById(R.id.remote_video_view)
        btnMute = findViewById(R.id.btn_mute)
        btnCamera = findViewById(R.id.btn_camera)
        btnSwitchCamera = findViewById(R.id.btn_switch_camera)
        btnHangup = findViewById(R.id.btn_hangup)
        
        // 静音按钮
        btnMute.setOnClickListener {
            isMuted = !isMuted
            trtcManager.muteLocalAudio(isMuted)
            updateMuteButtonUI()
        }
        
        // 摄像头开关
        btnCamera.setOnClickListener {
            isCameraOff = !isCameraOff
            if (isCameraOff) {
                trtcManager.stopLocalVideo()
                localVideoView.visibility = View.GONE
            } else {
                trtcManager.startLocalVideo(true)
                localVideoView.visibility = View.VISIBLE
            }
            updateCameraButtonUI()
        }
        
        // 切换摄像头
        btnSwitchCamera.setOnClickListener {
            trtcManager.switchCamera()
        }
        
        // 挂断
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
        
        // TODO: 从服务器获取TRTC凭证
        // 示例数据
        val sdkAppId = 1400000000
        val userId = "user_123"
        val userSig = "user_sig_from_server"
        roomId = "room_123_456"
        
        // 进入房间
        trtcManager.enterRoom(sdkAppId, userId, userSig, roomId!!)
        
        // 开启本地音视频（数据流操作）
        trtcManager.startLocalAudio()
        trtcManager.startLocalVideo(true)
        
        // 设置本地视频渲染（这里才涉及UI）
        // localVideoView由Activity管理，不是TRTC UI组件
        
        updateStatus("正在连接...")
    }
    
    private fun startDurationTimer() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!isFinishing) {
                    callDuration++
                    updateDuration()
                    handler.postDelayed(this, 1000)
                }
            }
        }, 1000)
    }
    
    private fun endCall() {
        handler.removeCallbacksAndMessages(null)
        trtcManager.stopLocalAudio()
        trtcManager.stopLocalVideo()
        trtcManager.exitRoom()
        
        // TODO: 通知服务器通话结束，记录通话时长
        
        finish()
    }
    
    private fun updateStatus(status: String) {
        runOnUiThread {
            tvStatus.text = status
        }
    }
    
    private fun updateDuration() {
        val minutes = callDuration / 60
        val seconds = callDuration % 60
        runOnUiThread {
            tvDuration.text = String.format("%02d:%02d", minutes, seconds)
            tvDuration.visibility = View.VISIBLE
        }
    }
    
    private fun updateMuteButtonUI() {
        btnMute.text = if (isMuted) "取消静音" else "静音"
        btnMute.alpha = if (isMuted) 0.6f else 1.0f
    }
    
    private fun updateCameraButtonUI() {
        btnCamera.text = if (isCameraOff) "开启摄像头" else "关闭摄像头"
        btnCamera.alpha = if (isCameraOff) 0.6f else 1.0f
    }
    
    // ==================== TRTC事件回调 ====================
    
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
        handler.postDelayed({ endCall() }, 2000)
    }
    
    override fun onUserVideoAvailable(userId: String, available: Boolean) {
        // 远程用户视频流状态变化
        runOnUiThread {
            if (available) {
                remoteVideoView.visibility = View.VISIBLE
                // TODO: 调用TRTC SDK设置远程视频渲染到remoteVideoView
            } else {
                remoteVideoView.visibility = View.GONE
            }
        }
    }
    
    override fun onUserAudioAvailable(userId: String, available: Boolean) {
        // 远程音频流状态
    }
    
    override fun onError(errCode: Int, errMsg: String) {
        updateStatus("错误: $errMsg")
    }
    
    override fun onNetworkQuality(quality: Int) {
        // 可根据网络质量显示提示
        // quality: 0=未知, 1=极佳, 2=较好, 3=一般, 4=差, 5=极差, 6=断开
        if (quality >= 4) {
            updateStatus("网络信号弱")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        trtcManager.removeEventListener(this)
        trtcManager.stopLocalAudio()
        trtcManager.stopLocalVideo()
        trtcManager.exitRoom()
    }
}

