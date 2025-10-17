package com.lanxin.im.ui.chat

/**
 * 输入面板状态枚举
 * 参考：WildFireChat ConversationInputPanel.java (Apache 2.0)
 * 适配：蓝信IM
 */
enum class InputPanelState {
    /**
     * 文本输入模式（默认）
     * 显示：输入框、发送按钮
     */
    TEXT,
    
    /**
     * 语音输入模式
     * 显示："按住说话"按钮
     */
    VOICE,
    
    /**
     * 表情面板显示
     * 显示：表情选择器
     */
    EMOTION,
    
    /**
     * 扩展面板显示
     * 显示：扩展功能网格
     */
    EXTENSION,
    
    /**
     * 无状态（面板隐藏）
     */
    NONE
}

