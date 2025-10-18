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
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lanxin.im.R
import com.lanxin.im.viewmodel.ConversationListViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * 聊天列表Fragment - 使用MVVM架构
 * 显示所有会话列表（使用ViewModel管理状态）
 */
@AndroidEntryPoint
class ChatListFragment : Fragment() {
    
    private val viewModel: ConversationListViewModel by viewModels()
    
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
            // 从SharedPreferences获取当前用户ID
            val sharedPref = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val currentUserId = sharedPref.getLong("user_id", 0)
            intent.putExtra("current_user_id", currentUserId)
            // 如果是单聊，传递对方ID
            if (conversation.type == "single") {
                intent.putExtra("peer_id", conversation.user1Id ?: conversation.user2Id ?: 0)
            }
            startActivity(intent)
        }
        
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }
    
    private fun loadConversations() {
        // 使用ViewModel观察数据变化，自动更新UI
        viewModel.conversations.observe(viewLifecycleOwner) { conversations ->
            // 转换为ConversationDisplayItem
            val displayItems = conversations.map { conversation ->
                ConversationDisplayItem(
                    conversation = conversation,
                    avatar = conversation.avatar,
                    name = conversation.name ?: "用户${conversation.id}",
                    lastMessageContent = conversation.lastMessage,
                    lastMessageType = conversation.lastMessageType ?: "text",
                    draft = conversation.draft,
                    isMuted = conversation.isMuted,
                    isTop = conversation.isTop
                )
            }
            adapter.submitList(displayItems)
        }
        
        // 触发刷新
        viewModel.refreshConversations()
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
        // Android 13+ requires explicit export flag
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(messageReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            requireContext().registerReceiver(messageReceiver, filter)
        }
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

