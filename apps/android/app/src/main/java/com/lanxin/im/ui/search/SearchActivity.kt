package com.lanxin.im.ui.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lanxin.im.R
import com.lanxin.im.data.model.Message
import com.lanxin.im.data.remote.RetrofitClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 搜索Activity（完整实现）
 */
class SearchActivity : AppCompatActivity() {
    
    private lateinit var btnBack: ImageButton
    private lateinit var etSearch: EditText
    private lateinit var btnClear: ImageButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var progressBar: ProgressBar
    
    private lateinit var adapter: SearchResultAdapter
    private var searchJob: Job? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        
        setupUI()
        setupListeners()
    }
    
    private fun setupUI() {
        btnBack = findViewById(R.id.btn_back)
        etSearch = findViewById(R.id.et_search)
        btnClear = findViewById(R.id.btn_clear)
        recyclerView = findViewById(R.id.recycler_view_results)
        tvEmpty = findViewById(R.id.tv_empty)
        progressBar = findViewById(R.id.progress_bar)
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = SearchResultAdapter { message ->
            onSearchResultClick(message)
        }
        recyclerView.adapter = adapter
        
        etSearch.requestFocus()
    }
    
    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }
        
        btnClear.setOnClickListener {
            etSearch.text.clear()
        }
        
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                btnClear.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE
                
                if (query.isEmpty()) {
                    showEmptyState()
                } else {
                    performSearch(query)
                }
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
    }
    
    /**
     * 执行搜索（带防抖）
     */
    private fun performSearch(query: String) {
        searchJob?.cancel()
        searchJob = lifecycleScope.launch {
            delay(300)
            
            progressBar.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE
            recyclerView.visibility = View.GONE
            
            try {
                val response = RetrofitClient.apiService.searchMessages(query)
                if (response.code == 0 && response.data != null) {
                    val results = response.data.messages
                    if (results.isEmpty()) {
                        showEmptyState("没有找到相关消息")
                    } else {
                        showResults(results)
                    }
                } else {
                    showEmptyState("搜索失败")
                }
            } catch (e: Exception) {
                showEmptyState("搜索出错")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
    
    /**
     * 显示搜索结果
     */
    private fun showResults(results: List<Message>) {
        adapter.submitList(results)
        recyclerView.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
    }
    
    /**
     * 显示空状态
     */
    private fun showEmptyState(message: String = "输入关键词搜索消息") {
        tvEmpty.text = message
        tvEmpty.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        adapter.submitList(emptyList())
    }
    
    /**
     * 点击搜索结果
     */
    private fun onSearchResultClick(message: Message) {
        val intent = android.content.Intent(this, com.lanxin.im.ui.chat.ChatActivity::class.java)
        intent.putExtra("conversation_id", message.conversationId)
        intent.putExtra("message_id", message.id)
        startActivity(intent)
        finish()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        searchJob?.cancel()
    }
}

