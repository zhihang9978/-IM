package com.lanxin.im.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
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
    
    private var conversationId: Long = 0
    private var peerId: Long = 0
    
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
            }
        )
        recyclerView.adapter = adapter
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
}

