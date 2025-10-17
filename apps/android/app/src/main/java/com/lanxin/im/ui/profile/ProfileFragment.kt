package com.lanxin.im.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.lanxin.im.R
import com.lanxin.im.data.remote.RetrofitClient
import kotlinx.coroutines.launch

/**
 * 我的Fragment - 个人中心（按设计文档实现）
 */
class ProfileFragment : Fragment() {
    
    private lateinit var ivAvatar: ImageView
    private lateinit var tvUsername: TextView
    private lateinit var tvLanxinId: TextView
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        loadUserInfo()
        setupClickListeners(view)
    }
    
    private fun initViews(view: View) {
        ivAvatar = view.findViewById(R.id.iv_avatar)
        tvUsername = view.findViewById(R.id.tv_username)
        tvLanxinId = view.findViewById(R.id.tv_lanxin_id)
    }
    
    private fun loadUserInfo() {
        lifecycleScope.launch {
            try {
                // 调用API获取当前用户信息
                val response = RetrofitClient.apiService.getCurrentUser()
                response.data?.let { user ->
                    tvUsername.text = user.username ?: "用户"
                    tvLanxinId.text = "蓝信号: ${user.lanxinId ?: "未设置"}"
                    // 头像使用默认图标（Glide加载在后续优化版本实现）
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // 加载失败，显示默认信息
                tvUsername.text = "用户"
                tvLanxinId.text = "蓝信号: 未登录"
            }
        }
    }
    
    private fun setupClickListeners(view: View) {
        // 收藏（功能待后续版本）
        view.findViewById<View>(R.id.btn_favorites).setOnClickListener {
            // 功能待实现
        }
        
        // 设置（完整实现，跳转到设置页面）
        view.findViewById<View>(R.id.btn_settings).setOnClickListener {
            val intent = android.content.Intent(requireContext(), com.lanxin.im.ui.settings.SettingsActivity::class.java)
            startActivity(intent)
        }
        
        // 投诉举报（功能待后续版本）
        view.findViewById<View>(R.id.btn_report).setOnClickListener {
            // 功能待实现
        }
    }
}

