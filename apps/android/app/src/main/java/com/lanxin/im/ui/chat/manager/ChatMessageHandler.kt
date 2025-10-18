package com.lanxin.im.ui.chat.manager

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.lanxin.im.R
import com.lanxin.im.data.model.Message
import com.lanxin.im.data.remote.RetrofitClient
import com.lanxin.im.ui.chat.ChatAdapter
import com.lanxin.im.ui.chat.ForwardActivity
import com.lanxin.im.ui.report.ReportActivity
import com.lanxin.im.utils.BurnAfterReadHelper
import kotlinx.coroutines.launch

/**
 * 聊天消息操作处理器
 * 负责处理消息的所有操作（发送/撤回/删除/转发/收藏/举报）
 * 
 * 参考：WildFireChat MessageManager (Apache 2.0)
 */
class ChatMessageHandler(
    private val activity: AppCompatActivity,
    private val adapter: ChatAdapter,
    private val conversationId: Long,
    private val currentUserId: Long,
    private val onQuoteMessage: (Message) -> Unit,
    private val onLoadMessages: () -> Unit
) {
    
    private val receivedMessageIds = mutableSetOf<Long>()
    
    /**
     * 消息去重检查
     * IM知识库要求：必须实现消息去重机制
     */
    fun isDuplicateMessage(messageId: Long): Boolean {
        return !receivedMessageIds.add(messageId)
    }
    
    /**
     * 显示消息长按菜单
     * WildFire IM style: 8个功能选项
     */
    fun showMessageMenu(message: Message, anchorView: android.view.View) {
        val popup = PopupMenu(activity, anchorView)
        popup.menuInflater.inflate(R.menu.menu_message_context_wildfire, popup.menu)
        
        // Control menu items visibility
        val isSent = message.senderId == currentUserId
        val canRecall = isSent && (System.currentTimeMillis() - message.createdAt < 120000) // 2 minutes
        val isTextMessage = message.type == "text"
        
        popup.menu.findItem(R.id.menu_copy)?.isVisible = isTextMessage
        popup.menu.findItem(R.id.menu_recall)?.isVisible = canRecall
        
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_copy -> {
                    copyMessageToClipboard(message.content)
                    true
                }
                R.id.menu_quote -> {
                    onQuoteMessage(message)
                    true
                }
                R.id.menu_forward -> {
                    forwardMessage(message)
                    true
                }
                R.id.menu_collect -> {
                    collectMessage(message)
                    true
                }
                R.id.menu_recall -> {
                    recallMessage(message)
                    true
                }
                R.id.menu_delete -> {
                    deleteMessage(message)
                    true
                }
                R.id.menu_select -> {
                    enterMultiSelectMode(message)
                    true
                }
                R.id.menu_report -> {
                    reportMessage(message)
                    true
                }
                else -> false
            }
        }
        
        popup.show()
    }
    
    private fun copyMessageToClipboard(content: String) {
        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("message", content)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(activity, "已复制", Toast.LENGTH_SHORT).show()
    }
    
    fun recallMessage(message: Message) {
        activity.lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.recallMessage(message.id)
                if (response.code == 0) {
                    Toast.makeText(activity, "已撤回", Toast.LENGTH_SHORT).show()
                    onLoadMessages()
                } else {
                    Toast.makeText(activity, response.message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(activity, "撤回失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    fun deleteMessage(message: Message) {
        AlertDialog.Builder(activity)
            .setTitle("删除消息")
            .setMessage("确定要删除这条消息吗？")
            .setPositiveButton("删除") { _, _ ->
                activity.lifecycleScope.launch {
                    try {
                        // Local deletion
                        val currentList = adapter.currentList.toMutableList()
                        currentList.remove(message)
                        adapter.submitList(currentList)
                        Toast.makeText(activity, "已删除", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(activity, "删除失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    fun forwardMessage(message: Message) {
        val intent = Intent(activity, ForwardActivity::class.java)
        intent.putExtra("message_id", message.id)
        intent.putExtra("message_content", message.content)
        intent.putExtra("message_type", message.type)
        activity.startActivity(intent)
    }
    
    fun collectMessage(message: Message) {
        activity.lifecycleScope.launch {
            try {
                val request = mapOf(
                    "message_id" to message.id
                )
                val response = RetrofitClient.apiService.collectMessage(request)
                
                if (response.code == 0) {
                    Toast.makeText(activity, "已收藏", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(activity, response.message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(activity, "收藏失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    fun enterMultiSelectMode(message: Message) {
        // Enter multi-select mode for batch operations
        val currentList = adapter.currentList.toMutableList()
        val selectedMessages = mutableListOf(message)
        
        AlertDialog.Builder(activity)
            .setTitle("多选操作")
            .setMessage("已选择 ${selectedMessages.size} 条消息")
            .setPositiveButton("转发") { _, _ ->
                selectedMessages.forEach { forwardMessage(it) }
            }
            .setNegativeButton("删除") { _, _ ->
                selectedMessages.forEach { msg ->
                    currentList.remove(msg)
                }
                adapter.submitList(currentList)
            }
            .setNeutralButton("取消", null)
            .show()
    }
    
    fun reportMessage(message: Message) {
        val intent = Intent(activity, ReportActivity::class.java)
        intent.putExtra("message_id", message.id)
        intent.putExtra("conversation_id", conversationId)
        activity.startActivity(intent)
    }
    
    fun deleteBurnMessage(message: Message) {
        activity.lifecycleScope.launch {
            try {
                // Mark message as read and delete locally
                BurnAfterReadHelper.markAsRead(message.id)
                
                val currentList = adapter.currentList.toMutableList()
                currentList.remove(message)
                adapter.submitList(currentList)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 发送文本消息
     */
    fun sendTextMessage(
        content: String,
        quotedMessage: Message? = null,
        mentionedUsers: List<Long> = emptyList()
    ) {
        activity.lifecycleScope.launch {
            try {
                var messageContent = content
                
                // Handle @ mentions
                if (mentionedUsers.isNotEmpty()) {
                    messageContent = "$messageContent|MENTIONS:${mentionedUsers.joinToString(",")}"
                }
                
                // Handle quoted message
                quotedMessage?.let {
                    messageContent = "$messageContent|QUOTE:${it.id}"
                }
                
                val request = com.lanxin.im.data.remote.SendMessageRequest(
                    receiver_id = conversationId,
                    content = messageContent,
                    type = "text"
                )
                
                val response = RetrofitClient.apiService.sendMessage(request)
                
                if (response.code == 0) {
                    onLoadMessages()
                } else {
                    Toast.makeText(activity, "发送失败: ${response.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(activity, "发送失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 发送语音消息
     */
    fun sendVoiceMessage(filePath: String, duration: Int) {
        activity.lifecycleScope.launch {
            try {
                // 上传语音文件到MinIO
                val uploadResult = com.lanxin.im.utils.MinIOUploader.uploadVoice(filePath)
                
                when (uploadResult) {
                    is com.lanxin.im.utils.Result.Error -> {
                        Toast.makeText(activity, "语音上传失败", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    is com.lanxin.im.utils.Result.Success -> {
                        val voiceUrl = uploadResult.data
                        
                        val request = com.lanxin.im.data.remote.SendMessageRequest(
                            receiver_id = conversationId,
                            content = duration.toString(),
                            type = "voice",
                            file_url = voiceUrl,
                            duration = duration
                        )
                        
                        val response = RetrofitClient.apiService.sendMessage(request)
                        
                        if (response.code == 0) {
                            onLoadMessages()
                        } else {
                            Toast.makeText(activity, "发送失败: ${response.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Toast.makeText(activity, "发送失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 发送图片消息
     */
    fun sendImageMessage(imagePath: String) {
        activity.lifecycleScope.launch {
            try {
                // 上传图片到MinIO（自动压缩）
                val uploadResult = com.lanxin.im.utils.MinIOUploader.uploadImage(activity, imagePath)
                
                when (uploadResult) {
                    is com.lanxin.im.utils.Result.Error -> {
                        Toast.makeText(activity, "图片上传失败", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    is com.lanxin.im.utils.Result.Success -> {
                        val imageUrl = uploadResult.data
                        
                        val request = com.lanxin.im.data.remote.SendMessageRequest(
                            receiver_id = conversationId,
                            content = imageUrl,
                            type = "image",
                            file_url = imageUrl
                        )
                        
                        val response = RetrofitClient.apiService.sendMessage(request)
                        
                        if (response.code == 0) {
                            onLoadMessages()
                        } else {
                            Toast.makeText(activity, "发送失败: ${response.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Toast.makeText(activity, "发送失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 发送视频消息
     */
    fun sendVideoMessage(videoPath: String) {
        activity.lifecycleScope.launch {
            try {
                // 上传视频到MinIO
                val uploadResult = com.lanxin.im.utils.MinIOUploader.uploadVideo(videoPath)
                
                when (uploadResult) {
                    is com.lanxin.im.utils.Result.Error -> {
                        Toast.makeText(activity, "视频上传失败", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    is com.lanxin.im.utils.Result.Success -> {
                        val videoUrl = uploadResult.data
                        
                        // 获取视频时长
                        val duration = com.lanxin.im.utils.VideoUtils.getVideoDuration(videoPath)
                        
                        val request = com.lanxin.im.data.remote.SendMessageRequest(
                            receiver_id = conversationId,
                            content = "$videoUrl|$duration",
                            type = "video",
                            file_url = videoUrl
                        )
                        
                        val response = RetrofitClient.apiService.sendMessage(request)
                        
                        if (response.code == 0) {
                            onLoadMessages()
                        } else {
                            Toast.makeText(activity, "发送失败: ${response.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Toast.makeText(activity, "发送失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 发送文件消息
     */
    fun sendFileMessage(filePath: String, fileName: String, fileSize: Long) {
        activity.lifecycleScope.launch {
            try {
                // 上传文件到MinIO
                val uploadResult = com.lanxin.im.utils.MinIOUploader.uploadDocument(filePath)
                
                when (uploadResult) {
                    is com.lanxin.im.utils.Result.Error -> {
                        Toast.makeText(activity, "文件上传失败", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    is com.lanxin.im.utils.Result.Success -> {
                        val fileUrl = uploadResult.data
                        
                        val request = com.lanxin.im.data.remote.SendMessageRequest(
                            receiver_id = conversationId,
                            content = fileName,
                            type = "file",
                            file_url = fileUrl,
                            file_size = fileSize
                        )
                        
                        val response = RetrofitClient.apiService.sendMessage(request)
                        
                        if (response.code == 0) {
                            onLoadMessages()
                        } else {
                            Toast.makeText(activity, "发送失败: ${response.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Toast.makeText(activity, "发送失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    fun clearDuplicateCache() {
        receivedMessageIds.clear()
    }
}
