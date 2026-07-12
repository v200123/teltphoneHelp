package com.example.myservicecenter.customView;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import com.google.android.material.tabs.TabItem;


import java.util.List;

/**
 * Five-item bottom tab container restored from {@code TabLayoutV2.kt}.
 *
 * <p>Call {@link #updateTabIcon(List)} before selecting a tab. Selection is
 * reported through {@link OnTabSelectedListenerV2}; the {@code fromClick}
 * argument is {@code true} only for a user tap.</p>
 */
public final class TabLayoutV2 extends LinearLayout {

    public static final int TAB_COUNT = 5;

    private int mCurrentIndex;
    private final OnTabSelectedListenerV2 mInnerTabSelectedListener =
            new OnTabSelectedListenerV2() {
                @Override
                public void onSelected(int position, boolean fromClick) {
//                    selectTab(position, fromClick);
                }
            };
    private OnTabSelectedListenerV2 mOnTabSelectedListener;

    public TabLayoutV2(Context context) {
        this(context, null);
    }

    public TabLayoutV2(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TabLayoutV2(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(HORIZONTAL);

        for (int index = 0; index < TAB_COUNT; index++) {
            TabItem tabItem = new TabItem(context);
//            TalkBackUtil.accessibilityDelegateView2Button(tabItem);
            addView(tabItem);
        }
    }

    public int getTAB_COUNT() {
        return TAB_COUNT;
    }

    public int getMCurrentIndex() {
        return mCurrentIndex;
    }

    public void setMCurrentIndex(int currentIndex) {
        mCurrentIndex = currentIndex;
    }

    /** Updates all five tab views. Lists with any other size are ignored. */
    public void updateTabIcon(List<? extends TabIconBean> tabIconList) {
        if (tabIconList == null || tabIconList.size() != TAB_COUNT) {
            return;
        }

//        for (int index = 0; index < TAB_COUNT; index++) {
//            getTabItem(index).updateTabItem(
//                    tabIconList.get(index),
//                    index,
//                    mInnerTabSelectedListener,
//                    mCurrentIndex == index
//            );
//        }
    }

    public void setTabSelectedListener(OnTabSelectedListenerV2 tabSelectedListener) {
        mOnTabSelectedListener = tabSelectedListener;
    }

//    public TabIconBean getCurrentTabItem() {
//        return getTabItemInfoWithPosition(mCurrentIndex);
//    }

//    public TabIconBean getTabItemInfoWithPosition(int position) {
//        if (!isValidPosition(position)) {
//            return null;
//        }
//        return getTabItem(position).getMTabIconBean();
//    }

//    public void selected(int position) {
//        selectTab(position, false);
//    }
//
//    private void selectTab(int position, boolean fromClick) {
//        if (!isValidPosition(position) || mCurrentIndex == position) {
//            return;
//        }
//
//        TabItem selectedTab = getTabItem(position);
//        TabIconBean tabIconBean = selectedTab.getMTabIconBean();
//        if (tabIconBean != null && tabIconBean.isLingXi) {
//            // 灵犀入口不会替换当前选中的普通标签。
//            selectedTab.onSelected();
//        } else {
//            mCurrentIndex = position;
//            for (int index = 0; index < getChildCount(); index++) {
//                TabItem tabItem = getTabItem(index);
//                if (index == position) {
//                    tabItem.onSelected();
//                } else {
//                    tabItem.onUnSelected();
//                }
//            }
//        }
//
//        if (mOnTabSelectedListener != null) {
//            mOnTabSelectedListener.onSelected(position, fromClick);
//        }
//    }
//
//    /** Starts any deferred image load for the third tab (the original behavior). */
//    public void runLoadImage() {
//        if (getChildCount() > 2) {
//            getTabItem(2).runLoadImage();
//        }
//    }
//
//    private boolean isValidPosition(int position) {
//        return position >= 0 && position < TAB_COUNT && position < getChildCount();
//    }
//
//    private TabItem getTabItem(int position) {
//        View child = getChildAt(position);
//        if (!(child instanceof TabItem)) {
//            throw new IllegalStateException("Expected TabItem at position " + position);
//        }
//        return (TabItem) child;
//    }
}
