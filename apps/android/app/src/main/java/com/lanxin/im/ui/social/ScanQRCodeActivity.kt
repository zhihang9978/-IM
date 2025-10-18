package com.lanxin.im.ui.social

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.lanxin.im.R

/**
 * 扫一扫Activity
 */
class ScanQRCodeActivity : AppCompatActivity() {
    
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            initCamera()
        } else {
            Toast.makeText(this, "需要相机权限才能使用扫一扫功能", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_scan_qrcode)
            
            findViewById<View>(R.id.btn_close)?.setOnClickListener {
                finish()
            }
            
            checkCameraPermission()
        } catch (e: Exception) {
            Toast.makeText(this, "扫一扫功能初始化失败", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                initCamera()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
    
    private fun initCamera() {
        Toast.makeText(this, "扫一扫功能开发中", Toast.LENGTH_SHORT).show()
    }
}

