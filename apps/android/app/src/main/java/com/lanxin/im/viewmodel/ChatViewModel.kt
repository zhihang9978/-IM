package com.lanxin.im.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lanxin.im.data.model.Message
import com.lanxin.im.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 聊天ViewModel - 管理聊天界面状态
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {
    
    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private var currentConversationId: Long = 0
    
    fun loadMessages(conversationId: Long) {
        currentConversationId = conversationId
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val messageList = chatRepository.getMessages(conversationId)
                _messages.value = messageList
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun sendMessage(receiverId: Long, content: String, type: String = "text") {
        viewModelScope.launch {
            try {
                val result = chatRepository.sendMessage(receiverId, content, type)
                result.onSuccess {
                    // 消息发送成功，刷新列表
                    loadMessages(currentConversationId)
                }
                result.onFailure {
                    _error.value = it.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
    
    fun recallMessage(messageId: Long) {
        viewModelScope.launch {
            try {
                chatRepository.recallMessage(messageId)
                loadMessages(currentConversationId)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}
