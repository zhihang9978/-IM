package com.lanxin.im.ui.profile

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.lanxin.im.R
import kotlinx.coroutines.launch

/**
 * 设置备注和标签Activity
 * 参考：WildFireChat remark功能 (Apache 2.0)
 * 适配：蓝信IM
 */
class RemarkActivity : AppCompatActivity() {
    
    private lateinit var etRemark: EditText
    private lateinit var etTags: EditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    
    private var contactId: Long = 0
    private var currentRemark: String? = null
    private var currentTags: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_remark)
        
        contactId = intent.getLongExtra("contact_id", 0)
        currentRemark = intent.getStringExtra("current_remark")
        currentTags = intent.getStringExtra("current_tags")
        
        setupUI()
    }
    
    private fun setupUI() {
        etRemark = findViewById(R.id.et_remark)
        etTags = findViewById(R.id.et_tags)
        btnSave = findViewById(R.id.btn_save)
        btnCancel = findViewById(R.id.btn_cancel)
        
        // 设置当前值
        etRemark.setText(currentRemark ?: "")
        etTags.setText(currentTags ?: "")
        
        // 保存按钮
        btnSave.setOnClickListener {
            saveRemark()
        }
        
        // 取消按钮
        btnCancel.setOnClickListener {
            finish()
        }
    }
    
    /**
     * 保存备注和标签
     * 参考：WildFireChat setFriendAlias (Apache 2.0)
     */
    private fun saveRemark() {
        val remark = etRemark.text.toString().trim()
        val tags = etTags.text.toString().trim()
        
        lifecycleScope.launch {
            try {
                // ✅ 调用真实API更新备注
                val request = mapOf(
                    "remark" to remark,
                    "tags" to tags
                )
                
                val response = com.lanxin.im.data.remote.RetrofitClient.apiService.updateContactRemark(
                    contactId,
                    request
                )
                
                if (response.code == 0) {
                    Toast.makeText(this@RemarkActivity, "备注已保存", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@RemarkActivity, "保存失败: ${response.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@RemarkActivity, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}

