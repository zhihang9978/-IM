package com.lanxin.im.ui.media

import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.VideoView
import android.widget.MediaController
import androidx.appcompat.app.AppCompatActivity
import com.lanxin.im.R

/**
 * 视频播放Activity
 */
class VideoPlayerActivity : AppCompatActivity() {
    
    private lateinit var videoView: VideoView
    private lateinit var btnBack: ImageButton
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)
        
        videoView = findViewById(R.id.video_view)
        btnBack = findViewById(R.id.btn_back)
        
        val videoUrl = intent.getStringExtra("video_url")
        if (videoUrl != null) {
            val mediaController = MediaController(this)
            mediaController.setAnchorView(videoView)
            videoView.setMediaController(mediaController)
            videoView.setVideoURI(Uri.parse(videoUrl))
            videoView.start()
        }
        
        btnBack.setOnClickListener {
            finish()
        }
    }
    
    override fun onPause() {
        super.onPause()
        videoView.pause()
    }
}

