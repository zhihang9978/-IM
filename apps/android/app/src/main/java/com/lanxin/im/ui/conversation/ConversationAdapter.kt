package com.lanxin.im.ui.conversation

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lanxin.im.R
import com.lanxin.im.data.model.Conversation
import java.text.SimpleDateFormat
import java.util.*

/**
 * 会话列表适配器 (WildFire IM style)
 * 参考：ConversationListAdapter.java (Apache 2.0)
 * 适配：蓝信IM
 */
class ConversationAdapter(
    private val onConversationClick: (Conversation) -> Unit,
    private val onConversationLongClick: ((Conversation) -> Unit)? = null
) : ListAdapter<Conversation, ConversationAdapter.ViewHolder>(ConversationDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation_wildfire, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onConversationClick, onConversationLongClick)
    }
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val portraitImageView: ImageView = itemView.findViewById(R.id.portraitImageView)
        private val conversationTitleTextView: TextView = itemView.findViewById(R.id.conversationTitleTextView)
        private val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)
        private val contentTextView: TextView = itemView.findViewById(R.id.contentTextView)
        private val unreadCountTextView: TextView = itemView.findViewById(R.id.unreadCountTextView)
        private val redDotView: View = itemView.findViewById(R.id.redDotView)
        private val muteImageView: ImageView = itemView.findViewById(R.id.muteImageView)
        
        fun bind(
            conversation: Conversation,
            onClick: (Conversation) -> Unit,
            onLongClick: ((Conversation) -> Unit)?
        ) {
            // 加载头像 (48dp - WildFire IM style)
            Glide.with(itemView.context)
                .load(conversation.avatar)
                .circleCrop()
                .placeholder(R.drawable.ic_profile)
                .into(portraitImageView)
            
            // 设置会话名称
            conversationTitleTextView.text = conversation.name
            
            // 格式化时间 (WildFire IM style)
            timeTextView.text = formatTime(conversation.lastMessageTime)
            
            // 格式化消息预览 (WildFire IM style)
            contentTextView.text = formatMessagePreview(conversation)
            
            // 未读徽章显示逻辑 (WildFire IM style)
            when {
                conversation.unreadCount == 0 -> {
                    // 无未读
                    redDotView.visibility = View.GONE
                    unreadCountTextView.visibility = View.GONE
                }
                conversation.isMuted -> {
                    // 免打扰：只显示红点
                    redDotView.visibility = View.VISIBLE
                    unreadCountTextView.visibility = View.GONE
                }
                else -> {
                    // 显示未读数字
                    redDotView.visibility = View.GONE
                    unreadCountTextView.visibility = View.VISIBLE
                    unreadCountTextView.text = when {
                        conversation.unreadCount > 99 -> "99+"
                        else -> conversation.unreadCount.toString()
                    }
                }
            }
            
            // 免打扰图标
            muteImageView.visibility = if (conversation.isMuted) View.VISIBLE else View.GONE
            
            // 置顶背景 (WildFire IM style)
            if (conversation.isTop) {
                itemView.setBackgroundColor(Color.parseColor("#F5F5F5"))
            } else {
                itemView.setBackgroundResource(R.drawable.selector_common_item_wf)
            }
            
            // 点击事件
            itemView.setOnClickListener { onClick(conversation) }
            
            // 长按事件
            onLongClick?.let { longClickHandler ->
                itemView.setOnLongClickListener {
                    longClickHandler(conversation)
                    true
                }
            }
        }
        
        /**
         * 格式化时间显示 (WildFire IM style)
         */
        private fun formatTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            val cal = Calendar.getInstance()
            cal.timeInMillis = timestamp
            
            return when {
                // 1分钟内
                diff < 60000 -> "刚刚"
                // 1小时内
                diff < 3600000 -> "${diff / 60000}分钟前"
                // 今天
                diff < 86400000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
                // 昨天
                diff < 172800000 -> "昨天"
                // 本周
                diff < 604800000 -> {
                    val days = arrayOf("日", "一", "二", "三", "四", "五", "六")
                    "星期${days[cal.get(Calendar.DAY_OF_WEEK) - 1]}"
                }
                // 更早
                else -> SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(timestamp))
            }
        }
        
        /**
         * 格式化消息预览 (WildFire IM style)
         */
        private fun formatMessagePreview(conversation: Conversation): CharSequence {
            // 草稿优先显示
            if (!conversation.draft.isNullOrEmpty()) {
                val draftText = "[草稿] ${conversation.draft}"
                val spannable = SpannableString(draftText)
                spannable.setSpan(
                    ForegroundColorSpan(Color.RED),
                    0, 4,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                return spannable
            }
            
            // 根据消息类型显示不同内容
            return when (conversation.lastMessageType) {
                "text" -> {
                    // 文本消息：显示前20字
                    val content = conversation.lastMessageContent
                    if (content.length > 20) content.substring(0, 20) + "..." else content
                }
                "voice" -> "[语音]"
                "image" -> "[图片]"
                "video" -> "[视频]"
                "file" -> {
                    // 文件消息：显示[文件] 文件名
                    val fileName = conversation.lastMessageContent.split("|").firstOrNull() ?: "文件"
                    "[文件] $fileName"
                }
                "location" -> "[位置]"
                "card" -> "[名片]"
                else -> "[消息]"
            }
        }
    }
}

/**
 * DiffUtil回调
 */
class ConversationDiffCallback : DiffUtil.ItemCallback<Conversation>() {
    override fun areItemsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
        return oldItem.id == newItem.id
    }
    
    override fun areContentsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
        return oldItem == newItem
    }
}

