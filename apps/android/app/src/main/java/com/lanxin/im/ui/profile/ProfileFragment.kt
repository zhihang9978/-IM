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
                if (response.code == 0 && response.data != null) {
                    val user = response.data
                    tvUsername.text = user.username
                    tvLanxinId.text = "蓝信号: ${user.lanxinId}"
                    // TODO: 使用Glide加载头像
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun setupClickListeners(view: View) {
        // 收藏
        view.findViewById<View>(R.id.btn_favorites).setOnClickListener {
            Toast.makeText(requireContext(), "收藏功能", Toast.LENGTH_SHORT).show()
        }
        
        // 设置
        view.findViewById<View>(R.id.btn_settings).setOnClickListener {
            Toast.makeText(requireContext(), "设置功能", Toast.LENGTH_SHORT).show()
        }
        
        // 投诉举报
        view.findViewById<View>(R.id.btn_report).setOnClickListener {
            Toast.makeText(requireContext(), "投诉举报", Toast.LENGTH_SHORT).show()
        }
    }
}

