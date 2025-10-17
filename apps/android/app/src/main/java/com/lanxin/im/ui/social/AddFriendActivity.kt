package com.lanxin.im.ui.social

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lanxin.im.R
import com.lanxin.im.data.model.User
import com.lanxin.im.data.remote.AddContactRequest
import com.lanxin.im.data.remote.RetrofitClient
import kotlinx.coroutines.launch

/**
 * 添加好友Activity（完整实现）
 */
class AddFriendActivity : AppCompatActivity() {
    
    private lateinit var etSearch: EditText
    private lateinit var btnSearch: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UserSearchAdapter
    
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
        
        adapter = UserSearchAdapter { user ->
            addFriend(user)
        }
        recyclerView.adapter = adapter
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
                if (response.code == 0 && response.data != null) {
                    adapter.submitList(response.data.users)
                    if (response.data.users.isEmpty()) {
                        Toast.makeText(this@AddFriendActivity, "未找到用户", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@AddFriendActivity, response.message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddFriendActivity, "搜索失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun addFriend(user: User) {
        lifecycleScope.launch {
            try {
                val request = AddContactRequest(contact_id = user.id)
                val response = RetrofitClient.apiService.addContact(request)
                if (response.code == 0) {
                    Toast.makeText(this@AddFriendActivity, "已发送好友申请", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@AddFriendActivity, response.message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddFriendActivity, "添加失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

class UserSearchAdapter(
    private val onAddClick: (User) -> Unit
) : ListAdapter<User, UserSearchAdapter.ViewHolder>(UserDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_search, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onAddClick)
    }
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAvatar: ImageView = itemView.findViewById(R.id.iv_avatar)
        private val tvName: TextView = itemView.findViewById(R.id.tv_name)
        private val btnAdd: Button = itemView.findViewById(R.id.btn_add)
        
        fun bind(user: User, onAddClick: (User) -> Unit) {
            tvName.text = user.username
            
            Glide.with(itemView.context)
                .load(user.avatar)
                .placeholder(R.drawable.ic_profile)
                .circleCrop()
                .into(ivAvatar)
            
            btnAdd.setOnClickListener {
                onAddClick(user)
            }
        }
    }
}

class UserDiffCallback : DiffUtil.ItemCallback<User>() {
    override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
        return oldItem.id == newItem.id
    }
    
    override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
        return oldItem == newItem
    }
}

