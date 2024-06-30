package com.u2tzjtne.telephonehelper.ui.activity;

import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.u2tzjtne.telephonehelper.util.StatusBarUtils;


/**
 * @author u2tzjtne@gmail.com
 * @date 2020/6/18
 */
public class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStatusBar();

    }



    private void setStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StatusBarUtils.setTranslucentForImageViewInFragment(this, null);
            StatusBarUtils.setLightStatusBar(this);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        } else {
            StatusBarUtils.setTranslucentForImageViewInFragment(this, null);
        }
    }
}
