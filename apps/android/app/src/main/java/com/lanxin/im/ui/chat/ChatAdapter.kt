package com.lanxin.im.ui.chat

import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lanxin.im.R
import com.lanxin.im.data.model.Message
import java.text.SimpleDateFormat
import java.util.*

/**
 * 聊天消息适配器
 * 支持文本、语音、图片、视频和文件消息类型
 */
class ChatAdapter(
    private val currentUserId: Long,
    private val onMessageLongClick: (Message) -> Unit,
    private val onVoiceClick: ((Message) -> Unit)? = null,
    private val onImageClick: ((Message) -> Unit)? = null,
    private val onVideoClick: ((Message) -> Unit)? = null,
    private val onFileClick: ((Message) -> Unit)? = null
) : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {
    
    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
        private const val VIEW_TYPE_VOICE_SENT = 3
        private const val VIEW_TYPE_VOICE_RECEIVED = 4
        private const val VIEW_TYPE_IMAGE_SENT = 5
        private const val VIEW_TYPE_IMAGE_RECEIVED = 6
        private const val VIEW_TYPE_VIDEO_SENT = 7
        private const val VIEW_TYPE_VIDEO_RECEIVED = 8
        private const val VIEW_TYPE_FILE_SENT = 9
        private const val VIEW_TYPE_FILE_RECEIVED = 10
    }
    
    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        val isSent = message.senderId == currentUserId
        
        return when (message.type) {
            "voice" -> if (isSent) VIEW_TYPE_VOICE_SENT else VIEW_TYPE_VOICE_RECEIVED
            "image" -> if (isSent) VIEW_TYPE_IMAGE_SENT else VIEW_TYPE_IMAGE_RECEIVED
            "video" -> if (isSent) VIEW_TYPE_VIDEO_SENT else VIEW_TYPE_VIDEO_RECEIVED
            "file" -> if (isSent) VIEW_TYPE_FILE_SENT else VIEW_TYPE_FILE_RECEIVED
            else -> if (isSent) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
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
            VIEW_TYPE_VOICE_SENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_voice_sent, parent, false)
                VoiceSentViewHolder(view)
            }
            VIEW_TYPE_VOICE_RECEIVED -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_voice_sent, parent, false)
                VoiceReceivedViewHolder(view)
            }
            VIEW_TYPE_IMAGE_SENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_image_sent, parent, false)
                ImageSentViewHolder(view)
            }
            VIEW_TYPE_IMAGE_RECEIVED -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_image_sent, parent, false)
                ImageReceivedViewHolder(view)
            }
            VIEW_TYPE_VIDEO_SENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_video_sent, parent, false)
                VideoSentViewHolder(view)
            }
            VIEW_TYPE_VIDEO_RECEIVED -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_video_sent, parent, false)
                VideoReceivedViewHolder(view)
            }
            VIEW_TYPE_FILE_SENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_file_sent, parent, false)
                FileSentViewHolder(view)
            }
            VIEW_TYPE_FILE_RECEIVED -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_file_sent, parent, false)
                FileReceivedViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message, onMessageLongClick)
            is ReceivedMessageViewHolder -> holder.bind(message, onMessageLongClick)
            is VoiceSentViewHolder -> holder.bind(message, onMessageLongClick, onVoiceClick)
            is VoiceReceivedViewHolder -> holder.bind(message, onMessageLongClick, onVoiceClick)
            is ImageSentViewHolder -> holder.bind(message, onMessageLongClick, onImageClick)
            is ImageReceivedViewHolder -> holder.bind(message, onMessageLongClick, onImageClick)
            is VideoSentViewHolder -> holder.bind(message, onMessageLongClick, onVideoClick)
            is VideoReceivedViewHolder -> holder.bind(message, onMessageLongClick, onVideoClick)
            is FileSentViewHolder -> holder.bind(message, onMessageLongClick, onFileClick)
            is FileReceivedViewHolder -> holder.bind(message, onMessageLongClick, onFileClick)
        }
    }
    
    // 发送消息ViewHolder
    class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvContent: TextView = itemView.findViewById(R.id.tv_content)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        private val ivAvatar: ImageView = itemView.findViewById(R.id.iv_avatar)
        private val messageBubble: View = itemView.findViewById(R.id.message_bubble)
        
        fun bind(message: Message, onLongClick: (Message) -> Unit) {
            // 根据消息状态显示内容
            if (message.status == "recalled") {
                tvContent.text = "你撤回了一条消息"
                tvContent.alpha = 0.6f
            } else {
                val content = message.content.split("|MENTIONS:")[0]
                
                if (message.type == "burn") {
                    val burnMessage = SpannableString("🔥 $content [阅后即焚]")
                    burnMessage.setSpan(
                        ForegroundColorSpan(ContextCompat.getColor(itemView.context, R.color.error)),
                        content.length + 3,
                        burnMessage.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    tvContent.text = burnMessage
                } else if (message.content.contains("|MENTIONS:")) {
                    tvContent.text = highlightMentions(content)
                } else {
                    tvContent.text = content
                }
                tvContent.alpha = 1.0f
            }
            
            // 时间格式化
            val time = Date(message.createdAt)
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            tvTime.text = timeFormat.format(time)
            
            // 使用Glide加载头像
            Glide.with(itemView.context)
                .load(R.drawable.ic_profile)
                .circleCrop()
                .into(ivAvatar)
            
            // 长按消息气泡弹出菜单（按设计文档要求）
            messageBubble.setOnLongClickListener {
                if (message.status != "recalled") {
                    onLongClick(message)
                }
                true
            }
        }
        
        private fun highlightMentions(content: String): SpannableString {
            val spannable = SpannableString(content)
            val regex = "@\\S+".toRegex()
            regex.findAll(content).forEach { match ->
                spannable.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(itemView.context, R.color.primary)),
                    match.range.first,
                    match.range.last + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            return spannable
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
                val content = message.content.split("|MENTIONS:")[0]
                
                if (message.type == "burn") {
                    val burnMessage = SpannableString("🔥 $content [阅后即焚]")
                    burnMessage.setSpan(
                        ForegroundColorSpan(ContextCompat.getColor(itemView.context, R.color.error)),
                        content.length + 3,
                        burnMessage.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    tvContent.text = burnMessage
                } else if (message.content.contains("|MENTIONS:")) {
                    tvContent.text = highlightMentions(content)
                } else {
                    tvContent.text = content
                }
                tvContent.alpha = 1.0f
            }
            
            // 时间格式化
            val time = Date(message.createdAt)
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            tvTime.text = timeFormat.format(time)
            
            // 使用Glide加载头像
            Glide.with(itemView.context)
                .load(R.drawable.ic_profile)
                .circleCrop()
                .into(ivAvatar)
            
            // 长按事件
            itemView.setOnLongClickListener {
                onLongClick(message)
                true
            }
        }
        
        private fun highlightMentions(content: String): SpannableString {
            val spannable = SpannableString(content)
            val regex = "@\\S+".toRegex()
            regex.findAll(content).forEach { match ->
                spannable.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(itemView.context, R.color.primary)),
                    match.range.first,
                    match.range.last + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            return spannable
        }
    }
    
    // 发送语音消息ViewHolder
    class VoiceSentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDuration: TextView = itemView.findViewById(R.id.tv_duration)
        private val ivAvatar: ImageView = itemView.findViewById(R.id.iv_avatar)
        private val ivVoiceIcon: ImageView = itemView.findViewById(R.id.iv_voice_icon)
        private val voiceBubble: View = itemView.findViewById(R.id.voice_bubble)
        
        fun bind(
            message: Message,
            onLongClick: (Message) -> Unit,
            onVoiceClick: ((Message) -> Unit)?
        ) {
            val duration = message.content.toIntOrNull() ?: 0
            tvDuration.text = "${duration}''"
            
            Glide.with(itemView.context)
                .load(R.drawable.ic_profile)
                .circleCrop()
                .into(ivAvatar)
            
            voiceBubble.setOnClickListener {
                onVoiceClick?.invoke(message)
            }
            
            voiceBubble.setOnLongClickListener {
                onLongClick(message)
                true
            }
        }
    }
    
    // 接收语音消息ViewHolder
    class VoiceReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDuration: TextView = itemView.findViewById(R.id.tv_duration)
        private val ivAvatar: ImageView = itemView.findViewById(R.id.iv_avatar)
        private val ivVoiceIcon: ImageView = itemView.findViewById(R.id.iv_voice_icon)
        private val voiceBubble: View = itemView.findViewById(R.id.voice_bubble)
        
        fun bind(
            message: Message,
            onLongClick: (Message) -> Unit,
            onVoiceClick: ((Message) -> Unit)?
        ) {
            val duration = message.content.toIntOrNull() ?: 0
            tvDuration.text = "${duration}''"
            
            Glide.with(itemView.context)
                .load(R.drawable.ic_profile)
                .circleCrop()
                .into(ivAvatar)
            
            voiceBubble.setOnClickListener {
                onVoiceClick?.invoke(message)
            }
            
            voiceBubble.setOnLongClickListener {
                onLongClick(message)
                true
            }
        }
    }
    
    // 发送图片消息ViewHolder
    class ImageSentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAvatar: ImageView = itemView.findViewById(R.id.iv_avatar)
        private val ivImage: ImageView = itemView.findViewById(R.id.iv_image)
        private val imageCard: View = itemView.findViewById(R.id.image_card)
        
        fun bind(
            message: Message,
            onLongClick: (Message) -> Unit,
            onImageClick: ((Message) -> Unit)?
        ) {
            Glide.with(itemView.context)
                .load(R.drawable.ic_profile)
                .circleCrop()
                .into(ivAvatar)
            
            Glide.with(itemView.context)
                .load(message.content)
                .centerCrop()
                .into(ivImage)
            
            imageCard.setOnClickListener {
                onImageClick?.invoke(message)
            }
            
            imageCard.setOnLongClickListener {
                onLongClick(message)
                true
            }
        }
    }
    
    // 接收图片消息ViewHolder
    class ImageReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAvatar: ImageView = itemView.findViewById(R.id.iv_avatar)
        private val ivImage: ImageView = itemView.findViewById(R.id.iv_image)
        private val imageCard: View = itemView.findViewById(R.id.image_card)
        
        fun bind(
            message: Message,
            onLongClick: (Message) -> Unit,
            onImageClick: ((Message) -> Unit)?
        ) {
            Glide.with(itemView.context)
                .load(R.drawable.ic_profile)
                .circleCrop()
                .into(ivAvatar)
            
            Glide.with(itemView.context)
                .load(message.content)
                .centerCrop()
                .into(ivImage)
            
            imageCard.setOnClickListener {
                onImageClick?.invoke(message)
            }
            
            imageCard.setOnLongClickListener {
                onLongClick(message)
                true
            }
        }
    }
    
    // 发送视频消息ViewHolder
    class VideoSentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAvatar: ImageView = itemView.findViewById(R.id.iv_avatar)
        private val ivThumbnail: ImageView = itemView.findViewById(R.id.iv_thumbnail)
        private val videoCard: View = itemView.findViewById(R.id.video_card)
        
        fun bind(
            message: Message,
            onLongClick: (Message) -> Unit,
            onVideoClick: ((Message) -> Unit)?
        ) {
            Glide.with(itemView.context)
                .load(R.drawable.ic_profile)
                .circleCrop()
                .into(ivAvatar)
            
            Glide.with(itemView.context)
                .load(message.content)
                .centerCrop()
                .into(ivThumbnail)
            
            videoCard.setOnClickListener {
                onVideoClick?.invoke(message)
            }
            
            videoCard.setOnLongClickListener {
                onLongClick(message)
                true
            }
        }
    }
    
    // 接收视频消息ViewHolder
    class VideoReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAvatar: ImageView = itemView.findViewById(R.id.iv_avatar)
        private val ivThumbnail: ImageView = itemView.findViewById(R.id.iv_thumbnail)
        private val videoCard: View = itemView.findViewById(R.id.video_card)
        
        fun bind(
            message: Message,
            onLongClick: (Message) -> Unit,
            onVideoClick: ((Message) -> Unit)?
        ) {
            Glide.with(itemView.context)
                .load(R.drawable.ic_profile)
                .circleCrop()
                .into(ivAvatar)
            
            Glide.with(itemView.context)
                .load(message.content)
                .centerCrop()
                .into(ivThumbnail)
            
            videoCard.setOnClickListener {
                onVideoClick?.invoke(message)
            }
            
            videoCard.setOnLongClickListener {
                onLongClick(message)
                true
            }
        }
    }
    
    // 发送文件消息ViewHolder
    class FileSentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAvatar: ImageView = itemView.findViewById(R.id.iv_avatar)
        private val tvFileName: TextView = itemView.findViewById(R.id.tv_file_name)
        private val tvFileSize: TextView = itemView.findViewById(R.id.tv_file_size)
        private val fileCard: View = itemView.findViewById(R.id.file_card)
        
        fun bind(
            message: Message,
            onLongClick: (Message) -> Unit,
            onFileClick: ((Message) -> Unit)?
        ) {
            Glide.with(itemView.context)
                .load(R.drawable.ic_profile)
                .circleCrop()
                .into(ivAvatar)
            
            val parts = message.content.split("|")
            if (parts.size >= 2) {
                tvFileName.text = parts[0]
                val size = parts[1].toLongOrNull() ?: 0L
                tvFileSize.text = formatFileSize(size)
            } else {
                tvFileName.text = "未知文件"
                tvFileSize.text = "0 B"
            }
            
            fileCard.setOnClickListener {
                onFileClick?.invoke(message)
            }
            
            fileCard.setOnLongClickListener {
                onLongClick(message)
                true
            }
        }
        
        private fun formatFileSize(size: Long): String {
            return when {
                size < 1024 -> "$size B"
                size < 1024 * 1024 -> String.format("%.1f KB", size / 1024.0)
                size < 1024 * 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024))
                else -> String.format("%.1f GB", size / (1024.0 * 1024 * 1024))
            }
        }
    }
    
    // 接收文件消息ViewHolder
    class FileReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAvatar: ImageView = itemView.findViewById(R.id.iv_avatar)
        private val tvFileName: TextView = itemView.findViewById(R.id.tv_file_name)
        private val tvFileSize: TextView = itemView.findViewById(R.id.tv_file_size)
        private val fileCard: View = itemView.findViewById(R.id.file_card)
        
        fun bind(
            message: Message,
            onLongClick: (Message) -> Unit,
            onFileClick: ((Message) -> Unit)?
        ) {
            Glide.with(itemView.context)
                .load(R.drawable.ic_profile)
                .circleCrop()
                .into(ivAvatar)
            
            val parts = message.content.split("|")
            if (parts.size >= 2) {
                tvFileName.text = parts[0]
                val size = parts[1].toLongOrNull() ?: 0L
                tvFileSize.text = formatFileSize(size)
            } else {
                tvFileName.text = "未知文件"
                tvFileSize.text = "0 B"
            }
            
            fileCard.setOnClickListener {
                onFileClick?.invoke(message)
            }
            
            fileCard.setOnLongClickListener {
                onLongClick(message)
                true
            }
        }
        
        private fun formatFileSize(size: Long): String {
            return when {
                size < 1024 -> "$size B"
                size < 1024 * 1024 -> String.format("%.1f KB", size / 1024.0)
                size < 1024 * 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024))
                else -> String.format("%.1f GB", size / (1024.0 * 1024 * 1024))
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

