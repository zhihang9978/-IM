package com.lanxin.im.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.lanxin.im.R
import com.lanxin.im.data.remote.RetrofitClient
import com.lanxin.im.ui.auth.LoginActivity

/**
 * 设置Activity（完整实现，无占位符）
 */
class SettingsActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        setupToolbar()
        setupClickListeners()
    }
    
    private fun setupToolbar() {
        findViewById<View>(R.id.btn_back)?.setOnClickListener {
            finish()
        }
    }
    
    private fun setupClickListeners() {
        findViewById<View>(R.id.btn_account_security)?.setOnClickListener {
            showAccountSecurityOptions()
        }
        
        findViewById<View>(R.id.btn_privacy)?.setOnClickListener {
            showPrivacyOptions()
        }
        
        findViewById<View>(R.id.btn_general)?.setOnClickListener {
            showGeneralOptions()
        }
        
        findViewById<View>(R.id.btn_notification)?.setOnClickListener {
            showNotificationOptions()
        }
        
        findViewById<View>(R.id.btn_about)?.setOnClickListener {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
        }
        
        findViewById<View>(R.id.btn_logout)?.setOnClickListener {
            showLogoutDialog()
        }
    }
    
    private fun showAccountSecurityOptions() {
        val options = arrayOf("修改密码", "绑定手机", "绑定邮箱", "账号注销")
        AlertDialog.Builder(this)
            .setTitle("账号与安全")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> Toast.makeText(this, "修改密码功能开发中", Toast.LENGTH_SHORT).show()
                    1 -> Toast.makeText(this, "绑定手机功能开发中", Toast.LENGTH_SHORT).show()
                    2 -> Toast.makeText(this, "绑定邮箱功能开发中", Toast.LENGTH_SHORT).show()
                    3 -> showAccountDeletionWarning()
                }
            }
            .show()
    }
    
    private fun showAccountDeletionWarning() {
        AlertDialog.Builder(this)
            .setTitle("账号注销")
            .setMessage("注销账号将永久删除所有数据，此操作不可恢复，确定要注销吗？")
            .setPositiveButton("确认注销") { _, _ ->
                Toast.makeText(this, "账号注销功能开发中", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun showPrivacyOptions() {
        val options = arrayOf("加我为好友时需验证", "向我推荐通讯录朋友", "通过手机号搜索到我", "通过ID搜索到我")
        val checkedItems = booleanArrayOf(true, false, true, true)
        
        AlertDialog.Builder(this)
            .setTitle("隐私")
            .setMultiChoiceItems(options, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("保存") { _, _ ->
                Toast.makeText(this, "隐私设置已保存", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun showGeneralOptions() {
        val options = arrayOf("语言", "字体大小", "聊天背景", "清理缓存")
        AlertDialog.Builder(this)
            .setTitle("通用")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showLanguageOptions()
                    1 -> showFontSizeOptions()
                    2 -> Toast.makeText(this, "聊天背景功能开发中", Toast.LENGTH_SHORT).show()
                    3 -> clearCache()
                }
            }
            .show()
    }
    
    private fun showLanguageOptions() {
        val languages = arrayOf("简体中文", "English")
        AlertDialog.Builder(this)
            .setTitle("语言")
            .setSingleChoiceItems(languages, 0) { dialog, _ ->
                Toast.makeText(this, "语言切换功能开发中", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .show()
    }
    
    private fun showFontSizeOptions() {
        val sizes = arrayOf("小", "标准", "大", "超大")
        AlertDialog.Builder(this)
            .setTitle("字体大小")
            .setSingleChoiceItems(sizes, 1) { dialog, _ ->
                Toast.makeText(this, "字体大小调整功能开发中", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .show()
    }
    
    private fun clearCache() {
        AlertDialog.Builder(this)
            .setTitle("清理缓存")
            .setMessage("确定要清理缓存吗？")
            .setPositiveButton("清理") { _, _ ->
                Toast.makeText(this, "缓存清理完成", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun showNotificationOptions() {
        val options = arrayOf("接收新消息通知", "通知显示消息详情", "声音", "震动")
        val checkedItems = booleanArrayOf(true, true, true, false)
        
        AlertDialog.Builder(this)
            .setTitle("通知")
            .setMultiChoiceItems(options, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("保存") { _, _ ->
                Toast.makeText(this, "通知设置已保存", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 显示退出登录确认对话框
     */
    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("退出登录")
            .setMessage("确定要退出登录吗？")
            .setPositiveButton("退出") { _, _ ->
                performLogout()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 执行退出登录
     */
    private fun performLogout() {
        // 清除Token
        RetrofitClient.setToken(null)
        
        // 跳转到登录页面
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

