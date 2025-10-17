package com.lanxin.im.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.lanxin.im.MainActivity
import com.lanxin.im.R
import com.lanxin.im.data.remote.LoginRequest
import com.lanxin.im.data.remote.RetrofitClient
import kotlinx.coroutines.launch

/**
 * 登录Activity（必须实现，否则用户无法登录）
 */
class LoginActivity : AppCompatActivity() {
    
    private lateinit var etIdentifier: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        initViews()
        setupListeners()
    }
    
    private fun initViews() {
        etIdentifier = findViewById(R.id.et_identifier)
        etPassword = findViewById(R.id.et_password)
        btnLogin = findViewById(R.id.btn_login)
    }
    
    private fun setupListeners() {
        btnLogin.setOnClickListener {
            val identifier = etIdentifier.text.toString().trim()
            val password = etPassword.text.toString().trim()
            
            if (identifier.isEmpty()) {
                Toast.makeText(this, "请输入账号/手机号/邮箱", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (password.isEmpty()) {
                Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            performLogin(identifier, password)
        }
    }
    
    private fun performLogin(identifier: String, password: String) {
        btnLogin.isEnabled = false
        btnLogin.text = "登录中..."
        
        lifecycleScope.launch {
            try {
                val request = LoginRequest(identifier, password)
                val response = RetrofitClient.apiService.login(request)
                
                if (response.code == 0 && response.data != null) {
                    // 保存Token
                    RetrofitClient.setToken(response.data.token)
                    
                    // TODO: 保存Token到SharedPreferences持久化
                    
                    Toast.makeText(this@LoginActivity, "登录成功", Toast.LENGTH_SHORT).show()
                    
                    // 跳转到主界面
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, response.message, Toast.LENGTH_SHORT).show()
                    btnLogin.isEnabled = true
                    btnLogin.text = "登录"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@LoginActivity, "登录失败: ${e.message}", Toast.LENGTH_SHORT).show()
                btnLogin.isEnabled = true
                btnLogin.text = "登录"
            }
        }
    }
}

