package com.u2tzjtne.telephonehelper.base;

import android.app.Application;
import android.content.Context;

import com.wenming.library.LogReport;
import com.wenming.library.save.imp.CrashWriter;

/**
 * @author u2tzjtne@gmail.com
 * @date 2020/6/3
 */
public class App extends Application {
    private static App instance;
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        if (instance == null) {
            instance = this;
            context = getApplicationContext();
        }
        initCrashReport();
    }

    public static App getInstance() {
        return instance;
    }

    public static Context getContext() {
        return context;
    }

    private void initCrashReport() {
        LogReport.getInstance()
                .setCacheSize(30 * 1024 * 1024)//支持设置缓存大小，超出后清空
                .setLogSaver(new CrashWriter(getApplicationContext()))//支持自定义保存崩溃信息的样式
                .init(getApplicationContext());
    }
}
