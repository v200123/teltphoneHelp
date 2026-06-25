package com.example.myservicecenter

import android.app.Application
import android.webkit.WebView

class MyServiceCenterApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // 开启 WebView 远程调试，方便在 chrome://inspect 里查看控制台日志
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }
}
