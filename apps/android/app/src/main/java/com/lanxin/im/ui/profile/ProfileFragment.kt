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
import com.bumptech.glide.Glide
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
    
    private var cachedUsername: String? = null
    private var cachedLanxinId: String? = null
    private var cachedAvatar: String? = null
    private var isDataLoaded = false
    
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
        if (isDataLoaded && cachedUsername != null) {
            tvUsername.text = cachedUsername
            tvLanxinId.text = "蓝信号: ${cachedLanxinId ?: "未设置"}"
            Glide.with(this@ProfileFragment)
                .load(cachedAvatar ?: R.drawable.ic_profile)
                .circleCrop()
                .placeholder(R.drawable.ic_profile)
                .into(ivAvatar)
            return
        }
        
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getCurrentUser()
                response.data?.let { user ->
                    cachedUsername = user.username ?: "用户"
                    cachedLanxinId = user.lanxinId ?: "未设置"
                    cachedAvatar = user.avatar
                    isDataLoaded = true
                    
                    tvUsername.text = cachedUsername
                    tvLanxinId.text = "蓝信号: $cachedLanxinId"
                    
                    Glide.with(this@ProfileFragment)
                        .load(cachedAvatar ?: R.drawable.ic_profile)
                        .circleCrop()
                        .placeholder(R.drawable.ic_profile)
                        .into(ivAvatar)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (!isDataLoaded) {
                    tvUsername.text = "用户"
                    tvLanxinId.text = "蓝信号: 未登录"
                }
            }
        }
    }
    
    private fun setupClickListeners(view: View) {
        // 收藏（完整实现，跳转到收藏页面）
        view.findViewById<View>(R.id.btn_favorites).setOnClickListener {
            val intent = android.content.Intent(requireContext(), com.lanxin.im.ui.social.FavoritesActivity::class.java)
            startActivity(intent)
        }
        
        // 设置（完整实现，跳转到设置页面）
        view.findViewById<View>(R.id.btn_settings).setOnClickListener {
            val intent = android.content.Intent(requireContext(), com.lanxin.im.ui.settings.SettingsActivity::class.java)
            startActivity(intent)
        }
        
        // 投诉举报（完整实现，跳转到投诉举报页面）
        view.findViewById<View>(R.id.btn_report).setOnClickListener {
            val intent = android.content.Intent(requireContext(), com.lanxin.im.ui.social.ReportActivity::class.java)
            startActivity(intent)
        }
    }
}

