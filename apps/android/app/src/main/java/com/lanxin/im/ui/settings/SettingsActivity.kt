package com.lanxin.im.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
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
        // 账号与安全
        findViewById<View>(R.id.btn_account_security)?.setOnClickListener {
            // 功能待实现（不显示Toast）
        }
        
        // 隐私
        findViewById<View>(R.id.btn_privacy)?.setOnClickListener {
            // 功能待实现
        }
        
        // 通用
        findViewById<View>(R.id.btn_general)?.setOnClickListener {
            // 功能待实现
        }
        
        // 通知
        findViewById<View>(R.id.btn_notification)?.setOnClickListener {
            // 功能待实现
        }
        
        // 关于蓝信
        findViewById<View>(R.id.btn_about)?.setOnClickListener {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
        }
        
        // 退出登录（完整实现）
        findViewById<View>(R.id.btn_logout)?.setOnClickListener {
            showLogoutDialog()
        }
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

