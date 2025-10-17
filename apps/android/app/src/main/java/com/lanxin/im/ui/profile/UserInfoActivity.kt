package com.lanxin.im.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.lanxin.im.R
import com.lanxin.im.data.remote.RetrofitClient
import com.lanxin.im.ui.chat.ChatActivity
import com.lanxin.im.ui.video.VideoCallActivity
import kotlinx.coroutines.launch

/**
 * 用户信息页面Activity (WildFire IM style)
 * 参考：WildFireChat UserInfoFragment (Apache 2.0)
 * 适配：蓝信IM
 * 
 * 功能:
 * - 大头像显示 (80dp)
 * - 基本信息展示 (昵称、ID、电话)
 * - OptionItem列表
 * - 开关状态管理
 * - 底部操作按钮
 */
class UserInfoActivity : AppCompatActivity() {
    
    private lateinit var avatarImageView: ImageView
    private lateinit var nameTextView: TextView
    private lateinit var userIdTextView: TextView
    private lateinit var phoneTextView: TextView
    private lateinit var qrcodeImageView: ImageView
    
    private lateinit var btnSendMessage: Button
    private lateinit var btnVideoCall: Button
    
    private var userId: Long = 0
    private var username: String = ""
    
    // OptionItem switches
    private lateinit var starFriendSwitch: SwitchCompat
    private lateinit var muteSwitch: SwitchCompat
    private lateinit var topChatSwitch: SwitchCompat
    private lateinit var blacklistSwitch: SwitchCompat
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_user_info_wildfire)
        
        userId = intent.getLongExtra("user_id", 0)
        username = intent.getStringExtra("username") ?: ""
        
        setupUI()
        setupOptionItems()
        loadUserInfo()
    }
    
    /**
     * Activity过渡动画 (WildFire IM style)
     */
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
    
    private fun setupUI() {
        // 基本信息
        avatarImageView = findViewById(R.id.avatarImageView)
        nameTextView = findViewById(R.id.nameTextView)
        userIdTextView = findViewById(R.id.userIdTextView)
        phoneTextView = findViewById(R.id.phoneTextView)
        qrcodeImageView = findViewById(R.id.qrcodeImageView)
        
        // 底部按钮
        btnSendMessage = findViewById(R.id.btnSendMessage)
        btnVideoCall = findViewById(R.id.btnVideoCall)
        
        // 二维码按钮点击
        qrcodeImageView.setOnClickListener {
            Toast.makeText(this, "二维码功能：待实现", Toast.LENGTH_SHORT).show()
        }
        
        // 发送消息按钮
        btnSendMessage.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("peer_id", userId)
            intent.putExtra("peer_name", username)
            startActivity(intent)
        }
        
        // 视频通话按钮
        btnVideoCall.setOnClickListener {
            val intent = Intent(this, VideoCallActivity::class.java)
            intent.putExtra("peer_id", userId)
            intent.putExtra("peer_name", username)
            startActivity(intent)
        }
    }
    
    /**
     * 设置OptionItem功能
     * 参考：WildFireChat UserInfoFragment (Apache 2.0)
     */
    private fun setupOptionItems() {
        // 设置备注和标签
        findViewById<View>(R.id.option_remark).apply {
            findViewById<TextView>(R.id.titleTextView).text = "设置备注和标签"
            findViewById<ImageView>(R.id.rightArrowImageView).visibility = View.VISIBLE
            setOnClickListener {
                Toast.makeText(this@UserInfoActivity, "设置备注：待实现", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 查看消息
        findViewById<View>(R.id.option_view_messages).apply {
            findViewById<TextView>(R.id.titleTextView).text = "查看消息"
            findViewById<ImageView>(R.id.rightArrowImageView).visibility = View.VISIBLE
            setOnClickListener {
                val intent = Intent(this@UserInfoActivity, ChatActivity::class.java)
                intent.putExtra("peer_id", userId)
                startActivity(intent)
            }
        }
        
        // 星标好友开关
        findViewById<View>(R.id.option_star_friend).apply {
            findViewById<TextView>(R.id.titleTextView).text = "星标好友"
            starFriendSwitch = findViewById(R.id.switchButton)
            starFriendSwitch.visibility = View.VISIBLE
            starFriendSwitch.setOnCheckedChangeListener { _, isChecked ->
                updateStarFriendStatus(isChecked)
            }
        }
        
        // 消息免打扰开关
        findViewById<View>(R.id.option_mute).apply {
            findViewById<TextView>(R.id.titleTextView).text = "消息免打扰"
            muteSwitch = findViewById(R.id.switchButton)
            muteSwitch.visibility = View.VISIBLE
            muteSwitch.setOnCheckedChangeListener { _, isChecked ->
                updateMuteStatus(isChecked)
            }
        }
        
        // 置顶聊天开关
        findViewById<View>(R.id.option_top_chat).apply {
            findViewById<TextView>(R.id.titleTextView).text = "置顶聊天"
            topChatSwitch = findViewById(R.id.switchButton)
            topChatSwitch.visibility = View.VISIBLE
            topChatSwitch.setOnCheckedChangeListener { _, isChecked ->
                updateTopChatStatus(isChecked)
            }
        }
        
        // 加入黑名单开关
        findViewById<View>(R.id.option_blacklist).apply {
            findViewById<TextView>(R.id.titleTextView).text = "加入黑名单"
            blacklistSwitch = findViewById(R.id.switchButton)
            blacklistSwitch.visibility = View.VISIBLE
            blacklistSwitch.setOnCheckedChangeListener { _, isChecked ->
                updateBlacklistStatus(isChecked)
            }
        }
    }
    
    /**
     * 加载用户信息
     */
    private fun loadUserInfo() {
        lifecycleScope.launch {
            try {
                // TODO: 调用API获取用户详细信息
                // 暂时使用传入的数据
                nameTextView.text = username
                userIdTextView.text = "蓝信号：$userId"
                
                // 加载头像
                Glide.with(this@UserInfoActivity)
                    .load("") // TODO: 用户头像URL
                    .circleCrop()
                    .placeholder(R.drawable.ic_profile)
                    .into(avatarImageView)
                    
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // OptionItem 功能实现
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * 更新星标好友状态
     */
    private fun updateStarFriendStatus(isStarred: Boolean) {
        lifecycleScope.launch {
            try {
                // TODO: 调用API更新星标状态
                val status = if (isStarred) "已设为星标好友" else "已取消星标"
                Toast.makeText(this@UserInfoActivity, status, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@UserInfoActivity, "操作失败", Toast.LENGTH_SHORT).show()
                // 恢复原状态
                starFriendSwitch.isChecked = !isStarred
            }
        }
    }
    
    /**
     * 更新免打扰状态
     */
    private fun updateMuteStatus(isMuted: Boolean) {
        lifecycleScope.launch {
            try {
                // TODO: 调用API更新免打扰状态
                val status = if (isMuted) "已开启免打扰" else "已关闭免打扰"
                Toast.makeText(this@UserInfoActivity, status, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@UserInfoActivity, "操作失败", Toast.LENGTH_SHORT).show()
                muteSwitch.isChecked = !isMuted
            }
        }
    }
    
    /**
     * 更新置顶聊天状态
     */
    private fun updateTopChatStatus(isTop: Boolean) {
        lifecycleScope.launch {
            try {
                // TODO: 调用API更新置顶状态
                val status = if (isTop) "已置顶聊天" else "已取消置顶"
                Toast.makeText(this@UserInfoActivity, status, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@UserInfoActivity, "操作失败", Toast.LENGTH_SHORT).show()
                topChatSwitch.isChecked = !isTop
            }
        }
    }
    
    /**
     * 更新黑名单状态
     */
    private fun updateBlacklistStatus(isBlacklisted: Boolean) {
        lifecycleScope.launch {
            try {
                // TODO: 调用API更新黑名单状态
                val status = if (isBlacklisted) "已加入黑名单" else "已移出黑名单"
                Toast.makeText(this@UserInfoActivity, status, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@UserInfoActivity, "操作失败", Toast.LENGTH_SHORT).show()
                blacklistSwitch.isChecked = !isBlacklisted
            }
        }
    }
}

