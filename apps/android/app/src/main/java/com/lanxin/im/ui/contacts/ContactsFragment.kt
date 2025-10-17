package com.lanxin.im.ui.contacts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lanxin.im.R
import com.lanxin.im.data.model.Contact
import com.lanxin.im.data.remote.RetrofitClient
import kotlinx.coroutines.launch

/**
 * 通讯录Fragment（按设计文档实现）
 */
class ContactsFragment : Fragment() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ContactAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_contacts, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        loadContacts()
    }
    
    private fun setupRecyclerView() {
        recyclerView = view?.findViewById(R.id.recycler_view) ?: return
        
        adapter = ContactAdapter { contact ->
            // 点击联系人，进入聊天页面
            val intent = android.content.Intent(requireContext(), com.lanxin.im.ui.chat.ChatActivity::class.java)
            intent.putExtra("conversation_id", 0L) // 新会话
            intent.putExtra("peer_id", contact.contactId)
            startActivity(intent)
        }
        
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }
    
    private fun loadContacts() {
        lifecycleScope.launch {
            try {
                // 调用API获取联系人列表
                val response = RetrofitClient.apiService.getContacts()
                if (response.code == 0 && response.data != null) {
                    // 显示联系人列表
                    adapter.submitList(response.data.contacts.map { item ->
                        Contact(
                            id = item.id,
                            userId = item.user.id,
                            contactId = item.contact_id,
                            remark = item.remark,
                            tags = item.tags,
                            status = item.status,
                            createdAt = item.created_at,
                            updatedAt = System.currentTimeMillis()
                        )
                    })
                }
            } catch (e: Exception) {
                // 加载失败，显示空列表
                e.printStackTrace()
            }
        }
    }
}

