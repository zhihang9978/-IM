package com.lanxin.im.ui.chat

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
 * 会话列表Adapter (WildFire IM style)
 * 参考：WildFireChat ConversationListAdapter & ConversationViewHolder (Apache 2.0)
 * 适配：蓝信IM
 * 
 * 功能:
 * - 显示48dp头像
 * - 时间格式化（刚刚、X分钟前、HH:mm、昨天、星期X、MM-dd）
 * - 消息预览格式化（文本、语音、图片、视频、文件）
 * - 未读徽章（1-99+）
 * - 未读红点（仅红点）
 * - 草稿标识（红色）
 * - 免打扰图标
 * - 置顶会话背景色
 */
class ConversationAdapter(
    private val onConversationClick: (Conversation) -> Unit
) : ListAdapter<ConversationDisplayItem, ConversationAdapter.ViewHolder>(ConversationDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation_wildfire, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onConversationClick)
    }
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val portraitImageView: ImageView = itemView.findViewById(R.id.portraitImageView)
        private val conversationTitleTextView: TextView = itemView.findViewById(R.id.conversationTitleTextView)
        private val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)
        private val contentTextView: TextView = itemView.findViewById(R.id.contentTextView)
        private val unreadCountTextView: TextView = itemView.findViewById(R.id.unreadCountTextView)
        private val redDotView: View = itemView.findViewById(R.id.redDotView)
        private val muteImageView: ImageView = itemView.findViewById(R.id.muteImageView)
        
        fun bind(item: ConversationDisplayItem, onClick: (Conversation) -> Unit) {
            // 加载头像 (WildFire IM: 48dp圆形头像)
            Glide.with(itemView.context)
                .load(item.avatar)
                .circleCrop()
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .into(portraitImageView)
            
            // 设置名称
            conversationTitleTextView.text = item.name
            
            // 格式化时间 (WildFire IM style)
            timeTextView.text = formatTime(item.conversation.lastMessageAt ?: System.currentTimeMillis())
            
            // 格式化消息预览 (WildFire IM style)
            contentTextView.text = formatMessagePreview(item)
            
            // 未读徽章 (WildFire IM style)
            val unreadCount = item.conversation.unreadCount
            if (unreadCount > 0) {
                if (item.isMuted) {
                    // 免打扰：只显示红点
                    redDotView.visibility = View.VISIBLE
                    unreadCountTextView.visibility = View.GONE
                } else {
                    // 显示未读数字
                    redDotView.visibility = View.GONE
                    unreadCountTextView.visibility = View.VISIBLE
                    unreadCountTextView.text = when {
                        unreadCount > 99 -> "99+"
                        else -> unreadCount.toString()
                    }
                }
            } else {
                redDotView.visibility = View.GONE
                unreadCountTextView.visibility = View.GONE
            }
            
            // 免打扰图标 (WildFire IM style)
            muteImageView.visibility = if (item.isMuted) View.VISIBLE else View.GONE
            
            // ✅ 置顶背景 (WildFire IM style)
            if (item.isTop) {
                itemView.setBackgroundColor(itemView.context.getColor(R.color.background_top))
            } else {
                itemView.setBackgroundResource(R.drawable.selector_common_item_wf)
            }
            
            // 点击事件
            itemView.setOnClickListener { onClick(item.conversation) }
        }
        
        /**
         * 时间格式化
         * 参考：WildFireChat TimeUtils.getMsgFormatTime (Apache 2.0)
         */
        private fun formatTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            val cal = Calendar.getInstance()
            cal.timeInMillis = timestamp
            
            return when {
                diff < 60000 -> "刚刚"
                diff < 3600000 -> "${diff / 60000}分钟前"
                diff < 86400000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
                diff < 172800000 -> "昨天"
                diff < 604800000 -> {
                    val days = arrayOf("日", "一", "二", "三", "四", "五", "六")
                    "星期${days[cal.get(Calendar.DAY_OF_WEEK) - 1]}"
                }
                else -> SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(timestamp))
            }
        }
        
        /**
         * 消息预览格式化
         * 参考：WildFireChat Message.digest() (Apache 2.0)
         */
        private fun formatMessagePreview(item: ConversationDisplayItem): CharSequence {
            // 草稿优先 (WildFire IM style)
            if (!item.draft.isNullOrEmpty()) {
                val draftText = "[草稿] ${item.draft}"
                val spannable = SpannableString(draftText)
                spannable.setSpan(
                    ForegroundColorSpan(Color.RED),
                    0, 4,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                return spannable
            }
            
            // 根据消息类型格式化 (WildFire IM style)
            return when (item.lastMessageType) {
                "text" -> item.lastMessageContent ?: ""
                "voice" -> "[语音]"
                "image" -> "[图片]"
                "video" -> "[视频]"
                "file" -> {
                    // 显示文件名
                    val fileName = item.lastMessageContent?.split("|")?.firstOrNull() ?: ""
                    "[文件] $fileName"
                }
                "burn" -> "[阅后即焚]"
                else -> "[消息]"
            }
        }
    }
}

/**
 * 会话显示项
 * 包含UI显示所需的所有信息
 */
data class ConversationDisplayItem(
    val conversation: Conversation,
    val avatar: String?,
    val name: String,
    val lastMessageContent: String?,
    val lastMessageType: String?,
    val draft: String?,
    val isMuted: Boolean,
    val isTop: Boolean
)

/**
 * DiffUtil回调
 */
class ConversationDiffCallback : DiffUtil.ItemCallback<ConversationDisplayItem>() {
    override fun areItemsTheSame(oldItem: ConversationDisplayItem, newItem: ConversationDisplayItem) = 
        oldItem.conversation.id == newItem.conversation.id
    
    override fun areContentsTheSame(oldItem: ConversationDisplayItem, newItem: ConversationDisplayItem) = 
        oldItem == newItem
}
