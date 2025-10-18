package com.lanxin.im.ui.social

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lanxin.im.R

/**
 * 投诉举报Activity（简化版）
 */
class ReportActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)
        
        setupViews()
    }
    
    private fun setupViews() {
        findViewById<View>(R.id.btn_back)?.setOnClickListener {
            finish()
        }
    }
}

