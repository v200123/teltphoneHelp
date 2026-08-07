package com.example.myservicecenter.customView;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.myservicecenter.R;

/**
 * Reusable action button displayed at the right side of the home account card.
 */
public class HomeTopInfoRightOtherButtonView extends FrameLayout {

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
        View inflate = LayoutInflater.from(context).inflate(R.layout.view_home_top_info_right_other_button, null, false);
        iconView = inflate.findViewById(R.id.home2_0_top_info_right_item_left_img);
        titleView = inflate.findViewById(R.id.home2_0_top_info_right_item_text);
        unreadDotView = inflate.findViewById(R.id.home2_0_top_info_right_iv_unread);
        unreadCountView = inflate.findViewById(R.id.home2_0_top_info_right_tv_unread);
        addView(inflate);
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
        if (visible) {
            unreadCountView.setVisibility(GONE);
        }
    }

    public void setUnreadDotDrawable(@Nullable Drawable drawable) {
        unreadDotView.setImageDrawable(drawable);
    }

    public void setUnreadBadge(@Nullable CharSequence text) {
        unreadCountView.setText(text);
        boolean hasText = !TextUtils.isEmpty(text);
        if (hasText) {
            updateUnreadBadgeSize(text.length());
            if (text.length() > 2) {
                unreadCountView.setBackgroundResource(R.drawable.home2_0_top_info_right_msg_3);
            } else if (text.length() > 1) {
                unreadCountView.setBackgroundResource(R.drawable.home2_0_top_info_right_msg_2);
            } else {
                unreadCountView.setBackgroundResource(R.drawable.home2_0_top_info_right_msg_1);
            }
        }
        unreadCountView.setVisibility(hasText ? VISIBLE : GONE);
        if (hasText) {
            unreadDotView.setVisibility(GONE);
        }
    }

    public void setUnreadBadgeVisible(boolean visible) {
        unreadCountView.setVisibility(visible ? VISIBLE : GONE);
        if (visible) {
            unreadDotView.setVisibility(GONE);
        }
    }

    public void setUnreadBadgeBackground(@Nullable Drawable drawable) {
        unreadCountView.setBackground(drawable);
    }

    public void setUnreadBadgeTextColor(int color) {
        unreadCountView.setTextColor(color);
    }

    private void updateUnreadBadgeSize(int textLength) {
        ViewGroup.LayoutParams layoutParams = unreadCountView.getLayoutParams();
        layoutParams.height = getResources().getDimensionPixelSize(R.dimen.x26);
        if (textLength > 2) {
            layoutParams.width = getResources().getDimensionPixelSize(R.dimen.x44);
        } else if (textLength > 1) {
            layoutParams.width = getResources().getDimensionPixelSize(R.dimen.x34);
        } else {
            layoutParams.width = getResources().getDimensionPixelSize(R.dimen.x24);
        }
        unreadCountView.setLayoutParams(layoutParams);
    }

    /**
     * Updates the unread badge using the same rules as the original home title bar:
     * 1-9, 10-99 and 99+ use different badge widths; zero displays only a red dot;
     * negative values hide all unread indicators.
     *
     * @param unreadCount number of unread messages; a negative value means no indicator
     * @param showCount whether positive unread messages should show their count instead of a dot
     */
    public void updateUnreadView(int unreadCount, boolean showCount) {
        if (unreadCount < 0) {
            unreadCountView.setVisibility(GONE);
            unreadDotView.setVisibility(GONE);
            return;
        }

        if (unreadCount == 0) {
            unreadCountView.setVisibility(GONE);
            unreadDotView.setVisibility(VISIBLE);
            return;
        }

        if (!showCount) {
            unreadCountView.setVisibility(GONE);
            unreadDotView.setVisibility(VISIBLE);
            return;
        }

        ViewGroup.LayoutParams layoutParams = unreadCountView.getLayoutParams();
        layoutParams.height = getResources().getDimensionPixelSize(R.dimen.x26);

        if (unreadCount > 99) {
            unreadCountView.setText("99+");
            layoutParams.width = getResources().getDimensionPixelSize(R.dimen.x44);
            unreadCountView.setBackgroundResource(R.drawable.home2_0_top_info_right_msg_3);
        } else if (unreadCount > 9) {
            unreadCountView.setText(String.valueOf(unreadCount));
            layoutParams.width = getResources().getDimensionPixelSize(R.dimen.x34);
            unreadCountView.setBackgroundResource(R.drawable.home2_0_top_info_right_msg_2);
        } else {
            unreadCountView.setText(String.valueOf(unreadCount));
            layoutParams.width = getResources().getDimensionPixelSize(R.dimen.x24);
            unreadCountView.setBackgroundResource(R.drawable.home2_0_top_info_right_msg_1);
        }

        unreadCountView.setLayoutParams(layoutParams);
        unreadCountView.setVisibility(VISIBLE);
        unreadDotView.setVisibility(GONE);
    }

    /** Convenience overload for the usual case where a positive count should be displayed. */
    public void updateUnreadView(int unreadCount) {
        updateUnreadView(unreadCount, true);
    }

    /**
     * Backwards-compatible API. Numeric text is normalized to the standard unread badge.
     * Empty or non-numeric values hide both unread indicators.
     */
    public void setUnreadCount(@Nullable CharSequence unreadCount) {
        if (TextUtils.isEmpty(unreadCount)) {
            updateUnreadView(-1);
            return;
        }
        try {
            updateUnreadView(Integer.parseInt(unreadCount.toString().trim()));
        } catch (NumberFormatException ignored) {
            updateUnreadView(-1);
        }
    }

    public void setUnreadCount(int unreadCount) {
        updateUnreadView(unreadCount);
    }

    public void setOnButtonClickListener(@Nullable View.OnClickListener listener) {
        findViewById(R.id.home2_0_top_info_right_item_cl).setOnClickListener(listener);
    }
}
