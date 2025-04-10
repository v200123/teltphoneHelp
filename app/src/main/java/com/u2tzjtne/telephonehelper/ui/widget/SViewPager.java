package com.u2tzjtne.telephonehelper.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * @author u2tzjtne@gmail.com
 * @date 2020/5/31
 */
public class SViewPager extends NoPreloadViewPager {

    private boolean canScroll;

    public SViewPager(Context context) {
        super(context);
        canScroll = false;
    }

    public SViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        canScroll = false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (canScroll) {
            try {
                return super.onInterceptTouchEvent(ev);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (canScroll) {
            return super.onTouchEvent(event);
        }
        return false;
    }

    @Override
    public void setCurrentItem(int item) {
        super.setCurrentItem(item, false);
    }

    public void toggleLock() {
        canScroll = !canScroll;
    }

    public void setCanScroll(boolean canScroll) {
        this.canScroll = canScroll;
    }

    public boolean isCanScroll() {
        return canScroll;
    }

}
