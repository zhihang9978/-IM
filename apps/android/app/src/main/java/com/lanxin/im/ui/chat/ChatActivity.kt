package com.lanxin.im.ui.chat

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lanxin.im.R

/**
 * 1对1聊天Activity
 * 从HTML原型转换而来
 */
class ChatActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        
        setupUI()
    }
    
    private fun setupUI() {
        recyclerView = findViewById(R.id.recycler_view_messages)
        recyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        
        // TODO: 设置Adapter，加载消息
        // TODO: 实现WebSocket连接
        // TODO: 实现发送消息
        // TODO: 实现消息长按菜单（复制、撤回、删除）
    }
}

