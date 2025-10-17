package com.lanxin.im.ui.media

import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.lanxin.im.R

/**
 * 图片预览Activity
 */
class ImagePreviewActivity : AppCompatActivity() {
    
    private lateinit var ivImage: ImageView
    private lateinit var btnBack: ImageButton
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_preview)
        
        ivImage = findViewById(R.id.iv_image)
        btnBack = findViewById(R.id.btn_back)
        
        val imageUrl = intent.getStringExtra("image_url")
        if (imageUrl != null) {
            Glide.with(this)
                .load(imageUrl)
                .into(ivImage)
        }
        
        btnBack.setOnClickListener {
            finish()
        }
    }
}

