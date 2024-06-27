package com.u2tzjtne.telephonehelper.util;

import android.annotation.SuppressLint;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.u2tzjtne.telephonehelper.base.App;

/**
 * 用单例模式,解决toast重复弹出的问题
 *
 * @author u2tzjtne@gmail.com
 */
public class ToastUtils {
    private static Toast mToast;

    @SuppressLint("ShowToast")
    public static void s(@NonNull String mString) {
        if (mToast == null) {
            mToast = Toast.makeText(App.getInstance(), mString, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(mString);
            mToast.setDuration(Toast.LENGTH_SHORT);
        }
        mToast.show();
    }

    @SuppressLint("ShowToast")
    public static void s(int messageId) {
        if (mToast == null) {
            mToast = Toast.makeText(App.getInstance(), messageId, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(messageId);
            mToast.setDuration(Toast.LENGTH_SHORT);
        }
        mToast.show();
    }

    @SuppressLint("ShowToast")
    public static void l(@NonNull String mString) {
        if (mToast == null) {
            mToast = Toast.makeText(App.getInstance(), mString, Toast.LENGTH_LONG);
        } else {
            mToast.setText(mString);
            mToast.setDuration(Toast.LENGTH_LONG);
        }
        mToast.show();
    }

    @SuppressLint("ShowToast")
    public static void l(int messageId) {
        if (mToast == null) {
            mToast = Toast.makeText(App.getInstance(), messageId, Toast.LENGTH_LONG);
        } else {
            mToast.setText(messageId);
            mToast.setDuration(Toast.LENGTH_LONG);
        }
        mToast.show();
    }
}
