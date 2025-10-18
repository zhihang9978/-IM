package com.lanxin.im.ui.contacts

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lanxin.im.R
import com.lanxin.im.data.remote.RetrofitClient
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint

/**
 * 新的朋友 - 好友申请列表
 */
@AndroidEntryPoint
class NewFriendsActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_friends)
        
        setupToolbar()
        setupRecyclerView()
        loadFriendRequests()
    }
    
    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "新的朋友"
    }
    
    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
    }
    
    private fun loadFriendRequests() {
        lifecycleScope.launch {
            try {
                // TODO: 实现好友申请列表加载
                // val response = RetrofitClient.apiService.getFriendRequests()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
