package com.lanxin.im.ui.chat

import android.content.Intent
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
                    val displayItems = response.data.conversations.map { item ->
                        val conversation = Conversation(
                            id = item.id,
                            type = item.type,
                            user1Id = null,
                            user2Id = null,
                            groupId = null,
                            lastMessageId = null,
                            lastMessageAt = item.updated_at,
                            unreadCount = item.unread_count,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        
                        ConversationDisplayItem(
                            conversation = conversation,
                            avatar = null, // TODO: 从API或数据库获取
                            name = "用户${item.id}", // TODO: 从API或数据库获取
                            lastMessageContent = null, // TODO: 从API或数据库获取
                            lastMessageType = "text", // TODO: 从API或数据库获取
                            draft = null, // TODO: 从数据库获取
                            isMuted = false, // TODO: 从数据库获取
                            isTop = false // TODO: 从数据库获取
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
}

