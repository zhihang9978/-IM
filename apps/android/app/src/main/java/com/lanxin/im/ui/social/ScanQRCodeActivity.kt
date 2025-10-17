package com.lanxin.im.ui.social

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.lanxin.im.R

/**
 * 扫一扫Activity
 */
class ScanQRCodeActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_qrcode)
        
        findViewById<View>(R.id.btn_close)?.setOnClickListener {
            finish()
        }
    }
}

