package com.lanxin.im.ui.report

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.lanxin.im.R
import com.lanxin.im.data.remote.RetrofitClient
import kotlinx.coroutines.launch

/**
 * 举报Activity
 * 用于举报消息、用户或群组
 */
class ReportActivity : AppCompatActivity() {
    
    private lateinit var radioGroupReportType: RadioGroup
    private lateinit var etReportReason: EditText
    private lateinit var btnSubmitReport: Button
    
    private var targetId: Long = 0
    private var targetType: String = "" // message, user, group
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)
        
        targetId = intent.getLongExtra("target_id", 0)
        targetType = intent.getStringExtra("target_type") ?: "message"
        
        initViews()
        setupListeners()
    }
    
    private fun initViews() {
        radioGroupReportType = findViewById(R.id.radio_group_type)
        etReportReason = findViewById(R.id.et_detail)
        btnSubmitReport = findViewById(R.id.btn_next)
        
        // 显示详情输入框
        findViewById<android.view.View>(R.id.step3_container).visibility = android.view.View.VISIBLE
        
        // 修改按钮文字为"提交"
        btnSubmitReport.text = "提交举报"
        
        supportActionBar?.title = when (targetType) {
            "message" -> "举报消息"
            "user" -> "举报用户"
            "group" -> "举报群组"
            else -> "举报"
        }
    }
    
    private fun setupListeners() {
        btnSubmitReport.setOnClickListener {
            submitReport()
        }
    }
    
    private fun submitReport() {
        val selectedId = radioGroupReportType.checkedRadioButtonId
        if (selectedId == -1) {
            Toast.makeText(this, "请选择举报类型", Toast.LENGTH_SHORT).show()
            return
        }
        
        val reason = etReportReason.text.toString().trim()
        if (reason.isEmpty()) {
            Toast.makeText(this, "请填写举报原因", Toast.LENGTH_SHORT).show()
            return
        }
        
        val reportType = when (selectedId) {
            R.id.radio_spam -> "spam"
            R.id.radio_harassment -> "harassment"
            R.id.radio_fraud -> "fraud"
            else -> "other"
        }
        
        lifecycleScope.launch {
            try {
                val request = mapOf(
                    "target_id" to targetId,
                    "target_type" to targetType,
                    "report_type" to reportType,
                    "reason" to reason
                )
                
                val response = RetrofitClient.apiService.reportMessage(request)
                
                if (response.code == 0) {
                    Toast.makeText(this@ReportActivity, "举报成功", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@ReportActivity, "举报失败：${response.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ReportActivity, "举报失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
