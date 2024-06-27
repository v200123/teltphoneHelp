package com.u2tzjtne.telephonehelper.http.observer;

import android.content.Context;


import com.u2tzjtne.telephonehelper.http.handler.LoadingDialogHandler;
import com.u2tzjtne.telephonehelper.ui.dialog.LoadingDialog;
import com.u2tzjtne.telephonehelper.util.ToastUtils;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * @author u2tzjtne@gmail.com
 */
public class LoadingObserver<T> implements Observer<T>, LoadingDialog.CancelListener {
    private LoadingDialogHandler mLoadingDialogHandler;
    private Disposable disposable;

    public LoadingObserver(Context context) {
        mLoadingDialogHandler = new LoadingDialogHandler(context, this);
    }

    private void showLoadingDialog() {
        if (mLoadingDialogHandler != null) {
            mLoadingDialogHandler.obtainMessage(LoadingDialogHandler.SHOW_DIALOG).sendToTarget();
        }
    }

    private void dismissLoadingDialog() {
        if (mLoadingDialogHandler != null) {
            mLoadingDialogHandler.obtainMessage(LoadingDialogHandler.DISMISS_DIALOG).sendToTarget();
            mLoadingDialogHandler = null;
        }
    }

    @Override
    public void onSubscribe(Disposable disposable) {
        this.disposable = disposable;
        showLoadingDialog();
    }

    @Override
    public void onNext(T t) {
    }

    @Override
    public void onError(Throwable e) {
        dismissLoadingDialog();
        e.printStackTrace();
        ToastUtils.s("error:" + e.getMessage());
    }

    @Override
    public void onComplete() {
        dismissLoadingDialog();
    }

    @Override
    public void onCancel() {
        if (!disposable.isDisposed()) {
            disposable.dispose();
        }
    }
}
