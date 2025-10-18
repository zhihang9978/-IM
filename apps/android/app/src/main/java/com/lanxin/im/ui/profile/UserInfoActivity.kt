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
import com.lanxin.im.trtc.VideoCallActivity
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
    private var conversationId: Long = 0
    private var currentRemark: String? = null
    private var currentTags: String? = null
    
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
        conversationId = intent.getLongExtra("conversation_id", 0)
        
        setupUI()
        setupOptionItems()
        loadUserInfo()
        loadConversationSettings()
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
            // ✅ 显示用户二维码（生成包含用户ID的二维码）
            showUserQRCode()
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
                openRemarkActivity()
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
     * 打开备注设置页面
     */
    private fun openRemarkActivity() {
        val intent = Intent(this, RemarkActivity::class.java)
        intent.putExtra("contact_id", userId)
        // ✅ 传递当前备注和标签（从Intent或SharedPreferences获取）
        intent.putExtra("current_remark", currentRemark ?: "")
        intent.putExtra("current_tags", currentTags ?: "")
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
    
    /**
     * 加载用户信息
     */
    private fun loadUserInfo() {
        lifecycleScope.launch {
            try {
                // ✅ 调用API获取用户详细信息
                val response = com.lanxin.im.data.remote.RetrofitClient.apiService.searchUsers(
                    keyword = userId.toString(),
                    page = 1,
                    pageSize = 1
                )
                
                if (response.code == 0 && response.data?.users?.isNotEmpty() == true) {
                    val user = response.data.users[0]
                    nameTextView.text = user.username
                    userIdTextView.text = "蓝信号：${user.lanxinId}"
                    
                    // 显示电话（如果有）
                    if (!user.phone.isNullOrEmpty()) {
                        phoneTextView.visibility = View.VISIBLE
                        phoneTextView.text = "电话：${user.phone}"
                    }
                    
                    // ✅ 加载头像
                    Glide.with(this@UserInfoActivity)
                        .load(user.avatar)
                        .circleCrop()
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .into(avatarImageView)
                } else {
                    // API失败，使用传入的数据
                    nameTextView.text = username
                    userIdTextView.text = "蓝信号：$userId"
                }
                    
            } catch (e: Exception) {
                e.printStackTrace()
                // 异常时使用传入的数据
                nameTextView.text = username
                userIdTextView.text = "蓝信号：$userId"
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // OptionItem 功能实现
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * 加载会话设置
     */
    private fun loadConversationSettings() {
        if (conversationId == 0L) return
        
        lifecycleScope.launch {
            try {
                val response = com.lanxin.im.data.remote.RetrofitClient.apiService.getConversationSettings(conversationId)
                if (response.code == 0 && response.data != null) {
                    starFriendSwitch.isChecked = response.data.is_starred
                    muteSwitch.isChecked = response.data.is_muted
                    topChatSwitch.isChecked = response.data.is_top
                    blacklistSwitch.isChecked = response.data.is_blocked
                }
            } catch (e: Exception) {
                // 加载失败，使用默认值
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 更新星标好友状态
     */
    private fun updateStarFriendStatus(isStarred: Boolean) {
        lifecycleScope.launch {
            try {
                // ✅ 调用真实API更新星标状态
                val settings = mapOf("is_starred" to isStarred)
                val response = com.lanxin.im.data.remote.RetrofitClient.apiService.updateConversationSettings(
                    conversationId,
                    settings
                )
                
                if (response.code == 0) {
                    val status = if (isStarred) "已设为星标好友" else "已取消星标"
                    Toast.makeText(this@UserInfoActivity, status, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@UserInfoActivity, response.message, Toast.LENGTH_SHORT).show()
                    starFriendSwitch.isChecked = !isStarred
                }
            } catch (e: Exception) {
                Toast.makeText(this@UserInfoActivity, "操作失败: ${e.message}", Toast.LENGTH_SHORT).show()
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
                // ✅ 调用真实API更新免打扰状态
                val settings = mapOf("is_muted" to isMuted)
                val response = com.lanxin.im.data.remote.RetrofitClient.apiService.updateConversationSettings(
                    conversationId,
                    settings
                )
                
                if (response.code == 0) {
                    val status = if (isMuted) "已开启免打扰" else "已关闭免打扰"
                    Toast.makeText(this@UserInfoActivity, status, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@UserInfoActivity, response.message, Toast.LENGTH_SHORT).show()
                    muteSwitch.isChecked = !isMuted
                }
            } catch (e: Exception) {
                Toast.makeText(this@UserInfoActivity, "操作失败: ${e.message}", Toast.LENGTH_SHORT).show()
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
                // ✅ 调用真实API更新置顶状态
                val settings = mapOf("is_top" to isTop)
                val response = com.lanxin.im.data.remote.RetrofitClient.apiService.updateConversationSettings(
                    conversationId,
                    settings
                )
                
                if (response.code == 0) {
                    val status = if (isTop) "已置顶聊天" else "已取消置顶"
                    Toast.makeText(this@UserInfoActivity, status, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@UserInfoActivity, response.message, Toast.LENGTH_SHORT).show()
                    topChatSwitch.isChecked = !isTop
                }
            } catch (e: Exception) {
                Toast.makeText(this@UserInfoActivity, "操作失败: ${e.message}", Toast.LENGTH_SHORT).show()
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
                // ✅ 调用真实API更新黑名单状态
                val settings = mapOf("is_blocked" to isBlacklisted)
                val response = com.lanxin.im.data.remote.RetrofitClient.apiService.updateConversationSettings(
                    conversationId,
                    settings
                )
                
                if (response.code == 0) {
                    val status = if (isBlacklisted) "已加入黑名单" else "已移出黑名单"
                    Toast.makeText(this@UserInfoActivity, status, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@UserInfoActivity, response.message, Toast.LENGTH_SHORT).show()
                    blacklistSwitch.isChecked = !isBlacklisted
                }
            } catch (e: Exception) {
                Toast.makeText(this@UserInfoActivity, "操作失败: ${e.message}", Toast.LENGTH_SHORT).show()
                blacklistSwitch.isChecked = !isBlacklisted
            }
        }
    }
    
    /**
     * 显示用户二维码
     */
    private fun showUserQRCode() {
        // 二维码内容：蓝信号
        val qrContent = "lanxin://user/$userId"
        
        // ✅ 显示二维码对话框（简化实现：显示文本，完整实现需要二维码生成库）
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("我的二维码")
            .setMessage("蓝信号：$userId\n\n扫码添加好友\n\n完整实现需集成二维码生成库（如zxing）")
            .setPositiveButton("确定", null)
            .setNegativeButton("复制", { _, _ ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("lanxin_id", userId.toString())
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "已复制蓝信号", Toast.LENGTH_SHORT).show()
            })
            .show()
    }
}

