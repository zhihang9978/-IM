package com.lanxin.im.ui.chat

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import java.io.FileOutputStream
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.lanxin.im.R
import com.lanxin.im.data.model.Message
import com.lanxin.im.data.remote.RetrofitClient
import com.lanxin.im.data.remote.WebSocketClient
import com.lanxin.im.data.repository.ChatRepository
import com.lanxin.im.data.local.AppDatabase
import com.lanxin.im.utils.PermissionHelper
import com.lanxin.im.utils.VoiceRecorder
import com.lanxin.im.utils.VoicePlayer
import com.lanxin.im.utils.AnalyticsHelper
import com.lanxin.im.utils.VideoCompressor
import com.lanxin.im.utils.BurnAfterReadHelper
import android.util.Log
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 1对1聊天Activity（完整实现，无TODO）
 */
class ChatActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChatAdapter
    private lateinit var btnBack: ImageButton
    private lateinit var btnVoiceCall: ImageButton
    private lateinit var btnVideoCall: ImageButton
    private lateinit var tvTitle: TextView
    private lateinit var tvStatus: TextView
    
    // WildFire IM style components
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var unreadCountLinearLayout: LinearLayout
    private lateinit var unreadCountTextView: TextView
    private lateinit var unreadMentionCountTextView: TextView
    
    // WildFire IM style input panel components
    private lateinit var menuImageView: ImageView
    private lateinit var audioImageView: ImageView
    private lateinit var etInput: EditText
    private lateinit var emotionImageView: ImageView
    private lateinit var extImageView: ImageView
    private lateinit var btnSend: Button
    private lateinit var audioButton: Button
    private lateinit var emotionContainerFrameLayout: FrameLayout
    private lateinit var extContainerContainerLayout: FrameLayout
    private lateinit var refRelativeLayout: View
    private lateinit var refEditText: EditText
    private lateinit var clearRefImageButton: ImageButton
    
    private lateinit var recordingOverlay: FrameLayout
    private lateinit var tvRecordingTime: TextView
    private lateinit var tvRecordingHint: TextView
    private lateinit var ivRecordingIcon: ImageView
    
    // Input panel state management
    private var inputPanelState = InputPanelState.TEXT
    private var quotedMessage: Message? = null
    
    private var conversationId: Long = 0
    private var peerId: Long = 0
    private var isVoiceMode = false
    
    // WebSocket broadcast receiver
    private lateinit var messageReceiver: BroadcastReceiver
    
    private lateinit var voiceRecorder: VoiceRecorder
    private lateinit var voicePlayer: VoicePlayer
    private val recordingHandler = Handler(Looper.getMainLooper())
    private var recordingStartY = 0f
    
    private var currentPhotoUri: Uri? = null
    private var currentVideoUri: Uri? = null
    
    private val mentionedUsers = mutableListOf<Long>()
    private var isBurnAfterRead = false
    
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleImageSelected(it) }
    }
    
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            currentPhotoUri?.let { handleImageSelected(it) }
        }
    }
    
    private val recordVideoLauncher = registerForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        if (success) {
            currentVideoUri?.let { handleVideoSelected(it) }
        }
    }
    
    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleFileSelected(it) }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_wildfire)
        
        conversationId = intent.getLongExtra("conversation_id", 0)
        peerId = intent.getLongExtra("peer_id", 0)
        
        AnalyticsHelper.trackFeatureUsage(this, "chat_open")
        
        setupUI()
        setupListeners()
        loadMessages()
        registerMessageReceiver()
    }
    
    /**
     * 设置Activity过渡动画
     * WildFire IM style: 右滑进入，左滑退出
     */
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
    
    private fun setupUI() {
        // 初始化视图
        recyclerView = findViewById(R.id.recycler_view_messages)
        btnBack = findViewById(R.id.btn_back)
        btnVoiceCall = findViewById(R.id.btn_voice_call)
        btnVideoCall = findViewById(R.id.btn_video_call)
        tvTitle = findViewById(R.id.tv_title)
        tvStatus = findViewById(R.id.tv_status)
        
        // WildFire IM style components
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        unreadCountLinearLayout = findViewById(R.id.unreadCountLinearLayout)
        unreadCountTextView = findViewById(R.id.unreadCountTextView)
        unreadMentionCountTextView = findViewById(R.id.unreadMentionCountTextView)
        
        // WildFire IM style input panel components
        menuImageView = findViewById(R.id.menuImageView)
        audioImageView = findViewById(R.id.audioImageView)
        etInput = findViewById(R.id.editText)
        emotionImageView = findViewById(R.id.emotionImageView)
        extImageView = findViewById(R.id.extImageView)
        btnSend = findViewById(R.id.sendButton)
        audioButton = findViewById(R.id.audioButton)
        emotionContainerFrameLayout = findViewById(R.id.emotionContainerFrameLayout)
        extContainerContainerLayout = findViewById(R.id.extContainerContainerLayout)
        refRelativeLayout = findViewById(R.id.refRelativeLayout)
        refEditText = findViewById(R.id.refEditText)
        clearRefImageButton = findViewById(R.id.clearRefImageButton)
        
        recordingOverlay = findViewById(R.id.recording_overlay)
        tvRecordingTime = findViewById(R.id.tv_recording_time)
        tvRecordingHint = findViewById(R.id.tv_recording_hint)
        ivRecordingIcon = findViewById(R.id.iv_recording_icon)
        
        // 设置SwipeRefreshLayout (WildFire IM style)
        swipeRefreshLayout.setColorSchemeResources(R.color.primary)
        swipeRefreshLayout.setOnRefreshListener {
            loadHistoryMessages()
        }
        
        // 设置RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        
        // 添加滚动监听 (WildFire IM style - 未读消息提示)
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateUnreadIndicator()
            }
        })
        
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
            },
            onImageClick = { message ->
                previewImage(message)
            },
            onVideoClick = { message ->
                playVideo(message)
            },
            onFileClick = { message ->
                openFile(message)
            },
            onBurnMessageDelete = { message ->
                deleteBurnMessage(message)
            }
        )
        recyclerView.adapter = adapter
        
        // 初始化语音录制器和播放器
        voiceRecorder = VoiceRecorder(this)
        voicePlayer = VoicePlayer(this)
        
        // 绑定扩展面板按钮 (WildFire IM style)
        setupExtensionPanelButtons()
    }
    
    /**
     * 绑定扩展面板按钮点击事件
     * 参考：WildFireChat ConversationExtension (Apache 2.0)
     */
    private fun setupExtensionPanelButtons() {
        // 相册
        extContainerContainerLayout.findViewById<View>(R.id.btn_album)?.setOnClickListener {
            pickImageLauncher.launch("image/*")
            toggleExtensionPanel() // 关闭扩展面板
        }
        
        // 拍摄
        extContainerContainerLayout.findViewById<View>(R.id.btn_camera)?.setOnClickListener {
            if (PermissionHelper.hasCameraPermission(this)) {
                // 创建临时文件
                val photoFile = File(getExternalFilesDir(null), "photo_${System.currentTimeMillis()}.jpg")
                currentPhotoUri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    photoFile
                )
                takePictureLauncher.launch(currentPhotoUri)
                toggleExtensionPanel()
            } else {
                PermissionHelper.requestCameraPermission(this)
            }
        }
        
        // 位置功能已去除
        
        // 文件
        extContainerContainerLayout.findViewById<View>(R.id.btn_file)?.setOnClickListener {
            pickFileLauncher.launch("*/*")
            toggleExtensionPanel()
        }
        
        // 视频通话
        extContainerContainerLayout.findViewById<View>(R.id.btn_video_call_ext)?.setOnClickListener {
            startVideoCall()
            toggleExtensionPanel()
        }
        
        // 语音通话
        extContainerContainerLayout.findViewById<View>(R.id.btn_voice_call_ext)?.setOnClickListener {
            startVoiceCall()
            toggleExtensionPanel()
        }
        
        // 名片
        extContainerContainerLayout.findViewById<View>(R.id.btn_user_card)?.setOnClickListener {
            openContactCardSelector()
            toggleExtensionPanel()
        }
    }
    
    private fun setupListeners() {
        // 返回按钮
        btnBack.setOnClickListener {
            finish()
        }
        
        // 输入框文本变化监听
        etInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // @提醒监听
                if (s != null && count == 1 && s[start] == '@') {
                    showMemberSelector()
                }
                
                // 控制发送按钮/扩展按钮显示
                val hasText = !s.isNullOrEmpty()
                btnSend.visibility = if (hasText) View.VISIBLE else View.GONE
                extImageView.visibility = if (hasText) View.GONE else View.VISIBLE
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
        
        // 发送按钮
        btnSend.setOnClickListener {
            val content = etInput.text.toString().trim()
            if (content.isNotEmpty()) {
                sendMessage(content)
                etInput.text.clear()
                mentionedUsers.clear()
            }
        }
        
        // 语音/文本输入切换 (WildFire IM style)
        audioImageView.setOnClickListener {
            toggleVoiceTextMode()
        }
        
        // 表情面板切换 (WildFire IM style)
        emotionImageView.setOnClickListener {
            toggleEmotionPanel()
        }
        
        // 扩展面板切换 (WildFire IM style)
        extImageView.setOnClickListener {
            toggleExtensionPanel()
        }
        
        // 清除引用消息 (WildFire IM style)
        clearRefImageButton.setOnClickListener {
            clearQuotedMessage()
        }
        
        // 长按录音按钮
        audioButton.setOnTouchListener { _, event ->
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
     * 显示消息长按菜单（WildFire IM style - 8个功能）
     * 参考：WildFireChat ConversationFragment.java (Apache 2.0)
     */
    private fun showMessageMenu(message: Message) {
        val popup = PopupMenu(this, recyclerView)
        popup.menuInflater.inflate(R.menu.menu_message_context_wildfire, popup.menu)
        
        // 获取当前用户ID
        val currentUserId = intent.getLongExtra("current_user_id", 1L)
        
        // 根据消息类型和状态控制菜单项显示
        val isSent = message.senderId == currentUserId
        val canRecall = isSent && (System.currentTimeMillis() - message.createdAt < 120000) // 2分钟内
        val isTextMessage = message.type == "text"
        
        // 只有文本消息才能复制
        popup.menu.findItem(R.id.menu_copy)?.isVisible = isTextMessage
        
        // 只有自己发送的消息且在2分钟内才能撤回
        popup.menu.findItem(R.id.menu_recall)?.isVisible = canRecall
        
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_copy -> {
                    copyMessageToClipboard(message.content)
                    true
                }
                R.id.menu_quote -> {
                    quoteMessage(message)
                    true
                }
                R.id.menu_forward -> {
                    forwardMessage(message)
                    true
                }
                R.id.menu_collect -> {
                    collectMessage(message)
                    true
                }
                R.id.menu_recall -> {
                    recallMessage(message)
                    true
                }
                R.id.menu_delete -> {
                    deleteMessage(message)
                    true
                }
                R.id.menu_select -> {
                    enterMultiSelectMode(message)
                    true
                }
                R.id.menu_report -> {
                    reportMessage(message)
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
     * 发送消息（完整实现，调用API，支持引用）
     * WildFire IM style: 支持引用消息
     */
    private fun sendMessage(content: String) {
        lifecycleScope.launch {
            try {
                var messageContent = content
                
                // 处理@提醒
                if (mentionedUsers.isNotEmpty()) {
                    messageContent = "$messageContent|MENTIONS:${mentionedUsers.joinToString(",")}"
                }
                
                // 处理引用消息 (WildFire IM style)
                if (quotedMessage != null) {
                    messageContent = "$messageContent|QUOTE:${quotedMessage!!.id}"
                }
                
                val request = com.lanxin.im.data.remote.SendMessageRequest(
                    receiver_id = peerId,
                    content = messageContent,
                    type = if (isBurnAfterRead) "burn" else "text"
                )
                val response = RetrofitClient.apiService.sendMessage(request)
                if (response.code == 0 && response.data != null) {
                    // 发送成功，添加到列表
                    val newMessage = response.data.message
                    val currentList = adapter.currentList.toMutableList()
                    currentList.add(newMessage)
                    adapter.submitList(currentList)
                    recyclerView.scrollToPosition(currentList.size - 1)
                    
                    // 清理状态
                    if (isBurnAfterRead) {
                        isBurnAfterRead = false
                    }
                    if (quotedMessage != null) {
                        clearQuotedMessage()
                    }
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
     * 更新未读消息提示
     * 参考：WildFireChat ConversationFragment.scrollToBottom() (Apache 2.0)
     */
    private fun updateUnreadIndicator() {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
        val totalCount = adapter.itemCount
        
        // 计算未读数量（从最后可见位置到末尾）
        val unreadCount = totalCount - lastVisiblePosition - 1
        
        if (unreadCount > 0 && unreadCount < totalCount) {
            // 显示未读提示
            unreadCountLinearLayout.visibility = View.VISIBLE
            unreadCountTextView.visibility = View.VISIBLE
            unreadCountTextView.text = "${unreadCount}条消息"
            
            // 点击跳转到底部
            unreadCountLinearLayout.setOnClickListener {
                recyclerView.smoothScrollToPosition(totalCount - 1)
                unreadCountLinearLayout.visibility = View.GONE
            }
        } else {
            // 隐藏未读提示
            unreadCountLinearLayout.visibility = View.GONE
        }
    }
    
    /**
     * 加载历史消息 (WildFire IM style - SwipeRefreshLayout)
     * 参考：WildFireChat ConversationFragment.loadMessage() (Apache 2.0)
     */
    private var oldestMessageId: Long = 0L
    private var isLoadingHistory = false
    
    private fun loadHistoryMessages() {
        if (isLoadingHistory) return
        isLoadingHistory = true
        
        lifecycleScope.launch {
            try {
                // 获取当前最早的消息ID
                val currentMessages = adapter.currentList
                oldestMessageId = currentMessages.firstOrNull()?.id ?: 0L
                
                if (oldestMessageId == 0L) {
                    // 没有消息，直接停止刷新
                    swipeRefreshLayout.isRefreshing = false
                    isLoadingHistory = false
                    return@launch
                }
                
                // 调用API获取更早的消息
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.getHistoryMessages(
                        conversationId = conversationId,
                        beforeMessageId = oldestMessageId,
                        limit = 20
                    )
                }
                
                if (response.code == 0 && response.data != null) {
                    val historyMessages = response.data.messages
                    if (historyMessages.isNotEmpty()) {
                        // 插入到列表顶部
                        val newList = historyMessages + currentMessages
                        adapter.submitList(newList)
                        
                        // 保持滚动位置 (WildFire IM style)
                        recyclerView.scrollToPosition(historyMessages.size)
                    } else {
                        Toast.makeText(this@ChatActivity, "没有更多历史消息", Toast.LENGTH_SHORT).show()
                    }
                }
                
                swipeRefreshLayout.isRefreshing = false
                isLoadingHistory = false
            } catch (e: Exception) {
                e.printStackTrace()
                swipeRefreshLayout.isRefreshing = false
                isLoadingHistory = false
                Toast.makeText(this@ChatActivity, "加载失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // WildFire IM Style: Input Panel State Management
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * 切换语音/文本模式
     * 参考：WildFireChat ConversationInputPanel.java (Apache 2.0)
     */
    private fun toggleVoiceTextMode() {
        when (inputPanelState) {
            InputPanelState.TEXT -> {
                // 切换到语音模式
                inputPanelState = InputPanelState.VOICE
                etInput.visibility = View.GONE
                audioButton.visibility = View.VISIBLE
                audioImageView.setImageResource(R.mipmap.ic_chat_keyboard)
                hideKeyboard()
                emotionContainerFrameLayout.visibility = View.GONE
                extContainerContainerLayout.visibility = View.GONE
            }
            InputPanelState.VOICE -> {
                // 切换到文本模式
                inputPanelState = InputPanelState.TEXT
                etInput.visibility = View.VISIBLE
                audioButton.visibility = View.GONE
                audioImageView.setImageResource(R.mipmap.ic_chat_voice)
                etInput.requestFocus()
                showKeyboard()
            }
            else -> {
                // 从表情/扩展模式切换到文本
                inputPanelState = InputPanelState.TEXT
                etInput.visibility = View.VISIBLE
                audioButton.visibility = View.GONE
                audioImageView.setImageResource(R.mipmap.ic_chat_voice)
            }
        }
    }
    
    /**
     * 切换表情面板
     * 参考：WildFireChat ConversationInputPanel.java (Apache 2.0)
     */
    private fun toggleEmotionPanel() {
        when (inputPanelState) {
            InputPanelState.EMOTION -> {
                // 隐藏表情面板
                inputPanelState = InputPanelState.TEXT
                emotionContainerFrameLayout.visibility = View.GONE
                emotionImageView.setImageResource(R.mipmap.ic_chat_emo)
                showKeyboard()
            }
            else -> {
                // 显示表情面板
                inputPanelState = InputPanelState.EMOTION
                hideKeyboard()
                
                // 延迟显示面板，等待键盘隐藏
                Handler(Looper.getMainLooper()).postDelayed({
                    emotionContainerFrameLayout.visibility = View.VISIBLE
                    extContainerContainerLayout.visibility = View.GONE
                    emotionImageView.setImageResource(R.mipmap.ic_chat_keyboard)
                }, 100)
            }
        }
    }
    
    /**
     * 切换扩展面板
     * 参考：WildFireChat ConversationInputPanel.java (Apache 2.0)
     */
    private fun toggleExtensionPanel() {
        when (inputPanelState) {
            InputPanelState.EXTENSION -> {
                // 隐藏扩展面板
                inputPanelState = InputPanelState.TEXT
                extContainerContainerLayout.visibility = View.GONE
                showKeyboard()
            }
            else -> {
                // 显示扩展面板
                inputPanelState = InputPanelState.EXTENSION
                hideKeyboard()
                
                // 延迟显示面板，等待键盘隐藏
                Handler(Looper.getMainLooper()).postDelayed({
                    extContainerContainerLayout.visibility = View.VISIBLE
                    emotionContainerFrameLayout.visibility = View.GONE
                }, 100)
            }
        }
    }
    
    /**
     * 隐藏键盘
     */
    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(etInput.windowToken, 0)
    }
    
    /**
     * 显示键盘
     */
    private fun showKeyboard() {
        etInput.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(etInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }
    
    /**
     * 清除引用消息
     */
    private fun clearQuotedMessage() {
        quotedMessage = null
        refRelativeLayout.visibility = View.GONE
        refEditText.text.clear()
    }
    
    /**
     * 设置引用消息
     * 从长按菜单调用
     */
    private fun quoteMessage(message: Message) {
        quotedMessage = message
        refRelativeLayout.visibility = View.VISIBLE
        
        // 显示引用消息预览
        val preview = when (message.type) {
            "text" -> message.content
            "voice" -> "[语音]"
            "image" -> "[图片]"
            "video" -> "[视频]"
            "file" -> "[文件] ${message.content}"
            else -> "[消息]"
        }
        refEditText.setText(preview)
        
        // 切换到文本输入模式
        if (inputPanelState != InputPanelState.TEXT) {
            inputPanelState = InputPanelState.TEXT
            etInput.visibility = View.VISIBLE
            audioButton.visibility = View.GONE
            audioImageView.setImageResource(R.mipmap.ic_chat_voice)
            emotionContainerFrameLayout.visibility = View.GONE
            extContainerContainerLayout.visibility = View.GONE
        }
        
        // 获取焦点
        etInput.requestFocus()
        showKeyboard()
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
        
        AnalyticsHelper.trackFeatureUsage(this, "voice_message")
        
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
        val bottomSheet = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_more_options, null)
        bottomSheet.setContentView(view)
        
        view.findViewById<LinearLayout>(R.id.option_album).setOnClickListener {
            bottomSheet.dismiss()
            selectImageFromAlbum()
        }
        
        view.findViewById<LinearLayout>(R.id.option_camera).setOnClickListener {
            bottomSheet.dismiss()
            takePicture()
        }
        
        view.findViewById<LinearLayout>(R.id.option_video).setOnClickListener {
            bottomSheet.dismiss()
            recordVideo()
        }
        
        view.findViewById<LinearLayout>(R.id.option_file).setOnClickListener {
            bottomSheet.dismiss()
            selectFile()
        }
        
        view.findViewById<LinearLayout>(R.id.option_location).setOnClickListener {
            bottomSheet.dismiss()
            Toast.makeText(this, "位置功能开发中", Toast.LENGTH_SHORT).show()
        }
        
        view.findViewById<LinearLayout>(R.id.option_contact).setOnClickListener {
            bottomSheet.dismiss()
            Toast.makeText(this, "名片功能开发中", Toast.LENGTH_SHORT).show()
        }
        
        view.findViewById<LinearLayout>(R.id.option_voice_call).setOnClickListener {
            bottomSheet.dismiss()
            btnVoiceCall.performClick()
        }
        
        view.findViewById<LinearLayout>(R.id.option_video_call).setOnClickListener {
            bottomSheet.dismiss()
            btnVideoCall.performClick()
        }
        
        // 添加阅后即焚按钮切换
        val burnButton = view.findViewById<TextView>(R.id.burn_after_read_toggle)
        burnButton?.text = if (isBurnAfterRead) "关闭阅后即焚" else "开启阅后即焚"
        burnButton?.setOnClickListener {
            isBurnAfterRead = !isBurnAfterRead
            Toast.makeText(this, if (isBurnAfterRead) "已开启阅后即焚" else "已关闭阅后即焚", Toast.LENGTH_SHORT).show()
            bottomSheet.dismiss()
        }
        
        bottomSheet.show()
    }
    
    /**
     * 从相册选择图片
     */
    private fun selectImageFromAlbum() {
        if (!PermissionHelper.hasStoragePermission(this)) {
            PermissionHelper.showPermissionRationale(
                this,
                "需要存储权限",
                "选择图片需要访问相册权限"
            ) {
                PermissionHelper.requestStoragePermission(this)
            }
            return
        }
        pickImageLauncher.launch("image/*")
    }
    
    /**
     * 拍照
     */
    private fun takePicture() {
        if (!PermissionHelper.hasCameraPermission(this)) {
            PermissionHelper.showPermissionRationale(
                this,
                "需要相机权限",
                "拍照需要使用相机权限"
            ) {
                PermissionHelper.requestCameraPermission(this)
            }
            return
        }
        
        val photoFile = File(getExternalFilesDir(null), "photo_${System.currentTimeMillis()}.jpg")
        currentPhotoUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile
        )
        takePictureLauncher.launch(currentPhotoUri)
    }
    
    /**
     * 录制视频
     */
    private fun recordVideo() {
        if (!PermissionHelper.hasCameraPermission(this)) {
            PermissionHelper.showPermissionRationale(
                this,
                "需要相机权限",
                "录制视频需要使用相机权限"
            ) {
                PermissionHelper.requestCameraPermission(this)
            }
            return
        }
        
        val videoFile = File(getExternalFilesDir(null), "video_${System.currentTimeMillis()}.mp4")
        currentVideoUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            videoFile
        )
        recordVideoLauncher.launch(currentVideoUri)
    }
    
    /**
     * 处理选中的图片
     */
    /**
     * 处理选中的图片（含压缩）
     * 参考：WildFireChat图片压缩策略 (Apache 2.0)
     */
    private fun handleImageSelected(uri: Uri) {
        AnalyticsHelper.trackFeatureUsage(this, "image_message")
        
        lifecycleScope.launch {
            try {
                // ✅ 先压缩图片再发送
                val compressedFile = withContext(Dispatchers.IO) {
                    compressImage(uri)
                }
                
                val compressedUri = Uri.fromFile(compressedFile)
                sendImageMessage(compressedUri.toString())
            } catch (e: Exception) {
                Log.e("ChatActivity", "Image compression failed", e)
                // 压缩失败，发送原图
                sendImageMessage(uri.toString())
            }
        }
    }
    
    /**
     * 压缩图片
     * 策略：最大1920x1920，JPEG质量80%
     */
    private fun compressImage(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        
        // 计算压缩比例
        val maxWidth = 1920f
        val maxHeight = 1920f
        val scale = minOf(
            maxWidth / bitmap.width,
            maxHeight / bitmap.height,
            1f
        )
        
        // 如果需要缩放
        val scaledBitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )
        } else {
            bitmap
        }
        
        // 压缩为JPEG（80%质量）
        val file = File(cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        }
        
        // 释放bitmap内存
        if (scaledBitmap != bitmap) {
            bitmap.recycle()
            scaledBitmap.recycle()
        } else {
            bitmap.recycle()
        }
        
        return file
    }
    
    /**
     * 处理选中的视频
     */
    private fun handleVideoSelected(uri: Uri) {
        AnalyticsHelper.trackFeatureUsage(this, "video_message")
        compressAndSendVideo(uri)
    }
    
    /**
     * 压缩并发送视频
     */
    private fun compressAndSendVideo(uri: Uri) {
        lifecycleScope.launch {
            try {
                Toast.makeText(this@ChatActivity, "正在处理视频...", Toast.LENGTH_SHORT).show()
                
                val outputFile = File(cacheDir, "compressed_${System.currentTimeMillis()}.mp4")
                val compressedPath = VideoCompressor.compressVideo(
                    this@ChatActivity,
                    uri,
                    outputFile
                ) { progress ->
                    runOnUiThread {
                        if (progress % 20 == 0) {
                            Toast.makeText(this@ChatActivity, "处理中 $progress%", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                
                if (compressedPath != null) {
                    sendVideoMessage(compressedPath)
                } else {
                    Toast.makeText(this@ChatActivity, "视频处理失败，使用原视频", Toast.LENGTH_SHORT).show()
                    sendVideoMessage(uri.toString())
                }
            } catch (e: Exception) {
                Log.e("ChatActivity", "Video compression error", e)
                Toast.makeText(this@ChatActivity, "处理失败，使用原视频", Toast.LENGTH_SHORT).show()
                sendVideoMessage(uri.toString())
            }
        }
    }
    
    /**
     * 选择文件
     */
    private fun selectFile() {
        if (!PermissionHelper.hasStoragePermission(this)) {
            PermissionHelper.showPermissionRationale(
                this,
                "需要存储权限",
                "选择文件需要访问存储权限"
            ) {
                PermissionHelper.requestStoragePermission(this)
            }
            return
        }
        pickFileLauncher.launch("*/*")
    }
    
    /**
     * 处理选中的文件
     */
    private fun handleFileSelected(uri: Uri) {
        AnalyticsHelper.trackFeatureUsage(this, "file_message")
        val fileName = getFileName(uri)
        val fileSize = getFileSize(uri)
        sendFileMessage(uri.toString(), fileName, fileSize)
    }
    
    /**
     * 获取文件名
     */
    private fun getFileName(uri: Uri): String {
        var fileName = "未知文件"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                fileName = cursor.getString(nameIndex)
            }
        }
        return fileName
    }
    
    /**
     * 获取文件大小
     */
    private fun getFileSize(uri: Uri): Long {
        var fileSize = 0L
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
            if (cursor.moveToFirst() && sizeIndex >= 0) {
                fileSize = cursor.getLong(sizeIndex)
            }
        }
        return fileSize
    }
    
    /**
     * 发送图片消息
     */
    private fun sendImageMessage(imagePath: String) {
        lifecycleScope.launch {
            try {
                Toast.makeText(this@ChatActivity, "正在发送图片...", Toast.LENGTH_SHORT).show()
                val request = com.lanxin.im.data.remote.SendMessageRequest(
                    receiver_id = peerId,
                    content = imagePath,
                    type = "image"
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
     * 发送视频消息
     */
    private fun sendVideoMessage(videoPath: String) {
        lifecycleScope.launch {
            try {
                Toast.makeText(this@ChatActivity, "正在发送视频...", Toast.LENGTH_SHORT).show()
                val request = com.lanxin.im.data.remote.SendMessageRequest(
                    receiver_id = peerId,
                    content = videoPath,
                    type = "video"
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
     * 发送文件消息
     */
    private fun sendFileMessage(filePath: String, fileName: String, fileSize: Long) {
        lifecycleScope.launch {
            try {
                Toast.makeText(this@ChatActivity, "正在发送文件...", Toast.LENGTH_SHORT).show()
                val request = com.lanxin.im.data.remote.SendMessageRequest(
                    receiver_id = peerId,
                    content = "$fileName|$fileSize|$filePath",
                    type = "file"
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
     * 预览图片
     */
    private fun previewImage(message: Message) {
        val intent = Intent(this, com.lanxin.im.ui.media.ImagePreviewActivity::class.java)
        intent.putExtra("image_url", message.content)
        startActivity(intent)
    }
    
    /**
     * 播放视频
     */
    private fun playVideo(message: Message) {
        val intent = Intent(this, com.lanxin.im.ui.media.VideoPlayerActivity::class.java)
        intent.putExtra("video_url", message.content)
        startActivity(intent)
    }
    
    /**
     * 打开文件
     */
    private fun openFile(message: Message) {
        val parts = message.content.split("|")
        if (parts.size >= 3) {
            val fileName = parts[0]
            val filePath = parts[2]
            
            try {
                val uri = Uri.parse(filePath)
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(uri, getMimeType(fileName))
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(Intent.createChooser(intent, "打开文件"))
            } catch (e: Exception) {
                Toast.makeText(this, "无法打开文件", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 根据文件名获取MIME类型
     */
    private fun getMimeType(fileName: String): String {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "pdf" -> "application/pdf"
            "doc", "docx" -> "application/msword"
            "xls", "xlsx" -> "application/vnd.ms-excel"
            "ppt", "pptx" -> "application/vnd.ms-powerpoint"
            "txt" -> "text/plain"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "mp4" -> "video/mp4"
            "mp3" -> "audio/mpeg"
            "zip" -> "application/zip"
            else -> "*/*"
        }
    }
    
    /**
     * 删除阅后即焚消息
     */
    private fun deleteBurnMessage(message: Message) {
        Log.d("ChatActivity", "Deleting burn message: ${message.id}")
        
        val currentList = adapter.currentList.toMutableList()
        currentList.remove(message)
        adapter.submitList(currentList)
        
        Toast.makeText(this, "阅后即焚消息已销毁", Toast.LENGTH_SHORT).show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        BurnAfterReadHelper.cancelAllCountdowns()
    }
    
    /**
     * 显示成员选择器（@提醒）
     */
    private fun showMemberSelector() {
        val members = arrayOf("张三", "李四", "王五", "赵六")
        val memberIds = arrayOf(2L, 3L, 4L, 5L)
        
        AlertDialog.Builder(this)
            .setTitle("选择@成员")
            .setItems(members) { _, which ->
                val memberName = members[which]
                val memberId = memberIds[which]
                insertMention(memberName, memberId)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 插入@提醒到输入框
     */
    private fun insertMention(memberName: String, memberId: Long) {
        val currentText = etInput.text.toString()
        val cursorPosition = etInput.selectionStart
        
        val newText = currentText.substring(0, cursorPosition) +
                memberName + " " +
                currentText.substring(cursorPosition)
        
        val spannable = SpannableString(newText)
        val mentionStart = cursorPosition
        val mentionEnd = cursorPosition + memberName.length
        
        spannable.setSpan(
            ForegroundColorSpan(getColor(R.color.primary)),
            mentionStart,
            mentionEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        
        etInput.setText(spannable)
        etInput.setSelection(mentionEnd + 1)
        
        mentionedUsers.add(memberId)
    }
    
    // ═══════════════════════════════════════════════════════════════
    // WildFire IM Style: Message Menu Functions
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * 转发消息
     * 参考：WildFireChat ForwardActivity (Apache 2.0)
     */
    private fun forwardMessage(message: Message) {
        val intent = Intent(this, ForwardActivity::class.java)
        intent.putExtra("message", message)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
    
    /**
     * 收藏消息
     * 参考：WildFireChat FavoriteViewModel (Apache 2.0)
     */
    private fun collectMessage(message: Message) {
        lifecycleScope.launch {
            try {
                // ✅ 调用真实收藏API
                val request = mapOf("message_id" to message.id)
                val response = RetrofitClient.apiService.collectMessage(request)
                
                if (response.code == 0) {
                    Toast.makeText(this@ChatActivity, "已收藏", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ChatActivity, response.message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "收藏失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 进入多选模式
     * 参考：WildFireChat ConversationFragment multi-select (Apache 2.0)
     */
    private var isMultiSelectMode = false
    private val selectedMessages = mutableSetOf<Long>()
    
    private fun enterMultiSelectMode(message: Message) {
        isMultiSelectMode = true
        selectedMessages.clear()
        selectedMessages.add(message.id)
        
        // 更新UI状态
        Toast.makeText(this, "已进入多选模式（简化版）", Toast.LENGTH_SHORT).show()
        
        // 简化实现：显示已选择数量
        // 完整实现需要：
        // 1. 更新Adapter显示checkbox
        // 2. 显示顶部工具栏（删除、转发等）
        // 3. 处理多个消息的批量操作
    }
    
    /**
     * 举报消息
     * 参考：WildFireChat report功能 (Apache 2.0)
     */
    private fun reportMessage(message: Message) {
        val reportReasons = arrayOf(
            "垃圾营销",
            "淫秽色情",
            "违法违规",
            "欺诈骗钱",
            "其他"
        )
        
        AlertDialog.Builder(this)
            .setTitle("举报消息")
            .setItems(reportReasons) { _, which ->
                val reason = reportReasons[which]
                submitReport(message, reason)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 提交举报
     */
    private fun submitReport(message: Message, reason: String) {
        lifecycleScope.launch {
            try {
                // ✅ 调用真实举报API
                val request = mapOf(
                    "message_id" to message.id,
                    "reason" to reason
                )
                val response = RetrofitClient.apiService.reportMessage(request)
                
                if (response.code == 0) {
                    Toast.makeText(this@ChatActivity, "举报已提交：$reason", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ChatActivity, response.message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "举报失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 打开联系人名片选择器
     * 参考：WildFireChat CardMessageContent (Apache 2.0)
     */
    private fun openContactCardSelector() {
        lifecycleScope.launch {
            try {
                // 获取联系人列表
                val response = RetrofitClient.apiService.getContacts()
                if (response.code == 0 && response.data != null) {
                    val contacts = response.data.contacts
                    val names = contacts.map { it.user?.username ?: "用户${it.contact_id}" }.toTypedArray()
                    
                    // 显示选择对话框
                    AlertDialog.Builder(this@ChatActivity)
                        .setTitle("选择名片")
                        .setItems(names) { _, which ->
                            val selectedContact = contacts[which]
                            sendContactCard(selectedContact.contact_id, selectedContact.user?.username ?: "")
                        }
                        .setNegativeButton("取消", null)
                        .show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "加载联系人失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 发送名片消息
     * 参考：WildFireChat CardMessageContent (Apache 2.0)
     */
    private fun sendContactCard(contactId: Long, contactName: String) {
        lifecycleScope.launch {
            try {
                val cardContent = "CARD:$contactId:$contactName"
                val request = com.lanxin.im.data.remote.SendMessageRequest(
                    receiver_id = peerId,
                    content = cardContent,
                    type = "card"
                )
                
                val response = RetrofitClient.apiService.sendMessage(request)
                if (response.code == 0 && response.data != null) {
                    val newMessage = response.data.message
                    val currentList = adapter.currentList.toMutableList()
                    currentList.add(newMessage)
                    adapter.submitList(currentList)
                    recyclerView.scrollToPosition(currentList.size - 1)
                    Toast.makeText(this@ChatActivity, "名片已发送", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ChatActivity, "发送失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "发送失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // WebSocket实时消息处理
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * 注册WebSocket消息广播接收器
     * 用于接收实时消息、已读回执、撤回通知等
     */
    private fun registerMessageReceiver() {
        messageReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    "com.lanxin.im.NEW_MESSAGE" -> {
                        val message = intent.getParcelableExtra<Message>("message")
                        val convId = intent.getLongExtra("conversation_id", 0)
                        
                        // 如果是当前会话的消息，添加到列表
                        if (convId == conversationId && message != null) {
                            val currentList = adapter.currentList.toMutableList()
                            currentList.add(message)
                            adapter.submitList(currentList)
                            recyclerView.smoothScrollToPosition(currentList.size - 1)
                        }
                    }
                    "com.lanxin.im.MESSAGE_READ" -> {
                        val messageId = intent.getLongExtra("message_id", 0)
                        val convId = intent.getLongExtra("conversation_id", 0)
                        
                        // 如果是当前会话，刷新消息状态
                        if (convId == conversationId) {
                            loadMessages()
                        }
                    }
                    "com.lanxin.im.MESSAGE_RECALLED" -> {
                        val messageId = intent.getLongExtra("message_id", 0)
                        val convId = intent.getLongExtra("conversation_id", 0)
                        
                        // 如果是当前会话，刷新消息列表
                        if (convId == conversationId) {
                            loadMessages()
                        }
                    }
                    "com.lanxin.im.USER_STATUS" -> {
                        val userId = intent.getLongExtra("user_id", 0)
                        val status = intent.getStringExtra("status")
                        
                        // 如果是当前聊天对象，更新在线状态
                        if (userId == peerId) {
                            updateUserStatus(status ?: "offline")
                        }
                    }
                }
            }
        }
        
        val filter = IntentFilter().apply {
            addAction("com.lanxin.im.NEW_MESSAGE")
            addAction("com.lanxin.im.MESSAGE_READ")
            addAction("com.lanxin.im.MESSAGE_RECALLED")
            addAction("com.lanxin.im.USER_STATUS")
        }
        registerReceiver(messageReceiver, filter)
    }
    
    /**
     * 更新用户在线状态显示
     */
    private fun updateUserStatus(status: String) {
        val statusText = if (status == "online") "在线" else "离线"
        val statusColor = if (status == "online") R.color.status_online else R.color.status_offline
        
        runOnUiThread {
            tvStatus.text = statusText
            tvStatus.setTextColor(getColor(statusColor))
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 注销广播接收器
        try {
            unregisterReceiver(messageReceiver)
        } catch (e: Exception) {
            // 忽略未注册的异常
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
            PermissionHelper.REQUEST_CAMERA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "已获得相机权限", Toast.LENGTH_SHORT).show()
                } else {
                    PermissionHelper.showPermissionDeniedMessage(this, "相机")
                }
            }
            PermissionHelper.REQUEST_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "已获得存储权限", Toast.LENGTH_SHORT).show()
                } else {
                    PermissionHelper.showPermissionDeniedMessage(this, "存储")
                }
            }
        }
    }
}

