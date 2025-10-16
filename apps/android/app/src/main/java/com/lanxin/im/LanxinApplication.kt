package com.lanxin.im

import android.app.Application

class LanxinApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // TODO: 初始化各种SDK和库
        // 初始化数据库
        // 初始化网络层
        // 初始化TRTC SDK
    }
    
    companion object {
        lateinit var instance: LanxinApplication
            private set
    }
}

