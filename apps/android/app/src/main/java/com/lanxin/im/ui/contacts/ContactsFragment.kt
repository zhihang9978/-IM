package com.lanxin.im.ui.contacts

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lanxin.im.R
import com.lanxin.im.data.model.Contact
import com.lanxin.im.data.remote.RetrofitClient
import kotlinx.coroutines.launch

/**
 * 通讯录Fragment - 野火IM风格UI
 */
class ContactsFragment : Fragment() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ContactAdapter
    private lateinit var searchBarLayout: LinearLayout
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_contacts_new, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupClickListeners(view)
        loadContacts()
    }
    
    private fun setupRecyclerView() {
        recyclerView = view?.findViewById(R.id.recycler_view) ?: return
        
        adapter = ContactAdapter { contact ->
            val intent = Intent(requireContext(), com.lanxin.im.ui.chat.ChatActivity::class.java)
            intent.putExtra("conversation_id", 0L)
            intent.putExtra("peer_id", contact.contactId)
            startActivity(intent)
        }
        
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }
    
    private fun setupClickListeners(view: View) {
        // 新的朋友
        view.findViewById<View>(R.id.btn_new_friends)?.setOnClickListener {
            val intent = Intent(requireContext(), com.lanxin.im.ui.contacts.NewFriendsActivity::class.java)
            startActivity(intent)
        }
        
        // 群聊
        view.findViewById<View>(R.id.btn_groups)?.setOnClickListener {
            val intent = Intent(requireContext(), com.lanxin.im.ui.group.GroupListActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun loadContacts() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getContacts()
                if (response.code == 0 && response.data != null) {
                    val contacts = response.data.contacts.mapNotNull { item ->
                        if (item.user == null) {
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
                    
                    val displayItems = ContactListHelper.toDisplayItems(contacts, response.data.contacts)
                    adapter.submitList(displayItems)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

