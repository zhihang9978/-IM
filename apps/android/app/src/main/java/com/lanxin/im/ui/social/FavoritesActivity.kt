package com.lanxin.im.ui.social

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.lanxin.im.R

/**
 * 收藏Activity
 */
class FavoritesActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)
        
        findViewById<View>(R.id.btn_back)?.setOnClickListener {
            finish()
        }
    }
}

