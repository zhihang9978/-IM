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
import com.lanxin.im.data.remote.RetrofitClient
import com.lanxin.im.ui.profile.UserInfoActivity
import kotlinx.coroutines.launch

/**
 * 通讯录Fragment - 野火IM风格UI
 */
class ContactsFragment : Fragment() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ContactsAdapter
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
        
        setupRecyclerView(view)
        setupSearchBar(view)
        loadContacts()
    }
    
    private fun setupRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.recycler_view)
        
        adapter = ContactsAdapter { contact ->
            val intent = Intent(requireContext(), UserInfoActivity::class.java)
            intent.putExtra("user_id", contact.userId)
            startActivity(intent)
        }
        
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }
    
    private fun setupSearchBar(view: View) {
        searchBarLayout = view.findViewById(R.id.searchBarLayout)
        searchBarLayout.setOnClickListener {
            val intent = Intent(requireContext(), com.lanxin.im.ui.search.SearchActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun loadContacts() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getContacts()
                if (response.code == 0 && response.data != null) {
                    adapter.submitList(response.data.contacts)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
