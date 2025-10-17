package com.lanxin.im.ui.social

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.lanxin.im.R

/**
 * 投诉举报Activity（三步骤流程）
 */
class ReportActivity : AppCompatActivity() {
    
    private var currentStep = 1
    private var selectedIssueType = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)
        
        setupViews()
        showStep(1)
    }
    
    private fun setupViews() {
        findViewById<View>(R.id.btn_back)?.setOnClickListener {
            finish()
        }
    }
    
    private fun showStep(step: Int) {
        currentStep = step
        // 显示对应步骤的UI
        // 功能待完善：三步骤切换逻辑
    }
}

