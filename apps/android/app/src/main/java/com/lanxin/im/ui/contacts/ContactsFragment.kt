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
                val response = RetrofitClient.apiService.getContacts()
                android.util.Log.d("ContactsFragment", "API Response: code=${response.code}, data=${response.data}")
                
                if (response.code == 0 && response.data != null) {
                    val contacts = response.data.contacts
                    android.util.Log.d("ContactsFragment", "Contacts count: ${contacts.size}")
                    
                    if (contacts.isEmpty()) {
                        android.widget.Toast.makeText(requireContext(), "暂无联系人", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        adapter.submitList(contacts.map { item ->
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
                } else {
                    android.util.Log.e("ContactsFragment", "API Error: ${response.message}")
                    android.widget.Toast.makeText(requireContext(), "获取联系人列表失败: ${response.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("ContactsFragment", "Exception loading contacts", e)
                android.widget.Toast.makeText(requireContext(), "网络错误: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
}

