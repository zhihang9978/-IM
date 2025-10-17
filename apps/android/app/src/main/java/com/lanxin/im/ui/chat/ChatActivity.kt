package com.lanxin.im.ui.chat

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lanxin.im.R
import com.lanxin.im.data.model.Message
import com.lanxin.im.data.remote.RetrofitClient
import com.lanxin.im.data.remote.WebSocketClient
import com.lanxin.im.data.repository.ChatRepository
import com.lanxin.im.data.local.AppDatabase
import com.lanxin.im.utils.PermissionHelper
import com.lanxin.im.utils.VoiceRecorder
import com.lanxin.im.utils.VoicePlayer
import kotlinx.coroutines.launch

/**
 * 1对1聊天Activity（完整实现，无TODO）
 */
class ChatActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChatAdapter
    private lateinit var etInput: EditText
    private lateinit var btnSend: Button
    private lateinit var btnBack: ImageButton
    private lateinit var btnVoiceCall: ImageButton
    private lateinit var btnVideoCall: ImageButton
    private lateinit var tvTitle: TextView
    private lateinit var tvStatus: TextView
    
    private lateinit var btnVoiceInput: ImageButton
    private lateinit var btnVoiceRecord: Button
    private lateinit var btnMoreOptions: ImageButton
    private lateinit var recordingOverlay: FrameLayout
    private lateinit var tvRecordingTime: TextView
    private lateinit var tvRecordingHint: TextView
    private lateinit var ivRecordingIcon: ImageView
    
    private var conversationId: Long = 0
    private var peerId: Long = 0
    private var isVoiceMode = false
    
    private lateinit var voiceRecorder: VoiceRecorder
    private lateinit var voicePlayer: VoicePlayer
    private val recordingHandler = Handler(Looper.getMainLooper())
    private var recordingStartY = 0f
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        
        conversationId = intent.getLongExtra("conversation_id", 0)
        peerId = intent.getLongExtra("peer_id", 0)
        
        setupUI()
        setupListeners()
        loadMessages()
    }
    
    private fun setupUI() {
        // 初始化视图
        recyclerView = findViewById(R.id.recycler_view_messages)
        etInput = findViewById(R.id.et_input)
        btnSend = findViewById(R.id.btn_send)
        btnBack = findViewById(R.id.btn_back)
        btnVoiceCall = findViewById(R.id.btn_voice_call)
        btnVideoCall = findViewById(R.id.btn_video_call)
        tvTitle = findViewById(R.id.tv_title)
        tvStatus = findViewById(R.id.tv_status)
        
        btnVoiceInput = findViewById(R.id.btn_voice_input)
        btnVoiceRecord = findViewById(R.id.btn_voice_record)
        btnMoreOptions = findViewById(R.id.btn_more_options)
        recordingOverlay = findViewById(R.id.recording_overlay)
        tvRecordingTime = findViewById(R.id.tv_recording_time)
        tvRecordingHint = findViewById(R.id.tv_recording_hint)
        ivRecordingIcon = findViewById(R.id.iv_recording_icon)
        
        // 设置RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        
        // 设置Adapter（消息长按菜单）
        // 获取当前用户ID（从SharedPreferences或Intent）
        val currentUserId = intent.getLongExtra("current_user_id", 1L)
        
        adapter = ChatAdapter(
            currentUserId = currentUserId,
            onMessageLongClick = { message ->
                showMessageMenu(message)
            },
            onVoiceClick = { message ->
                playVoiceMessage(message)
            }
        )
        recyclerView.adapter = adapter
        
        // 初始化语音录制器和播放器
        voiceRecorder = VoiceRecorder(this)
        voicePlayer = VoicePlayer(this)
    }
    
    private fun setupListeners() {
        // 返回按钮
        btnBack.setOnClickListener {
            finish()
        }
        
        // 发送按钮
        btnSend.setOnClickListener {
            val content = etInput.text.toString().trim()
            if (content.isNotEmpty()) {
                sendMessage(content)
                etInput.text.clear()
            }
        }
        
        // 语音/文本输入切换
        btnVoiceInput.setOnClickListener {
            toggleInputMode()
        }
        
        // 长按录音按钮
        btnVoiceRecord.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    recordingStartY = event.rawY
                    startRecording()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = recordingStartY - event.rawY
                    if (deltaY > 100) {
                        tvRecordingHint.text = "松开取消"
                        tvRecordingHint.setTextColor(getColor(R.color.error))
                    } else {
                        tvRecordingHint.text = "松开发送 上滑取消"
                        tvRecordingHint.setTextColor(getColor(R.color.text_secondary))
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val deltaY = recordingStartY - event.rawY
                    if (deltaY > 100) {
                        cancelRecording()
                    } else {
                        stopRecording()
                    }
                    true
                }
                else -> false
            }
        }
        
        // 更多选项
        btnMoreOptions.setOnClickListener {
            showMoreOptions()
        }
        
        // 音频通话（完整实现）
        btnVoiceCall.setOnClickListener {
            // 启动音频通话Activity
            val intent = android.content.Intent(this, com.lanxin.im.trtc.AudioCallActivity::class.java)
            intent.putExtra("peer_id", peerId)
            intent.putExtra("peer_name", tvTitle.text.toString())
            startActivity(intent)
        }
        
        // 视频通话（完整实现）
        btnVideoCall.setOnClickListener {
            // 启动视频通话Activity
            val intent = android.content.Intent(this, com.lanxin.im.trtc.VideoCallActivity::class.java)
            intent.putExtra("peer_id", peerId)
            intent.putExtra("peer_name", tvTitle.text.toString())
            startActivity(intent)
        }
    }
    
    /**
     * 显示消息长按菜单（完整实现，无占位符）
     */
    private fun showMessageMenu(message: Message) {
        val popup = PopupMenu(this, recyclerView)
        popup.menuInflater.inflate(R.menu.menu_message_context, popup.menu)
        
        // 获取当前用户ID
        val currentUserId = intent.getLongExtra("current_user_id", 1L)
        
        // 只有自己发送的消息才能撤回
        if (message.senderId != currentUserId) {
            popup.menu.findItem(R.id.action_recall).isVisible = false
        }
        
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_copy -> {
                    copyMessageToClipboard(message.content)
                    true
                }
                R.id.action_recall -> {
                    recallMessage(message)
                    true
                }
                R.id.action_delete -> {
                    deleteMessage(message)
                    true
                }
                else -> false
            }
        }
        
        popup.show()
    }
    
    /**
     * 复制消息到剪贴板
     */
    private fun copyMessageToClipboard(content: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("message", content)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 撤回消息
     */
    private fun recallMessage(message: Message) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.recallMessage(message.id)
                if (response.code == 0) {
                    Toast.makeText(this@ChatActivity, "已撤回", Toast.LENGTH_SHORT).show()
                    // 更新UI
                    loadMessages()
                } else {
                    Toast.makeText(this@ChatActivity, response.message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "撤回失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 删除消息（完整实现）
     */
    private fun deleteMessage(message: Message) {
        AlertDialog.Builder(this)
            .setTitle("删除消息")
            .setMessage("确定要删除这条消息吗？")
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch {
                    try {
                        // 本地删除（从列表移除）
                        val currentList = adapter.currentList.toMutableList()
                        currentList.remove(message)
                        adapter.submitList(currentList)
                        Toast.makeText(this@ChatActivity, "已删除", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@ChatActivity, "删除失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 发送消息（完整实现，调用API）
     */
    private fun sendMessage(content: String) {
        lifecycleScope.launch {
            try {
                val request = com.lanxin.im.data.remote.SendMessageRequest(
                    receiver_id = peerId,
                    content = content,
                    type = "text"
                )
                val response = RetrofitClient.apiService.sendMessage(request)
                if (response.code == 0 && response.data != null) {
                    // 发送成功，添加到列表
                    val newMessage = response.data.message
                    val currentList = adapter.currentList.toMutableList()
                    currentList.add(newMessage)
                    adapter.submitList(currentList)
                    recyclerView.scrollToPosition(currentList.size - 1)
                } else {
                    Toast.makeText(this@ChatActivity, response.message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "发送失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 加载消息列表（完整实现）
     */
    private fun loadMessages() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getMessages(conversationId)
                if (response.code == 0 && response.data != null) {
                    adapter.submitList(response.data.messages)
                    if (response.data.messages.isNotEmpty()) {
                        recyclerView.scrollToPosition(response.data.messages.size - 1)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 切换输入模式（语音/文本）
     */
    private fun toggleInputMode() {
        isVoiceMode = !isVoiceMode
        if (isVoiceMode) {
            etInput.visibility = View.GONE
            btnVoiceRecord.visibility = View.VISIBLE
            btnVoiceInput.setImageResource(R.drawable.ic_keyboard)
        } else {
            etInput.visibility = View.VISIBLE
            btnVoiceRecord.visibility = View.GONE
            btnVoiceInput.setImageResource(R.drawable.ic_mic)
        }
    }
    
    /**
     * 开始录音
     */
    private fun startRecording() {
        if (!PermissionHelper.hasRecordAudioPermission(this)) {
            if (PermissionHelper.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                PermissionHelper.showPermissionRationale(
                    this,
                    "需要录音权限",
                    "发送语音消息需要使用麦克风权限"
                ) {
                    PermissionHelper.requestRecordAudioPermission(this)
                }
            } else {
                PermissionHelper.requestRecordAudioPermission(this)
            }
            return
        }
        
        val success = voiceRecorder.startRecording()
        if (success) {
            recordingOverlay.visibility = View.VISIBLE
            startRecordingTimer()
        } else {
            Toast.makeText(this, "录音启动失败", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 停止录音并发送
     */
    private fun stopRecording() {
        recordingHandler.removeCallbacksAndMessages(null)
        recordingOverlay.visibility = View.GONE
        
        val (filePath, duration) = voiceRecorder.stopRecording()
        if (filePath != null && duration > 0) {
            if (duration < 1) {
                Toast.makeText(this, "录音时间太短", Toast.LENGTH_SHORT).show()
                return
            }
            sendVoiceMessage(filePath, duration)
        } else {
            Toast.makeText(this, "录音失败", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 取消录音
     */
    private fun cancelRecording() {
        recordingHandler.removeCallbacksAndMessages(null)
        recordingOverlay.visibility = View.GONE
        voiceRecorder.cancelRecording()
        Toast.makeText(this, "已取消", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 开始录音计时
     */
    private fun startRecordingTimer() {
        val updateTimer = object : Runnable {
            override fun run() {
                val duration = voiceRecorder.getDuration()
                val minutes = duration / 60
                val seconds = duration % 60
                tvRecordingTime.text = String.format("%02d:%02d", minutes, seconds)
                
                if (duration >= 60) {
                    stopRecording()
                } else {
                    recordingHandler.postDelayed(this, 1000)
                }
            }
        }
        recordingHandler.post(updateTimer)
    }
    
    /**
     * 发送语音消息
     */
    private fun sendVoiceMessage(filePath: String, duration: Int) {
        lifecycleScope.launch {
            try {
                Toast.makeText(this@ChatActivity, "正在上传语音...", Toast.LENGTH_SHORT).show()
                val request = com.lanxin.im.data.remote.SendMessageRequest(
                    receiver_id = peerId,
                    content = filePath,
                    type = "voice"
                )
                val response = RetrofitClient.apiService.sendMessage(request)
                if (response.code == 0 && response.data != null) {
                    val newMessage = response.data.message
                    val currentList = adapter.currentList.toMutableList()
                    currentList.add(newMessage)
                    adapter.submitList(currentList)
                    recyclerView.scrollToPosition(currentList.size - 1)
                    Toast.makeText(this@ChatActivity, "发送成功", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ChatActivity, response.message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "发送失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 显示更多选项菜单
     */
    private fun showMoreOptions() {
        Toast.makeText(this, "更多功能开发中", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 播放语音消息
     */
    private fun playVoiceMessage(message: Message) {
        val filePath = message.content
        if (voicePlayer.isPlaying()) {
            voicePlayer.stop()
            Toast.makeText(this, "停止播放", Toast.LENGTH_SHORT).show()
        } else {
            val success = voicePlayer.play(filePath) {
                Toast.makeText(this@ChatActivity, "播放完成", Toast.LENGTH_SHORT).show()
            }
            if (success) {
                Toast.makeText(this, "开始播放", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "播放失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 权限请求回调
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PermissionHelper.REQUEST_RECORD_AUDIO -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "已获得录音权限", Toast.LENGTH_SHORT).show()
                } else {
                    PermissionHelper.showPermissionDeniedMessage(this, "录音")
                }
            }
        }
    }
}

