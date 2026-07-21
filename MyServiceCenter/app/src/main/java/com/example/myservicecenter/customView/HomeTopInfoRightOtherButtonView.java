package com.example.myservicecenter.customView;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.myservicecenter.R;

/**
 * Reusable action button displayed at the right side of the home account card.
 */
public class HomeTopInfoRightOtherButtonView extends ConstraintLayout {

    public final ImageView iconView;
    private final TextView titleView;
    private final ImageView unreadDotView;
    private final TextView unreadCountView;

    public HomeTopInfoRightOtherButtonView(Context context) {
        this(context, null);
    }

    public HomeTopInfoRightOtherButtonView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HomeTopInfoRightOtherButtonView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.view_home_top_info_right_other_button, this, true);
        iconView = findViewById(R.id.home2_0_top_info_right_item_left_img);
        titleView = findViewById(R.id.home2_0_top_info_right_item_text);
        unreadDotView = findViewById(R.id.home2_0_top_info_right_iv_unread);
        unreadCountView = findViewById(R.id.home2_0_top_info_right_tv_unread);
    }

    public void setIcon(@Nullable Drawable drawable) {
        iconView.setImageDrawable(drawable);
    }

    public void setIconResource(int resId) {
        iconView.setImageResource(resId);
    }

    public void setText(@Nullable CharSequence text) {
        titleView.setText(text);
    }

    public CharSequence getText() {
        return titleView.getText();
    }

    public void setUnreadDotVisible(boolean visible) {
        unreadDotView.setVisibility(visible ? VISIBLE : GONE);
    }

    public void setUnreadCount(@Nullable CharSequence unreadCount) {
        boolean hasUnreadCount = !TextUtils.isEmpty(unreadCount);
        unreadCountView.setText(unreadCount);
        unreadCountView.setVisibility(hasUnreadCount ? VISIBLE : GONE);
    }

    public void setOnButtonClickListener(@Nullable View.OnClickListener listener) {
        findViewById(R.id.home2_0_top_info_right_item_cl).setOnClickListener(listener);
    }
}
