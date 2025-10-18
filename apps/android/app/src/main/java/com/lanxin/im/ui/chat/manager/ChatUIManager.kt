package com.lanxin.im.ui.chat.manager

import android.app.Activity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.lanxin.im.R
import com.lanxin.im.data.model.Message
import com.lanxin.im.data.remote.RetrofitClient
import com.lanxin.im.ui.chat.ChatAdapter
import kotlinx.coroutines.launch

/**
 * 聊天UI管理器
 * 负责管理UI状态（未读提示/刷新/滚动）
 * 
 * 参考：WildFireChat ConversationFragment (Apache 2.0)
 */
class ChatUIManager(
    private val activity: AppCompatActivity,
    private val adapter: ChatAdapter,
    private val conversationId: Long
) {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var unreadCountLinearLayout: LinearLayout
    private lateinit var unreadCountTextView: TextView
    private lateinit var unreadMentionCountTextView: TextView
    
    private var isLoadingHistory = false
    private var hasMoreHistory = true
    private var oldestMessageId: Long? = null
    
    fun initialize() {
        recyclerView = activity.findViewById(R.id.recycler_view_messages)
        swipeRefreshLayout = activity.findViewById(R.id.swipeRefreshLayout)
        unreadCountLinearLayout = activity.findViewById(R.id.unreadCountLinearLayout)
        unreadCountTextView = activity.findViewById(R.id.unreadCountTextView)
        unreadMentionCountTextView = activity.findViewById(R.id.unreadMentionCountTextView)
        
        setupRecyclerView()
        setupSwipeRefresh()
    }
    
    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(activity).apply {
            stackFromEnd = true
        }
        recyclerView.adapter = adapter
        
        // Scroll listener for unread indicator
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateUnreadIndicator()
            }
        })
    }
    
    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(R.color.primary)
        swipeRefreshLayout.setOnRefreshListener {
            loadHistoryMessages()
        }
    }
    
    fun loadMessages() {
        activity.lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getMessages(conversationId)
                
                if (response.code == 0 && response.data != null) {
                    val messages = response.data.messages
                    adapter.submitList(messages)
                    
                    if (messages.isNotEmpty()) {
                        oldestMessageId = messages.first().id
                        recyclerView.scrollToPosition(messages.size - 1)
                    }
                    
                    // Mark messages as read
                    markMessagesAsRead()
                }
            } catch (e: Exception) {
                Toast.makeText(activity, "加载消息失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    fun loadHistoryMessages() {
        if (isLoadingHistory || !hasMoreHistory) {
            swipeRefreshLayout.isRefreshing = false
            return
        }
        
        isLoadingHistory = true
        
        activity.lifecycleScope.launch {
            try {
                val beforeMessageId = oldestMessageId ?: 0L
                
                val response = RetrofitClient.apiService.getHistoryMessages(conversationId, beforeMessageId)
                
                if (response.code == 0 && response.data != null) {
                    val historyMessages = response.data.messages
                    
                    if (historyMessages.isEmpty()) {
                        hasMoreHistory = false
                        Toast.makeText(activity, "没有更多消息了", Toast.LENGTH_SHORT).show()
                    } else {
                        // Save scroll position
                        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                        val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
                        
                        // Prepend history messages
                        val currentList = adapter.currentList.toMutableList()
                        currentList.addAll(0, historyMessages)
                        adapter.submitList(currentList)
                        
                        // Restore scroll position
                        layoutManager.scrollToPositionWithOffset(
                            firstVisiblePosition + historyMessages.size,
                            0
                        )
                        
                        oldestMessageId = historyMessages.first().id
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(activity, "加载历史消息失败", Toast.LENGTH_SHORT).show()
            } finally {
                isLoadingHistory = false
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }
    
    fun updateUnreadIndicator() {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
        val totalItemCount = adapter.itemCount
        
        // Calculate unread count
        val unreadCount = totalItemCount - lastVisiblePosition - 1
        
        if (unreadCount > 0) {
            unreadCountLinearLayout.visibility = View.VISIBLE
            unreadCountTextView.text = if (unreadCount > 99) "99+" else unreadCount.toString()
            
            // Click to scroll to bottom
            unreadCountLinearLayout.setOnClickListener {
                scrollToBottom()
            }
        } else {
            unreadCountLinearLayout.visibility = View.GONE
        }
    }
    
    fun scrollToBottom(smooth: Boolean = true) {
        val itemCount = adapter.itemCount
        if (itemCount > 0) {
            if (smooth) {
                recyclerView.smoothScrollToPosition(itemCount - 1)
            } else {
                recyclerView.scrollToPosition(itemCount - 1)
            }
        }
    }
    
    fun addNewMessage(message: Message) {
        val currentList = adapter.currentList.toMutableList()
        currentList.add(message)
        adapter.submitList(currentList)
        scrollToBottom()
    }
    
    private fun markMessagesAsRead() {
        activity.lifecycleScope.launch {
            try {
                RetrofitClient.apiService.markAsRead(conversationId)
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }
    
    fun setTitle(title: String) {
        activity.findViewById<TextView>(R.id.tv_title)?.text = title
    }
    
    fun setStatus(status: String) {
        activity.findViewById<TextView>(R.id.tv_status)?.text = status
    }
    
    fun showOnlineStatus(isOnline: Boolean) {
        val statusText = if (isOnline) "在线" else "离线"
        setStatus(statusText)
    }
}
