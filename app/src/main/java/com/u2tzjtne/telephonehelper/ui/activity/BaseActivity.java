package com.u2tzjtne.telephonehelper.ui.activity;

import android.Manifest;
import android.content.pm.PackageManager;
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
import com.u2tzjtne.telephonehelper.util.ToastUtils;

import java.util.List;

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StatusBarUtils.setTranslucentForImageViewInFragment(this, null);
            StatusBarUtils.setLightStatusBar(this);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        } else {
            StatusBarUtils.setTranslucentForImageViewInFragment(this, null);
        }
    }
}
