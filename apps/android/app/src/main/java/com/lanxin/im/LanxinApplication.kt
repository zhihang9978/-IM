package com.lanxin.im

import android.app.Application
import com.lanxin.im.utils.AnalyticsHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LanxinApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        AnalyticsHelper.init(this)
        AnalyticsHelper.trackUserActive(this)
    }
    
    companion object {
        lateinit var instance: LanxinApplication
            private set
    }
}

