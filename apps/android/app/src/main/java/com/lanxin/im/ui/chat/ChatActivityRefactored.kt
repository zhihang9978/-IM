package com.lanxin.im.ui.chat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.lanxin.im.R
import com.lanxin.im.data.model.Message
import com.lanxin.im.ui.chat.manager.ChatInputManager
import com.lanxin.im.ui.chat.manager.ChatMediaHandler
import com.lanxin.im.ui.chat.manager.ChatMessageHandler
import com.lanxin.im.ui.chat.manager.ChatUIManager
import com.lanxin.im.utils.AnalyticsHelper

/**
 * 重构版聊天Activity
 * 使用Manager模式，将1771行代码重构为清晰的模块化结构
 * 
 * 运营级别标准：
 * - 单一职责原则
 * - 易于测试和维护
 * - 性能优化
 * - 错误处理完善
 */
class ChatActivityRefactored : AppCompatActivity() {
    
    // Basic info
    private var conversationId: Long = 0
    private var peerId: Long = 0
    private var currentUserId: Long = 0
    
    // View references
    private lateinit var btnBack: ImageButton
    private lateinit var btnVoiceCall: ImageButton
    private lateinit var btnVideoCall: ImageButton
    private lateinit var tvTitle: TextView
    private lateinit var tvStatus: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChatAdapter
    
    // Managers (Delegation Pattern)
    private lateinit var inputManager: ChatInputManager
    private lateinit var mediaHandler: ChatMediaHandler
    private lateinit var messageHandler: ChatMessageHandler
    private lateinit var uiManager: ChatUIManager
    
