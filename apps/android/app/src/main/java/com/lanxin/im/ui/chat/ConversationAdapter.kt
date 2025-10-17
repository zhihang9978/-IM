package com.lanxin.im.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lanxin.im.R
import com.lanxin.im.data.model.Conversation
import java.text.SimpleDateFormat
import java.util.*

/**
 * 会话列表适配器（按设计文档实现）
 */
class ConversationAdapter(
    private val onItemClick: (Conversation) -> Unit
) : ListAdapter<Conversation, ConversationAdapter.ViewHolder>(ConversationDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick)
    }
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAvatar: ImageView = itemView.findViewById(R.id.iv_avatar)
        private val tvName: TextView = itemView.findViewById(R.id.tv_name)
        private val tvLastMessage: TextView = itemView.findViewById(R.id.tv_last_message)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        private val tvUnreadCount: TextView = itemView.findViewById(R.id.tv_unread_count)
        
        fun bind(conversation: Conversation, onClick: (Conversation) -> Unit) {
            // 头像使用默认图标（Glide集成在后续优化）
            
            // 显示对方名称（临时显示ID，实际需要从User对象获取）
            tvName.text = "联系人${conversation.id}"
            
            // 显示最后消息（临时文字，实际需要从lastMessage获取）
            tvLastMessage.text = "最后一条消息"
            
            // 显示时间
            conversation.lastMessageAt?.let { timestamp ->
                val time = Date(timestamp)
                val format = SimpleDateFormat("HH:mm", Locale.getDefault())
                tvTime.text = format.format(time)
            }
            
            // 显示未读数量
            if (conversation.unreadCount > 0) {
                tvUnreadCount.visibility = View.VISIBLE
                tvUnreadCount.text = if (conversation.unreadCount > 99) {
                    "99+"
                } else {
                    conversation.unreadCount.toString()
                }
            } else {
                tvUnreadCount.visibility = View.GONE
            }
            
            // 点击事件
            itemView.setOnClickListener {
                onClick(conversation)
            }
        }
    }
}

class ConversationDiffCallback : DiffUtil.ItemCallback<Conversation>() {
    override fun areItemsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
        return oldItem.id == newItem.id
    }
    
    override fun areContentsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
        return oldItem == newItem
    }
}

