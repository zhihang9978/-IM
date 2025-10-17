package com.lanxin.im.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lanxin.im.data.model.Message
import com.lanxin.im.data.repository.ChatRepository
import com.lanxin.im.data.remote.WebSocketClient
import kotlinx.coroutines.launch

/**
 * 聊天ViewModel
 * MVVM架构模式
 */
class ChatViewModel(
    private val repository: ChatRepository,
    private val webSocketClient: WebSocketClient
) : ViewModel() {
    
    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages
    
    private val _sendStatus = MutableLiveData<SendStatus>()
    val sendStatus: LiveData<SendStatus> = _sendStatus
    
    private var conversationId: Long = 0
    
    // WebSocket监听器
    private val wsListener = object : WebSocketClient.WebSocketListener {
        override fun onConnected() {
            // WebSocket连接成功
        }
        
        override fun onDisconnected() {
            // WebSocket断开连接
        }
        
        override fun onError(error: String) {
            // WebSocket错误
        }
        
        override fun onNewMessage(message: Message) {
            // 收到新消息，更新UI
            if (message.conversationId == conversationId) {
                val currentList = _messages.value.orEmpty().toMutableList()
                currentList.add(message)
                _messages.postValue(currentList)
                
                // 标记已读
                markAsRead()
            }
        }
        
        override fun onMessageStatusUpdate(update: com.lanxin.im.data.remote.MessageStatusUpdate) {
            // 消息状态更新
            val currentList = _messages.value.orEmpty().toMutableList()
            val index = currentList.indexOfFirst { it.id == update.message_id }
            if (index != -1) {
                val message = currentList[index]
                // 更新状态
                currentList[index] = message.copy(status = update.status)
                _messages.postValue(currentList)
                
                // 更新本地数据库
                viewModelScope.launch {
                    repository.updateMessageStatus(update.message_id, update.status)
                }
            }
        }
        
        override fun onCallInvite(invite: com.lanxin.im.data.remote.CallInvite) {
        }
        
        override fun onReadReceipt(receipt: com.lanxin.im.data.remote.ReadReceipt) {
            // 收到已读回执
            if (receipt.conversation_id == conversationId) {
                // 更新该会话中发送给对方的所有消息为已读状态
                val currentList = _messages.value.orEmpty().toMutableList()
                var updated = false
                for (i in currentList.indices) {
                    val message = currentList[i]
                    if (message.senderId != receipt.reader_id && message.receiverId == receipt.reader_id) {
                        if (message.status != "read") {
                            currentList[i] = message.copy(status = "read")
                            updated = true
                        }
                    }
                }
                if (updated) {
                    _messages.postValue(currentList)
                }
            }
        }
    }
    
    init {
        webSocketClient.addListener(wsListener)
    }
    
    /**
     * 加载消息列表
     */
    fun loadMessages(convId: Long) {
        conversationId = convId
        viewModelScope.launch {
            val messageList = repository.getMessages(convId)
            _messages.value = messageList
        }
    }
    
    /**
     * 发送文本消息
     */
    fun sendTextMessage(content: String, receiverId: Long) {
        viewModelScope.launch {
            _sendStatus.value = SendStatus.Sending
            
            val result = repository.sendMessage(
                receiverId = receiverId,
                content = content,
                type = "text"
            )
            
            result.onSuccess { message ->
                _sendStatus.value = SendStatus.Success
                // 添加到消息列表
                val currentList = _messages.value.orEmpty().toMutableList()
                currentList.add(message)
                _messages.value = currentList
            }.onFailure { error ->
                _sendStatus.value = SendStatus.Failure(error.message ?: "发送失败")
            }
        }
    }
    
    /**
     * 撤回消息
     */
    fun recallMessage(messageId: Long) {
        viewModelScope.launch {
            val result = repository.recallMessage(messageId)
            result.onSuccess {
                // 更新UI中的消息状态
                val currentList = _messages.value.orEmpty().toMutableList()
                val index = currentList.indexOfFirst { it.id == messageId }
                if (index != -1) {
                    val message = currentList[index]
                    currentList[index] = message.copy(status = "recalled")
                    _messages.value = currentList
                }
            }
        }
    }
    
    /**
     * 标记消息已读
     */
    private fun markAsRead() {
        viewModelScope.launch {
            repository.clearUnreadCount(conversationId)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        webSocketClient.removeListener(wsListener)
    }
}

// 发送状态
sealed class SendStatus {
    object Idle : SendStatus()
    object Sending : SendStatus()
    object Success : SendStatus()
    data class Failure(val error: String) : SendStatus()
}

// Message扩展函数（用于copy）
fun Message.copy(
    id: Long = this.id,
    conversationId: Long = this.conversationId,
    senderId: Long = this.senderId,
    receiverId: Long = this.receiverId,
    content: String = this.content,
    type: String = this.type,
    fileUrl: String? = this.fileUrl,
    fileSize: Long? = this.fileSize,
    duration: Int? = this.duration,
    status: String = this.status,
    createdAt: Long = this.createdAt,
    updatedAt: Long = this.updatedAt
): Message {
    return Message(
        id, conversationId, senderId, receiverId, content, type,
        fileUrl, fileSize, duration, status, createdAt, updatedAt
    )
}

