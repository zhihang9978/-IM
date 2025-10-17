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
import com.lanxin.im.utils.BurnAfterReadHelper
import com.lanxin.im.utils.FileTypeHelper
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
    private val onFileClick: ((Message) -> Unit)? = null,
    private val onBurnMessageDelete: ((Message) -> Unit)? = null
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
                    .inflate(R.layout.item_message_sent_wildfire, parent, false)
                SentMessageViewHolder(view)
            }
            VIEW_TYPE_RECEIVED -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_received_wildfire, parent, false)
                ReceivedMessageViewHolder(view)
            }
            VIEW_TYPE_VOICE_SENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_voice_sent_wildfire, parent, false)
                VoiceSentViewHolder(view)
            }
            VIEW_TYPE_VOICE_RECEIVED -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_voice_received_wildfire, parent, false)
                VoiceReceivedViewHolder(view)
            }
            VIEW_TYPE_IMAGE_SENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_image_sent_wildfire, parent, false)
                ImageSentViewHolder(view)
            }
            VIEW_TYPE_IMAGE_RECEIVED -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_image_received_wildfire, parent, false)
                ImageReceivedViewHolder(view)
            }
            VIEW_TYPE_VIDEO_SENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_video_sent_wildfire, parent, false)
                VideoSentViewHolder(view)
            }
            VIEW_TYPE_VIDEO_RECEIVED -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_video_received_wildfire, parent, false)
                VideoReceivedViewHolder(view)
            }
            VIEW_TYPE_FILE_SENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_file_sent_wildfire, parent, false)
                FileSentViewHolder(view)
            }
            VIEW_TYPE_FILE_RECEIVED -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_file_received_wildfire, parent, false)
                FileReceivedViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message, onMessageLongClick, onBurnMessageDelete)
            is ReceivedMessageViewHolder -> holder.bind(message, onMessageLongClick, onBurnMessageDelete)
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
        
        fun bind(
            message: Message, 
            onLongClick: (Message) -> Unit,
            onBurnDelete: ((Message) -> Unit)? = null
        ) {
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
        
        fun bind(
            message: Message, 
            onLongClick: (Message) -> Unit,
            onBurnDelete: ((Message) -> Unit)? = null
        ) {
            // 根据消息状态显示内容
            if (message.status == "recalled") {
                tvContent.text = "对方撤回了一条消息"
                tvContent.alpha = 0.6f
            } else {
                val content = message.content.split("|MENTIONS:")[0]
                
                if (message.type == "burn") {
                    // 阅后即焚消息
                    if (!BurnAfterReadHelper.isCountingDown(message.id)) {
                        // 首次查看，启动倒计时
                        BurnAfterReadHelper.startBurnCountdown(
                            messageId = message.id,
                            onCountdown = { seconds ->
                                val burnText = "🔥 $content [${seconds}秒后销毁]"
                                val burnMessage = SpannableString(burnText)
                                burnMessage.setSpan(
                                    ForegroundColorSpan(ContextCompat.getColor(itemView.context, R.color.error)),
                                    content.length + 3,
                                    burnText.length,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                )
                                tvContent.post { tvContent.text = burnMessage }
                            },
                            onDelete = {
                                onBurnDelete?.invoke(message)
                            }
                        )
                    }
                    
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
    
    // 发送语音消息ViewHolder (WildFire IM style)
    class VoiceSentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val durationTextView: TextView = itemView.findViewById(R.id.durationTextView)
        private val audioImageView: ImageView = itemView.findViewById(R.id.audioImageView)
        private val audioContentLayout: View = itemView.findViewById(R.id.audioContentLayout)
        private val speechToTextLinearLayout: View = itemView.findViewById(R.id.speechToTextLinearLayout)
        private val speechToTextTextView: TextView = itemView.findViewById(R.id.speechToTextTextView)
        
        fun bind(
            message: Message,
            onLongClick: (Message) -> Unit,
            onVoiceClick: ((Message) -> Unit)?
        ) {
            val duration = message.content.toIntOrNull() ?: 0
            durationTextView.text = "${duration}''"
            
            // 点击播放语音
            audioContentLayout.setOnClickListener {
                onVoiceClick?.invoke(message)
                // TODO: 播放时启动动画
                // (audioImageView.background as? AnimationDrawable)?.start()
            }
            
            // 长按菜单
            audioContentLayout.setOnLongClickListener {
                onLongClick(message)
                true
            }
            
            // 语音转文字（如果有的话）
            // TODO: 实现语音转文字显示逻辑
            speechToTextLinearLayout.visibility = View.GONE
        }
    }
    
    // 接收语音消息ViewHolder (WildFire IM style)
    class VoiceReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val durationTextView: TextView = itemView.findViewById(R.id.durationTextView)
        private val audioImageView: ImageView = itemView.findViewById(R.id.audioImageView)
        private val audioContentLayout: View = itemView.findViewById(R.id.audioContentLayout)
        private val playStatusIndicator: View = itemView.findViewById(R.id.playStatusIndicator)
        private val speechToTextLinearLayout: View = itemView.findViewById(R.id.speechToTextLinearLayout)
        private val speechToTextTextView: TextView = itemView.findViewById(R.id.speechToTextTextView)
        
        fun bind(
            message: Message,
            onLongClick: (Message) -> Unit,
            onVoiceClick: ((Message) -> Unit)?
        ) {
            val duration = message.content.toIntOrNull() ?: 0
            durationTextView.text = "${duration}''"
            
            // 红点状态指示器（未播放显示）
            // TODO: 根据播放状态显示/隐藏红点
            playStatusIndicator.visibility = View.GONE
            
            // 点击播放语音
            audioContentLayout.setOnClickListener {
                onVoiceClick?.invoke(message)
                playStatusIndicator.visibility = View.GONE
                // TODO: 播放时启动动画
                // (audioImageView.background as? AnimationDrawable)?.start()
            }
            
            // 长按菜单
            audioContentLayout.setOnLongClickListener {
                onLongClick(message)
                true
            }
            
            // 语音转文字（如果有的话）
            // TODO: 实现语音转文字显示逻辑
            speechToTextLinearLayout.visibility = View.GONE
        }
    }
    
    // 发送图片消息ViewHolder (WildFire IM style)
    class ImageSentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageView)
        private val imageCardView: View = itemView.findViewById(R.id.imageCardView)
        
        fun bind(
            message: Message,
            onLongClick: (Message) -> Unit,
            onImageClick: ((Message) -> Unit)?
        ) {
            // 使用Glide加载图片，自适应尺寸
            Glide.with(itemView.context)
                .load(message.content)
                .centerCrop()
                .placeholder(R.drawable.ic_image)
                .error(R.drawable.ic_image)
                .into(imageView)
            
            // 点击预览图片
            imageCardView.setOnClickListener {
                onImageClick?.invoke(message)
            }
            
            // 长按菜单
            imageCardView.setOnLongClickListener {
                onLongClick(message)
                true
            }
        }
    }
    
    // 接收图片消息ViewHolder (WildFire IM style)
    class ImageReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageView)
        private val imageCardView: View = itemView.findViewById(R.id.imageCardView)
        
        fun bind(
            message: Message,
            onLongClick: (Message) -> Unit,
            onImageClick: ((Message) -> Unit)?
        ) {
            // 使用Glide加载图片，自适应尺寸
            Glide.with(itemView.context)
                .load(message.content)
                .centerCrop()
                .placeholder(R.drawable.ic_image)
                .error(R.drawable.ic_image)
                .into(imageView)
            
            // 点击预览图片
            imageCardView.setOnClickListener {
                onImageClick?.invoke(message)
            }
            
            // 长按菜单
            imageCardView.setOnLongClickListener {
                onLongClick(message)
                true
            }
        }
    }
    
    // 发送视频消息ViewHolder (WildFire IM style)
    class VideoSentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val videoImageView: ImageView = itemView.findViewById(R.id.videoImageView)
        private val playImageView: ImageView = itemView.findViewById(R.id.playImageView)
        private val durationTextView: TextView = itemView.findViewById(R.id.durationTextView)
        private val videoCardView: View = itemView.findViewById(R.id.videoCardView)
        private val loadingProgressBar: View = itemView.findViewById(R.id.loadingProgressBar)
        
        fun bind(
            message: Message,
            onLongClick: (Message) -> Unit,
            onVideoClick: ((Message) -> Unit)?
        ) {
            // 解析视频消息内容：格式为 "videoUrl|duration"
            val parts = message.content.split("|")
            val videoUrl = if (parts.isNotEmpty()) parts[0] else message.content
            val duration = if (parts.size >= 2) parts[1].toIntOrNull() ?: 0 else 0
            
            // 加载视频封面（使用Glide从视频URL获取第一帧）
            Glide.with(itemView.context)
                .load(videoUrl)
                .centerCrop()
                .placeholder(R.mipmap.img_video_default)
                .error(R.mipmap.img_video_default)
                .into(videoImageView)
            
            // 显示视频时长
            durationTextView.text = formatDuration(duration)
            
            // 点击播放视频
            videoCardView.setOnClickListener {
                onVideoClick?.invoke(message)
            }
            
            // 长按菜单
            videoCardView.setOnLongClickListener {
                onLongClick(message)
                true
            }
            
            // 隐藏加载进度条
            loadingProgressBar.visibility = View.GONE
        }
        
        private fun formatDuration(seconds: Int): String {
            val minutes = seconds / 60
            val secs = seconds % 60
            return String.format("%02d:%02d", minutes, secs)
        }
    }
    
    // 接收视频消息ViewHolder (WildFire IM style)
    class VideoReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val videoImageView: ImageView = itemView.findViewById(R.id.videoImageView)
        private val playImageView: ImageView = itemView.findViewById(R.id.playImageView)
        private val durationTextView: TextView = itemView.findViewById(R.id.durationTextView)
        private val videoCardView: View = itemView.findViewById(R.id.videoCardView)
        private val loadingProgressBar: View = itemView.findViewById(R.id.loadingProgressBar)
        
        fun bind(
            message: Message,
            onLongClick: (Message) -> Unit,
            onVideoClick: ((Message) -> Unit)?
        ) {
            // 解析视频消息内容：格式为 "videoUrl|duration"
            val parts = message.content.split("|")
            val videoUrl = if (parts.isNotEmpty()) parts[0] else message.content
            val duration = if (parts.size >= 2) parts[1].toIntOrNull() ?: 0 else 0
            
            // 加载视频封面（使用Glide从视频URL获取第一帧）
            Glide.with(itemView.context)
                .load(videoUrl)
                .centerCrop()
                .placeholder(R.mipmap.img_video_default)
                .error(R.mipmap.img_video_default)
                .into(videoImageView)
            
            // 显示视频时长
            durationTextView.text = formatDuration(duration)
            
            // 点击播放视频
            videoCardView.setOnClickListener {
                onVideoClick?.invoke(message)
            }
            
            // 长按菜单
            videoCardView.setOnLongClickListener {
                onLongClick(message)
                true
            }
            
            // 隐藏加载进度条
            loadingProgressBar.visibility = View.GONE
        }
        
        private fun formatDuration(seconds: Int): String {
            val minutes = seconds / 60
            val secs = seconds % 60
            return String.format("%02d:%02d", minutes, secs)
        }
    }
    
    // 发送文件消息ViewHolder (WildFire IM style)
    class FileSentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val fileIconImageView: ImageView = itemView.findViewById(R.id.fileIconImageView)
        private val fileNameTextView: TextView = itemView.findViewById(R.id.fileNameTextView)
        private val fileSizeTextView: TextView = itemView.findViewById(R.id.fileSizeTextView)
        private val fileMessageContentView: View = itemView.findViewById(R.id.fileMessageContentView)
        private val downloadProgressBar: View = itemView.findViewById(R.id.downloadProgressBar)
        
        fun bind(
            message: Message,
            onLongClick: (Message) -> Unit,
            onFileClick: ((Message) -> Unit)?
        ) {
            // 解析文件消息内容：格式为 "fileName|fileSize|fileUrl"
            val parts = message.content.split("|")
            val fileName = if (parts.isNotEmpty()) parts[0] else "未知文件"
            val fileSize = if (parts.size >= 2) parts[1].toLongOrNull() ?: 0L else 0L
            
            // 设置文件名
            fileNameTextView.text = fileName
            
            // 设置文件大小
            fileSizeTextView.text = FileTypeHelper.formatFileSize(fileSize)
            
            // 根据文件名设置文件类型图标
            val iconResId = FileTypeHelper.getFileTypeIcon(fileName)
            fileIconImageView.setImageResource(iconResId)
            
            // 点击打开文件
            fileMessageContentView.setOnClickListener {
                onFileClick?.invoke(message)
            }
            
            // 长按菜单
            fileMessageContentView.setOnLongClickListener {
                onLongClick(message)
                true
            }
            
            // 隐藏下载进度条
            downloadProgressBar.visibility = View.GONE
        }
    }
    
    // 接收文件消息ViewHolder (WildFire IM style)
    class FileReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val fileIconImageView: ImageView = itemView.findViewById(R.id.fileIconImageView)
        private val fileNameTextView: TextView = itemView.findViewById(R.id.fileNameTextView)
        private val fileSizeTextView: TextView = itemView.findViewById(R.id.fileSizeTextView)
        private val fileMessageContentView: View = itemView.findViewById(R.id.fileMessageContentView)
        private val downloadProgressBar: View = itemView.findViewById(R.id.downloadProgressBar)
        
        fun bind(
            message: Message,
            onLongClick: (Message) -> Unit,
            onFileClick: ((Message) -> Unit)?
        ) {
            // 解析文件消息内容：格式为 "fileName|fileSize|fileUrl"
            val parts = message.content.split("|")
            val fileName = if (parts.isNotEmpty()) parts[0] else "未知文件"
            val fileSize = if (parts.size >= 2) parts[1].toLongOrNull() ?: 0L else 0L
            
            // 设置文件名
            fileNameTextView.text = fileName
            
            // 设置文件大小
            fileSizeTextView.text = FileTypeHelper.formatFileSize(fileSize)
            
            // 根据文件名设置文件类型图标
            val iconResId = FileTypeHelper.getFileTypeIcon(fileName)
            fileIconImageView.setImageResource(iconResId)
            
            // 点击打开文件
            fileMessageContentView.setOnClickListener {
                onFileClick?.invoke(message)
            }
            
            // 长按菜单
            fileMessageContentView.setOnLongClickListener {
                onLongClick(message)
                true
            }
            
            // 隐藏下载进度条
            downloadProgressBar.visibility = View.GONE
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

