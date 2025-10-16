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
import com.lanxin.im.data.model.Message
import java.text.SimpleDateFormat
import java.util.*

/**
 * 聊天消息适配器
 * 支持发送和接收两种消息类型
 */
class ChatAdapter(
    private val currentUserId: Long,
    private val onMessageLongClick: (Message) -> Unit
) : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {
    
    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }
    
    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return if (message.senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_sent, parent, false)
                SentMessageViewHolder(view)
            }
            VIEW_TYPE_RECEIVED -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_received, parent, false)
                ReceivedMessageViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message, onMessageLongClick)
            is ReceivedMessageViewHolder -> holder.bind(message, onMessageLongClick)
        }
    }
    
    // 发送消息ViewHolder
    class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvContent: TextView = itemView.findViewById(R.id.tv_content)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        private val ivAvatar: ImageView = itemView.findViewById(R.id.iv_avatar)
        
        fun bind(message: Message, onLongClick: (Message) -> Unit) {
            // 根据消息状态显示内容
            if (message.status == "recalled") {
                tvContent.text = "你撤回了一条消息"
                tvContent.alpha = 0.6f
            } else {
                tvContent.text = message.content
                tvContent.alpha = 1.0f
            }
            
            // 时间格式化
            val time = Date(message.createdAt)
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            tvTime.text = timeFormat.format(time)
            
            // 长按事件
            itemView.setOnLongClickListener {
                onLongClick(message)
                true
            }
        }
    }
    
    // 接收消息ViewHolder
    class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvContent: TextView = itemView.findViewById(R.id.tv_content)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        private val ivAvatar: ImageView = itemView.findViewById(R.id.iv_avatar)
        
        fun bind(message: Message, onLongClick: (Message) -> Unit) {
            // 根据消息状态显示内容
            if (message.status == "recalled") {
                tvContent.text = "对方撤回了一条消息"
                tvContent.alpha = 0.6f
            } else {
                tvContent.text = message.content
                tvContent.alpha = 1.0f
            }
            
            // 时间格式化
            val time = Date(message.createdAt)
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            tvTime.text = timeFormat.format(time)
            
            // 长按事件
            itemView.setOnLongClickListener {
                onLongClick(message)
                true
            }
        }
    }
}

// DiffUtil回调
class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
    override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem.id == newItem.id
    }
    
    override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem == newItem
    }
}

