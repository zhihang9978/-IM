package com.lanxin.im.ui.chat.manager

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.lanxin.im.R
import com.lanxin.im.data.model.Message
import com.lanxin.im.utils.VoiceRecorder

/**
 * 聊天输入管理器
 * 负责管理输入面板的所有交互（文本/语音/表情/扩展）
 * 
 * 参考：WildFireChat ConversationInputPanel (Apache 2.0)
 */
class ChatInputManager(
    private val activity: Activity,
    private val onSendMessage: (String) -> Unit,
    private val onSendVoice: (String, Int) -> Unit,
    private val onMentionTriggered: () -> Unit
) {
    
    // Input panel state
    enum class InputPanelState {
        TEXT, VOICE, EMOTION, EXTENSION
    }
    
    private var inputPanelState = InputPanelState.TEXT
    private var quotedMessage: Message? = null
    private val mentionedUsers = mutableListOf<Long>()
    
    // View references
    private lateinit var audioImageView: ImageView
    private lateinit var etInput: EditText
    private lateinit var emotionImageView: ImageView
    private lateinit var extImageView: ImageView
    private lateinit var btnSend: Button
    private lateinit var audioButton: Button
    private lateinit var emotionContainerFrameLayout: FrameLayout
    private lateinit var extContainerContainerLayout: FrameLayout
    private lateinit var refRelativeLayout: View
    private lateinit var refEditText: EditText
    private lateinit var clearRefImageButton: ImageButton
    
    // Recording overlay
    private lateinit var recordingOverlay: FrameLayout
    private lateinit var tvRecordingTime: TextView
    private lateinit var tvRecordingHint: TextView
    private lateinit var ivRecordingIcon: ImageView
    
    // Voice recording
    private lateinit var voiceRecorder: VoiceRecorder
    private val recordingHandler = Handler(Looper.getMainLooper())
    private var recordingStartY = 0f
    private var recordingStartTime = 0L
    
    fun initialize() {
        // Find views
        audioImageView = activity.findViewById(R.id.audioImageView)
        etInput = activity.findViewById(R.id.editText)
        emotionImageView = activity.findViewById(R.id.emotionImageView)
        extImageView = activity.findViewById(R.id.extImageView)
        btnSend = activity.findViewById(R.id.sendButton)
        audioButton = activity.findViewById(R.id.audioButton)
        emotionContainerFrameLayout = activity.findViewById(R.id.emotionContainerFrameLayout)
        extContainerContainerLayout = activity.findViewById(R.id.extContainerContainerLayout)
        refRelativeLayout = activity.findViewById(R.id.refRelativeLayout)
        refEditText = activity.findViewById(R.id.refEditText)
        clearRefImageButton = activity.findViewById(R.id.clearRefImageButton)
        
        recordingOverlay = activity.findViewById(R.id.recording_overlay)
        tvRecordingTime = activity.findViewById(R.id.tv_recording_time)
        tvRecordingHint = activity.findViewById(R.id.tv_recording_hint)
        ivRecordingIcon = activity.findViewById(R.id.iv_recording_icon)
        
        // Initialize voice recorder
        voiceRecorder = VoiceRecorder(activity)
        
        setupListeners()
    }
    
    private fun setupListeners() {
        // Input text change listener
        etInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // @ mention detection
                if (s != null && count == 1 && s[start] == '@') {
                    onMentionTriggered()
                }
                
                // Show/hide send button
                val hasText = !s.isNullOrEmpty()
                btnSend.visibility = if (hasText) View.VISIBLE else View.GONE
                extImageView.visibility = if (hasText) View.GONE else View.VISIBLE
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
        
        // Send button
        btnSend.setOnClickListener {
            val content = etInput.text.toString().trim()
            if (content.isNotEmpty()) {
                onSendMessage(content)
                etInput.text.clear()
                mentionedUsers.clear()
            }
        }
        
        // Voice/Text toggle
        audioImageView.setOnClickListener {
            toggleVoiceTextMode()
        }
        
        // Emotion panel toggle
        emotionImageView.setOnClickListener {
            toggleEmotionPanel()
        }
        
        // Extension panel toggle
        extImageView.setOnClickListener {
            toggleExtensionPanel()
        }
        
        // Clear quoted message
        clearRefImageButton.setOnClickListener {
            clearQuotedMessage()
        }
        
        // Long press recording
        audioButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    recordingStartY = event.rawY
                    startRecording()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = recordingStartY - event.rawY
                    if (deltaY > 100) {
                        tvRecordingHint.text = "松开取消"
                        tvRecordingHint.setTextColor(activity.getColor(R.color.error))
                    } else {
                        tvRecordingHint.text = "松开发送 上滑取消"
                        tvRecordingHint.setTextColor(activity.getColor(R.color.text_secondary))
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val deltaY = recordingStartY - event.rawY
                    if (deltaY > 100) {
                        cancelRecording()
                    } else {
                        stopRecording()
                    }
                    true
                }
                else -> false
            }
        }
    }
    
    private fun toggleVoiceTextMode() {
        when (inputPanelState) {
            InputPanelState.TEXT, InputPanelState.EMOTION, InputPanelState.EXTENSION -> {
                // Switch to voice mode
                etInput.visibility = View.GONE
                audioButton.visibility = View.VISIBLE
                btnSend.visibility = View.GONE
                extImageView.visibility = View.GONE
                emotionContainerFrameLayout.visibility = View.GONE
                extContainerContainerLayout.visibility = View.GONE
                audioImageView.setImageResource(R.drawable.ic_keyboard)
                inputPanelState = InputPanelState.VOICE
                hideKeyboard()
            }
            InputPanelState.VOICE -> {
                // Switch to text mode
                etInput.visibility = View.VISIBLE
                audioButton.visibility = View.GONE
                btnSend.visibility = if (etInput.text.isNotEmpty()) View.VISIBLE else View.GONE
                extImageView.visibility = if (etInput.text.isEmpty()) View.VISIBLE else View.GONE
                audioImageView.setImageResource(R.drawable.ic_voice)
                inputPanelState = InputPanelState.TEXT
                showKeyboard()
            }
        }
    }
    
    private fun toggleEmotionPanel() {
        when (inputPanelState) {
            InputPanelState.EMOTION -> {
                emotionContainerFrameLayout.visibility = View.GONE
                inputPanelState = InputPanelState.TEXT
                showKeyboard()
            }
            else -> {
                emotionContainerFrameLayout.visibility = View.VISIBLE
                extContainerContainerLayout.visibility = View.GONE
                inputPanelState = InputPanelState.EMOTION
                hideKeyboard()
            }
        }
    }
    
    private fun toggleExtensionPanel() {
        when (inputPanelState) {
            InputPanelState.EXTENSION -> {
                extContainerContainerLayout.visibility = View.GONE
                inputPanelState = InputPanelState.TEXT
                showKeyboard()
            }
            else -> {
                extContainerContainerLayout.visibility = View.VISIBLE
                emotionContainerFrameLayout.visibility = View.GONE
                inputPanelState = InputPanelState.EXTENSION
                hideKeyboard()
            }
        }
    }
    
    private fun hideKeyboard() {
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etInput.windowToken, 0)
    }
    
    private fun showKeyboard() {
        etInput.requestFocus()
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(etInput, InputMethodManager.SHOW_IMPLICIT)
    }
    
    fun quoteMessage(message: Message) {
        quotedMessage = message
        refRelativeLayout.visibility = View.VISIBLE
        
        // Display quoted message info
        val quotedText = when (message.type) {
            "text" -> message.content
            "image" -> "[图片]"
            "voice" -> "[语音]"
            "video" -> "[视频]"
            "file" -> "[文件]"
            else -> "[消息]"
        }
        refEditText.setText(quotedText)
        
        // Show keyboard and focus input
        etInput.requestFocus()
        showKeyboard()
    }
    
    private fun clearQuotedMessage() {
        quotedMessage = null
        refRelativeLayout.visibility = View.GONE
        refEditText.text.clear()
    }
    
    fun insertMention(memberName: String, memberId: Long) {
        val currentText = etInput.text.toString()
        val atIndex = currentText.lastIndexOf('@')
        
        if (atIndex != -1) {
            val beforeAt = currentText.substring(0, atIndex)
            val afterAt = currentText.substring(atIndex + 1)
            
            // Remove any partial input after @
            val newText = "$beforeAt@$memberName "
            etInput.setText(newText)
            etInput.setSelection(newText.length)
            
            // Add to mentioned users list
            mentionedUsers.add(memberId)
        }
    }
    
    private fun startRecording() {
        if (voiceRecorder.startRecording()) {
            recordingOverlay.visibility = View.VISIBLE
            recordingStartTime = System.currentTimeMillis()
            startRecordingTimer()
        }
    }
    
    private fun stopRecording() {
        val (filePath, actualDuration) = voiceRecorder.stopRecording()
        
        recordingOverlay.visibility = View.GONE
        recordingHandler.removeCallbacksAndMessages(null)
        
        if (filePath != null && actualDuration >= 1) {
            onSendVoice(filePath, actualDuration)
        }
    }
    
    private fun cancelRecording() {
        voiceRecorder.cancelRecording()
        recordingOverlay.visibility = View.GONE
        recordingHandler.removeCallbacksAndMessages(null)
    }
    
    private fun startRecordingTimer() {
        recordingHandler.post(object : Runnable {
            override fun run() {
                val elapsed = (System.currentTimeMillis() - recordingStartTime) / 1000
                val minutes = elapsed / 60
                val seconds = elapsed % 60
                tvRecordingTime.text = String.format("%02d:%02d", minutes, seconds)
                
                // Max 60 seconds
                if (elapsed < 60) {
                    recordingHandler.postDelayed(this, 1000)
                } else {
                    stopRecording()
                }
            }
        })
    }
    
    fun getExtensionContainer(): FrameLayout = extContainerContainerLayout
    
    fun getMentionedUsers(): List<Long> = mentionedUsers.toList()
    
    fun getQuotedMessage(): Message? = quotedMessage
    
    fun cleanup() {
        voiceRecorder.release()
        recordingHandler.removeCallbacksAndMessages(null)
    }
}
