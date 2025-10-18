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
                    "message_id" to message.id,
                    "content" to message.content,
                    "type" to message.type
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
        // TODO: Implement multi-select mode
        Toast.makeText(activity, "多选功能开发中", Toast.LENGTH_SHORT).show()
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
                
                val request = mapOf(
                    "conversation_id" to conversationId,
                    "content" to messageContent,
                    "type" to "text"
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
                // TODO: Upload voice file and get URL
                val voiceUrl = uploadFile(filePath)
                
                val request = mapOf(
                    "conversation_id" to conversationId,
                    "content" to "语音消息",
                    "type" to "voice",
                    "file_url" to voiceUrl,
                    "duration" to duration
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
     * 发送图片消息
     */
    fun sendImageMessage(imagePath: String) {
        activity.lifecycleScope.launch {
            try {
                val imageUrl = uploadFile(imagePath)
                
                val request = mapOf(
                    "conversation_id" to conversationId,
                    "content" to "[图片]",
                    "type" to "image",
                    "file_url" to imageUrl
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
     * 发送视频消息
     */
    fun sendVideoMessage(videoPath: String) {
        activity.lifecycleScope.launch {
            try {
                val videoUrl = uploadFile(videoPath)
                
                val request = mapOf(
                    "conversation_id" to conversationId,
                    "content" to "[视频]",
                    "type" to "video",
                    "file_url" to videoUrl
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
     * 发送文件消息
     */
    fun sendFileMessage(filePath: String, fileName: String, fileSize: Long) {
        activity.lifecycleScope.launch {
            try {
                val fileUrl = uploadFile(filePath)
                
                val request = mapOf(
                    "conversation_id" to conversationId,
                    "content" to fileName,
                    "type" to "file",
                    "file_url" to fileUrl,
                    "file_size" to fileSize
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
     * Upload file to server
     * TODO: Implement actual file upload with MinIO
     */
    private suspend fun uploadFile(filePath: String): String {
        // Placeholder - should integrate with MinIO
        return "https://example.com/files/${System.currentTimeMillis()}"
    }
    
    fun clearDuplicateCache() {
        receivedMessageIds.clear()
    }
}
