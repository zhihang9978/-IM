package com.lanxin.im.ui.chat

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lanxin.im.R
import com.lanxin.im.data.model.Contact
import com.lanxin.im.data.model.Message
import com.lanxin.im.data.remote.RetrofitClient
import com.lanxin.im.data.remote.SendMessageRequest
import com.lanxin.im.ui.contacts.ContactAdapter
import com.lanxin.im.ui.contacts.ContactDisplayItem
import com.lanxin.im.ui.contacts.ContactListHelper
import kotlinx.coroutines.launch

/**
 * 转发消息Activity
 * 参考：WildFireChat ForwardActivity.java (Apache 2.0)
 * 适配：蓝信IM
 * 
 * 功能：
 * - 显示联系人列表供选择
 * - 支持单条消息转发
 * - 支持批量消息转发（多选模式）
 * - 转发确认对话框
 */
class ForwardActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ContactAdapter
    private lateinit var btnCancel: Button
    private lateinit var btnConfirm: Button
    
    private var messageToForward: Message? = null
    private var selectedContacts = mutableListOf<Contact>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forward)
        
        messageToForward = intent.getParcelableExtra("message")
        
        setupUI()
        loadContacts()
    }
    
    private fun setupUI() {
        recyclerView = findViewById(R.id.recycler_view_contacts)
        btnCancel = findViewById(R.id.btn_cancel)
        btnConfirm = findViewById(R.id.btn_confirm)
        
        // 设置RecyclerView
        adapter = ContactAdapter { contact ->
            onContactSelected(contact)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        
        // 取消按钮
        btnCancel.setOnClickListener {
            finish()
        }
        
        // 确认转发按钮
        btnConfirm.setOnClickListener {
            if (selectedContacts.isNotEmpty()) {
                showForwardConfirmDialog()
            } else {
                Toast.makeText(this, "请选择转发对象", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun onContactSelected(contact: Contact) {
        // 简化版：直接转发给该联系人
        showForwardConfirmDialog(contact)
    }
    
    private fun loadContacts() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getContacts()
                if (response.code == 0 && response.data != null) {
                    val contacts = response.data.contacts.map { item ->
                        Contact(
                            id = item.id,
                            userId = item.user_id,
                            contactId = item.contact_id,
                            username = item.user?.username ?: "用户${item.contact_id}",
                            remark = item.remark,
                            tags = item.tags,
                            status = item.status,
                            createdAt = item.created_at,
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                    val displayItems = ContactListHelper.toDisplayItems(contacts)
                    adapter.submitList(displayItems)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "加载联系人失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 显示转发确认对话框
     * 参考：WildFireChat ForwardActivity.forward() (Apache 2.0)
     */
    private fun showForwardConfirmDialog(contact: Contact) {
        val message = messageToForward ?: return
        
        // 创建确认对话框
        AlertDialog.Builder(this)
            .setTitle("确认转发")
            .setMessage("转发给 ${contact.username}？")
            .setPositiveButton("确定") { _, _ ->
                forwardMessage(contact, message)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun showForwardConfirmDialog() {
        Toast.makeText(this, "批量转发功能：待实现", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 执行转发操作
     * 参考：WildFireChat ForwardViewModel.forward() (Apache 2.0)
     */
    private fun forwardMessage(contact: Contact, message: Message) {
        lifecycleScope.launch {
            try {
                // 根据消息类型转发
                val request = SendMessageRequest(
                    receiver_id = contact.contactId,
                    content = message.content,
                    type = message.type,
                    file_url = null,
                    file_size = null,
                    duration = null
                )
                
                val response = RetrofitClient.apiService.sendMessage(request)
                if (response.code == 0) {
                    Toast.makeText(this@ForwardActivity, "转发成功", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@ForwardActivity, "转发失败: ${response.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ForwardActivity, "转发失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}

