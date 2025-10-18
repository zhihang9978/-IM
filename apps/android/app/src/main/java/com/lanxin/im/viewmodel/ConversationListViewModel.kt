package com.lanxin.im.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.lanxin.im.data.model.Conversation
import com.lanxin.im.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 对话列表ViewModel - 管理会话列表状态
 */
@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {
    
    // 使用Flow转LiveData，自动更新UI
    val conversations: LiveData<List<Conversation>> = 
        chatRepository.observeConversations().asLiveData()
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    init {
        refreshConversations()
    }
    
    fun refreshConversations() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                chatRepository.refreshConversations()
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearUnreadCount(conversationId: Long) {
        viewModelScope.launch {
            try {
                chatRepository.clearUnreadCount(conversationId)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}