    // WebSocket message receiver
    private lateinit var messageReceiver: BroadcastReceiver
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_wildfire)
        
        // Get parameters
        conversationId = intent.getLongExtra("conversation_id", 0)
        peerId = intent.getLongExtra("peer_id", 0)
        currentUserId = intent.getLongExtra("current_user_id", 1L)
        
        // Analytics
        AnalyticsHelper.trackFeatureUsage(this, "chat_open")
        
        // Initialize UI
        setupViews()
        setupAdapter()
        initializeManagers()
        setupTopBar()
        registerMessageReceiver()
        
        // Load initial data
        uiManager.loadMessages()
    }
    
    private fun setupViews() {
        btnBack = findViewById(R.id.btn_back)
        btnVoiceCall = findViewById(R.id.btn_voice_call)
        btnVideoCall = findViewById(R.id.btn_video_call)
        tvTitle = findViewById(R.id.tv_title)
        tvStatus = findViewById(R.id.tv_status)
        recyclerView = findViewById(R.id.recycler_view_messages)
    }
    
    private fun setupAdapter() {
        adapter = ChatAdapter(
            currentUserId = currentUserId,
            onMessageLongClick = { message ->
                // Delegate to message handler
                messageHandler.showMessageMenu(message, recyclerView)
            },
            onVoiceClick = { message ->
                mediaHandler.playVoiceMessage(message)
            },
            onImageClick = { message ->
                mediaHandler.previewImage(message)
            },
            onVideoClick = { message ->
                mediaHandler.playVideo(message)
            },
            onFileClick = { message ->
                mediaHandler.openFile(message)
            },
            onBurnMessageDelete = { message ->
                messageHandler.deleteBurnMessage(message)
            }
        )
        recyclerView.adapter = adapter
    }
    
    private fun initializeManagers() {
        // Initialize UI Manager
        uiManager = ChatUIManager(this, adapter, conversationId)
        uiManager.initialize()
        
        // Initialize Message Handler
        messageHandler = ChatMessageHandler(
            activity = this,
            adapter = adapter,
            conversationId = conversationId,
            currentUserId = currentUserId,
            onQuoteMessage = { message ->
                inputManager.quoteMessage(message)
            },
            onLoadMessages = {
                uiManager.loadMessages()
            }
        )
        
        // Initialize Media Handler
        mediaHandler = ChatMediaHandler(
            activity = this,
            onImageSelected = { imagePath ->
                messageHandler.sendImageMessage(imagePath)
            },
            onVideoSelected = { videoPath ->
                messageHandler.sendVideoMessage(videoPath)
            },
            onFileSelected = { filePath, fileName, fileSize ->
                messageHandler.sendFileMessage(filePath, fileName, fileSize)
            }
        )
        
        // Initialize Input Manager
        inputManager = ChatInputManager(
            activity = this,
            onSendMessage = { content ->
                val quotedMessage = inputManager.getQuotedMessage()
                val mentionedUsers = inputManager.getMentionedUsers()
                messageHandler.sendTextMessage(content, quotedMessage, mentionedUsers)
            },
            onSendVoice = { filePath, duration ->
                messageHandler.sendVoiceMessage(filePath, duration)
            },
            onMentionTriggered = {
                showMemberSelector()
            }
        )
        inputManager.initialize()
        
        // Setup extension panel buttons
        setupExtensionPanel()
    }
    
    private fun setupTopBar() {
        // Set title
        val peerName = intent.getStringExtra("peer_name") ?: "聊天"
        uiManager.setTitle(peerName)
        
        // Back button
        btnBack.setOnClickListener {
            finish()
        }
        
        // Voice call
        btnVoiceCall.setOnClickListener {
            val intent = Intent(this, com.lanxin.im.trtc.AudioCallActivity::class.java)
            intent.putExtra("peer_id", peerId)
            intent.putExtra("peer_name", tvTitle.text.toString())
            startActivity(intent)
        }
        
        // Video call
        btnVideoCall.setOnClickListener {
            val intent = Intent(this, com.lanxin.im.trtc.VideoCallActivity::class.java)
            intent.putExtra("peer_id", peerId)
            intent.putExtra("peer_name", tvTitle.text.toString())
            startActivity(intent)
        }
    }
    
    private fun setupExtensionPanel() {
        val extContainer = inputManager.getExtensionContainer()
        
        // Album
        extContainer.findViewById<android.view.View>(R.id.btn_album)?.setOnClickListener {
            mediaHandler.selectImageFromAlbum()
        }
        
        // Camera
        extContainer.findViewById<android.view.View>(R.id.btn_camera)?.setOnClickListener {
            mediaHandler.takePicture()
        }
        
        // File
        extContainer.findViewById<android.view.View>(R.id.btn_file)?.setOnClickListener {
            mediaHandler.selectFile()
        }
        
        // Video call
        extContainer.findViewById<android.view.View>(R.id.btn_video_call_ext)?.setOnClickListener {
            val intent = Intent(this, com.lanxin.im.trtc.VideoCallActivity::class.java)
            intent.putExtra("peer_id", peerId)
            intent.putExtra("peer_name", tvTitle.text.toString())
            startActivity(intent)
        }
        
        // Voice call
        extContainer.findViewById<android.view.View>(R.id.btn_voice_call_ext)?.setOnClickListener {
            val intent = Intent(this, com.lanxin.im.trtc.AudioCallActivity::class.java)
            intent.putExtra("peer_id", peerId)
            intent.putExtra("peer_name", tvTitle.text.toString())
            startActivity(intent)
        }
        
        // User card
        extContainer.findViewById<android.view.View>(R.id.btn_user_card)?.setOnClickListener {
            // TODO: Open contact card selector
        }
    }
    
    private fun showMemberSelector() {
        // TODO: Show member selector for @ mention
        // For now, just insert a placeholder
        inputManager.insertMention("测试用户", 999L)
    }
    
    private fun registerMessageReceiver() {
        messageReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    "com.lanxin.im.NEW_MESSAGE" -> {
                        val message = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra("message", Message::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra("message")
                        }
                        
                        message?.let {
                            if (it.conversationId == conversationId) {
                                // Check for duplicates
                                if (!messageHandler.isDuplicateMessage(it.id)) {
                                    uiManager.addNewMessage(it)
                                }
                            }
                        }
                    }
                    "com.lanxin.im.MESSAGE_RECALLED" -> {
                        uiManager.loadMessages()
                    }
                    "com.lanxin.im.USER_STATUS" -> {
                        val userId = intent.getLongExtra("user_id", 0)
                        val status = intent.getStringExtra("status") ?: ""
                        if (userId == peerId) {
                            uiManager.showOnlineStatus(status == "online")
                        }
                    }
                }
            }
        }
        
        val filter = IntentFilter().apply {
            addAction("com.lanxin.im.NEW_MESSAGE")
            addAction("com.lanxin.im.MESSAGE_RECALLED")
            addAction("com.lanxin.im.USER_STATUS")
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(messageReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(messageReceiver, filter)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Cleanup managers
        inputManager.cleanup()
        mediaHandler.cleanup()
        messageHandler.clearDuplicateCache()
        
        // Unregister receiver
        try {
            unregisterReceiver(messageReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
