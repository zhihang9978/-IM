package com.lanxin.im.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.lanxin.im.R
import com.lanxin.im.data.remote.RetrofitClient
import com.lanxin.im.widget.OptionItemView
import kotlinx.coroutines.launch

/**
 * 我的Fragment - 野火IM风格UI
 */
class ProfileFragment : Fragment() {
    
    private lateinit var portraitImageView: ImageView
    private lateinit var nameTextView: TextView
    private lateinit var accountTextView: TextView
    private lateinit var meLinearLayout: LinearLayout
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile_new, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        loadUserInfo()
        setupClickListeners(view)
    }
    
    private fun initViews(view: View) {
        portraitImageView = view.findViewById(R.id.portraitImageView)
        nameTextView = view.findViewById(R.id.nameTextView)
        accountTextView = view.findViewById(R.id.accountTextView)
        meLinearLayout = view.findViewById(R.id.meLinearLayout)
    }
    
    private fun loadUserInfo() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getCurrentUser()
                response.data?.let { user ->
                    nameTextView.text = user.username ?: "用户"
                    accountTextView.text = "蓝信号: ${user.lanxinId ?: "未设置"}"
                    
                    Glide.with(this@ProfileFragment)
                        .load(user.avatar ?: R.mipmap.ic_launcher)
                        .circleCrop()
                        .placeholder(R.mipmap.ic_launcher)
                        .into(portraitImageView)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                nameTextView.text = "用户"
                accountTextView.text = "蓝信号: 未登录"
            }
        }
    }
    
    private fun setupClickListeners(view: View) {
        // 点击用户信息卡片
        meLinearLayout.setOnClickListener {
            val intent = Intent(requireContext(), UserInfoActivity::class.java)
            startActivity(intent)
        }
        
        // 账号安全
        view.findViewById<OptionItemView>(R.id.accountOptionItemView).setOnClickListener {
            // TODO: 跳转到账号安全页面
        }
        
        // 消息通知
        view.findViewById<OptionItemView>(R.id.notificationOptionItemView).setOnClickListener {
            // TODO: 跳转到消息通知设置
        }
        
        // 文件
        view.findViewById<OptionItemView>(R.id.fileRecordOptionItemView).setOnClickListener {
            // TODO: 跳转到文件管理
        }
        
        // 收藏
        view.findViewById<OptionItemView>(R.id.favOptionItemView).setOnClickListener {
            val intent = Intent(requireContext(), com.lanxin.im.ui.social.FavoritesActivity::class.java)
            startActivity(intent)
        }
        
        // 设置
        view.findViewById<OptionItemView>(R.id.settingOptionItemView).setOnClickListener {
            val intent = Intent(requireContext(), com.lanxin.im.ui.settings.SettingsActivity::class.java)
            startActivity(intent)
        }
    }
}

