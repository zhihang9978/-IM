package com.lanxin.im.ui.social

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lanxin.im.R

/**
 * 投诉举报Activity（简化版本）
 */
class ReportActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)
        
        findViewById<android.view.View>(R.id.btn_back)?.setOnClickListener {
            finish()
        }
        
        Toast.makeText(this, "投诉举报功能开发中", Toast.LENGTH_SHORT).show()
    }
}

