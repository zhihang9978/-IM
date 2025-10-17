package com.lanxin.im.ui.settings

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.lanxin.im.R

/**
 * 关于蓝信Activity
 */
class AboutActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        
        findViewById<View>(R.id.btn_back)?.setOnClickListener {
            finish()
        }
    }
}

