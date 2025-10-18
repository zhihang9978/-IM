package com.lanxin.im.ui.contacts

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
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
    
    private lateinit var toolbar: Toolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var loadingView: ProgressBar
    private lateinit var adapter: FriendRequestAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_friends)
        
        setupToolbar()
        setupViews()
        setupRecyclerView()
        loadFriendRequests()
    }
    
    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupViews() {
        emptyView = findViewById(R.id.empty_view)
        loadingView = findViewById(R.id.loading_view)
    }
    
    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        adapter = FriendRequestAdapter(
            onAccept = { requestId -> handleAccept(requestId) },
            onReject = { requestId -> handleReject(requestId) }
        )
        recyclerView.adapter = adapter
    }
    
    private fun loadFriendRequests() {
        showLoading()
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getFriendRequests(type = "received")
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    if (apiResponse.code == 0 && apiResponse.data != null) {
                        val requests = apiResponse.data.requests
                        if (requests.isEmpty()) {
                            showEmpty()
                        } else {
                            showContent()
                            adapter.submitList(requests)
                        }
                    } else {
                        showEmpty()
                        Toast.makeText(this@NewFriendsActivity, apiResponse.message ?: "加载失败", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    showEmpty()
                    Toast.makeText(this@NewFriendsActivity, "加载失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showEmpty()
                Toast.makeText(this@NewFriendsActivity, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun handleAccept(requestId: Long) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.acceptFriendRequest(requestId)
                if (response.isSuccessful) {
                    Toast.makeText(this@NewFriendsActivity, "已添加为好友", Toast.LENGTH_SHORT).show()
                    loadFriendRequests()
                } else {
                    Toast.makeText(this@NewFriendsActivity, "操作失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@NewFriendsActivity, "网络错误", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun handleReject(requestId: Long) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.rejectFriendRequest(requestId)
                if (response.isSuccessful) {
                    Toast.makeText(this@NewFriendsActivity, "已拒绝", Toast.LENGTH_SHORT).show()
                    loadFriendRequests()
                } else {
                    Toast.makeText(this@NewFriendsActivity, "操作失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@NewFriendsActivity, "网络错误", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showLoading() {
        loadingView.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyView.visibility = View.GONE
    }
    
    private fun showContent() {
        loadingView.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        emptyView.visibility = View.GONE
    }
    
    private fun showEmpty() {
        loadingView.visibility = View.GONE
        recyclerView.visibility = View.GONE
        emptyView.visibility = View.VISIBLE
    }
}
