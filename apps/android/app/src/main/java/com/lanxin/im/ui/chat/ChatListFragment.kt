package com.lanxin.im.ui.chat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lanxin.im.R
import com.lanxin.im.data.model.Conversation
import com.lanxin.im.data.remote.RetrofitClient
import kotlinx.coroutines.launch

/**
 * 聊天列表Fragment - 对应"蓝信"底部导航
 * 显示所有会话列表（按设计文档实现）
 */
class ChatListFragment : Fragment() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ConversationAdapter
    private lateinit var messageReceiver: BroadcastReceiver
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat_list, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        loadConversations()
        registerMessageReceiver()
    }
    
    private fun setupRecyclerView() {
        recyclerView = view?.findViewById(R.id.recycler_view) ?: return
        
        adapter = ConversationAdapter { conversation ->
            // 点击会话，进入聊天界面
            val intent = Intent(requireContext(), ChatActivity::class.java)
            intent.putExtra("conversation_id", conversation.id)
            startActivity(intent)
        }
        
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }
    
    private fun loadConversations() {
        lifecycleScope.launch {
            try {
                // 调用API获取会话列表
                val response = RetrofitClient.apiService.getConversations()
                if (response.code == 0 && response.data != null) {
                    // 转换为ConversationDisplayItem (WildFire IM style)
                    // 使用API返回的完整数据
                    val displayItems = response.data.conversations.map { item ->
                        val conversation = Conversation(
                            id = item.id,
                            type = item.type,
                            user1Id = null,
                            user2Id = null,
                            groupId = null,
                            lastMessageId = item.last_message?.id,
                            lastMessageAt = item.updated_at,
                            unreadCount = item.unread_count,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        
                        ConversationDisplayItem(
                            conversation = conversation,
                            avatar = item.user?.avatar,
                            name = item.user?.username ?: item.user?.displayName ?: "用户${item.id}",
                            lastMessageContent = item.last_message?.content,
                            lastMessageType = item.last_message?.type ?: "text",
                            draft = null, // 草稿从本地数据库获取
                            isMuted = false, // 免打扰从本地数据库获取
                            isTop = false // 置顶从本地数据库获取
                        )
                    }
                    
                    adapter.submitList(displayItems)
                }
            } catch (e: Exception) {
                // 加载失败，显示空列表
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 注册WebSocket消息广播接收器
     * 收到新消息时自动刷新会话列表
     */
    private fun registerMessageReceiver() {
        messageReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    "com.lanxin.im.NEW_MESSAGE" -> {
                        // 收到新消息，刷新会话列表
                        loadConversations()
                    }
                    "com.lanxin.im.MESSAGE_READ" -> {
                        // 消息已读，刷新会话列表（更新未读数）
                        loadConversations()
                    }
                    "com.lanxin.im.MESSAGE_RECALLED" -> {
                        // 消息撤回，刷新会话列表
                        loadConversations()
                    }
                }
            }
        }
        
        val filter = IntentFilter().apply {
            addAction("com.lanxin.im.NEW_MESSAGE")
            addAction("com.lanxin.im.MESSAGE_READ")
            addAction("com.lanxin.im.MESSAGE_RECALLED")
        }
        requireContext().registerReceiver(messageReceiver, filter)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // 注销广播接收器
        try {
            requireContext().unregisterReceiver(messageReceiver)
        } catch (e: Exception) {
            // 忽略未注册的异常
        }
    }
}

