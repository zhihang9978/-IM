package com.lanxin.im.ui.group

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
 * 群聊列表
 */
@AndroidEntryPoint
class GroupListActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_list)
        
        setupToolbar()
        setupRecyclerView()
        loadGroups()
    }
    
    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "群聊"
    }
    
    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
    }
    
    private fun loadGroups() {
        lifecycleScope.launch {
            try {
                // TODO: 实现群组列表加载
                // val response = RetrofitClient.apiService.getGroups()
                // if (response.code == 0 && response.data != null) {
                //     // 显示群组列表
                // }
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
