package com.u2tzjtne.telephonehelper.ui.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.XXPermissions;
import com.hjq.permissions.permission.PermissionLists;
import com.u2tzjtne.telephonehelper.util.StatusBarUtils;
import com.u2tzjtne.telephonehelper.util.ThemeManager;
import com.u2tzjtne.telephonehelper.util.ToastUtils;
import com.zackratos.ultimatebarx.ultimatebarx.java.UltimateBarX;

import java.util.List;

/**
 * @author u2tzjtne@gmail.com
 * @date 2020/6/18
 */
public class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setStatusBar();

    }

    @Override
    protected void onResume() {
        super.onResume();
        setStatusBar();
    }

    protected void requestAppPermissionsIfNeeded() {
        if (hasAppPermissions()) {
            return;
        }
        XXPermissions.with(this)
                .permission(PermissionLists.getRecordAudioPermission())
                .permission(PermissionLists.getReadExternalStoragePermission())
                .request(new OnPermissionCallback() {
                    @Override
                    public void onResult(List grantedList, List deniedList) {
                        if (deniedList == null || deniedList.isEmpty()) {
                            return;
                        }
                        boolean doNotAskAgain = XXPermissions.isDoNotAskAgainPermissions(BaseActivity.this, deniedList);
                        if (doNotAskAgain) {
                            ToastUtils.s("请在系统设置中开启录音和存储权限");
                            return;
                        }
                        ToastUtils.s("部分功能需要录音和存储权限才能正常使用");
                    }
                });
    }

    protected boolean hasAppPermissions() {
        return hasPermission(Manifest.permission.RECORD_AUDIO)
                && hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    protected boolean hasRecordAudioPermission() {
        return hasPermission(Manifest.permission.RECORD_AUDIO);
    }

    protected boolean hasReadStoragePermission() {
        return hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    protected void openAppPermissionSettings() {
        XXPermissions.startPermissionActivity(this);
    }

    protected boolean hasPermission(String permission) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void setStatusBar() {
        // 根据当前主题模式设置状态栏文字颜色
        // 白天模式用黑色文字，黑夜模式用白色文字
//        if (isDarkMode()) {
//            UltimateBarX.statusBarOnly(this).fitWindow(false).light(false).apply();
//        } else {
//            UltimateBarX.statusBarOnly(this).light(false).apply();
//        }
    }

    /**
     * 判断当前是否为黑夜模式
     * 同时考虑用户设置和系统配置
     */
    private boolean isDarkMode() {
        int currentMode = ThemeManager.getCurrentMode();
        if (currentMode == ThemeManager.MODE_DARK) {
            return true;
        } else if (currentMode == ThemeManager.MODE_LIGHT) {
            return false;
        } else {
            // 跟随系统模式，检查系统当前配置
            int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
        }
    }
}
