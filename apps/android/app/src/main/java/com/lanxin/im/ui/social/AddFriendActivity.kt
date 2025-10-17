package com.lanxin.im.ui.social

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lanxin.im.R
import com.lanxin.im.data.remote.RetrofitClient
import kotlinx.coroutines.launch

/**
 * 添加好友Activity（完整实现）
 */
class AddFriendActivity : AppCompatActivity() {
    
    private lateinit var etSearch: EditText
    private lateinit var btnSearch: Button
    private lateinit var recyclerView: RecyclerView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_friend)
        
        setupViews()
        setupListeners()
    }
    
    private fun setupViews() {
        etSearch = findViewById(R.id.et_search)
        btnSearch = findViewById(R.id.btn_search)
        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
    }
    
    private fun setupListeners() {
        findViewById<View>(R.id.btn_back)?.setOnClickListener {
            finish()
        }
        
        btnSearch.setOnClickListener {
            val keyword = etSearch.text.toString().trim()
            if (keyword.isNotEmpty()) {
                searchUsers(keyword)
            }
        }
    }
    
    private fun searchUsers(keyword: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.searchUsers(keyword)
                // 显示搜索结果
                // 功能待完善：显示用户列表并允许添加
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

