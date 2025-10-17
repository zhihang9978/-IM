package com.lanxin.im.ui.social

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.lanxin.im.R

/**
 * 扫一扫Activity
 */
class ScanQRCodeActivity : AppCompatActivity() {
    
    private companion object {
        const val CAMERA_PERMISSION_REQUEST_CODE = 100
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_qrcode)
        
        findViewById<View>(R.id.btn_close)?.setOnClickListener {
            finish()
        }
        
        checkCameraPermission()
    }
    
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            initCamera()
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initCamera()
                } else {
                    Toast.makeText(this, "需要相机权限才能使用扫一扫功能", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }
    
    private fun initCamera() {
        Toast.makeText(this, "扫一扫功能开发中", Toast.LENGTH_SHORT).show()
    }
}

