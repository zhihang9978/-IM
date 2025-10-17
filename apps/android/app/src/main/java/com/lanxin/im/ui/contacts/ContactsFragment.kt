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
                    // 转换为Contact列表（使用API返回的完整数据）
                    // ⚠️ 处理user可能为null的情况
                    val contacts = response.data.contacts.mapNotNull { item ->
                        if (item.user == null) {
                            // 跳过没有用户信息的联系人
                            android.util.Log.w("ContactsFragment", "Contact ${item.id} has no user info")
                            return@mapNotNull null
                        }
                        
                        Contact(
                            id = item.id,
                            userId = item.user_id,
                            contactId = item.contact_id,
                            username = item.user.username,
                            remark = item.remark,
                            tags = item.tags,
                            status = item.status,
                            createdAt = item.created_at,
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                    
                    // 使用ContactListHelper转换为显示项（带字母分组）
                    // 传入API数据以获取头像等信息
                    val displayItems = ContactListHelper.toDisplayItems(contacts, response.data.contacts)
                    adapter.submitList(displayItems)
                }
            } catch (e: Exception) {
                // 加载失败，显示空列表
                e.printStackTrace()
            }
        }
    }
}

