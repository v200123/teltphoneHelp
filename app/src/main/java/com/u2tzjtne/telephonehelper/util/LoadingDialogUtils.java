package com.u2tzjtne.telephonehelper.util;

import android.content.Context;

import com.u2tzjtne.telephonehelper.ui.dialog.LoadingDialog;


/**
 * @author u2tzjtne@gmail.com
 */
public class LoadingDialogUtils {

    private static LoadingDialog mDialog;

    /**
     * 显示
     */
    public static void show(Context context) {
        dismiss();
        mDialog = new LoadingDialog(context);
        // 点击屏幕不隐藏
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setCancelable(true);
        if (!mDialog.isShowing()) {
            mDialog.show();
        }
    }

    /**
     * 显示
     */
    public static void show(Context context, LoadingDialog.CancelListener cancelListener) {
        dismiss();
        mDialog = new LoadingDialog(context);
        // 点击屏幕不隐藏
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setOnCancelListener(dialog -> cancelListener.onCancel());
        mDialog.setCancelable(true);
        if (!mDialog.isShowing()) {
            mDialog.show();
        }
    }

    /**
     * 隐藏
     */
    public static void dismiss() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }
}
