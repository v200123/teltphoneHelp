package com.example.myservicecenter.customView;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.example.myservicecenter.R;

import kotlin.Unit;

/**
 * Reusable top-navigation text tab based on {@code tab_item_home2_0_text.xml}.
 *
 * <p>Use {@link #setText(CharSequence)} to set its label and
 * {@link #setIndicatorVisible(boolean)} to show the selection indicator.</p>
 */
public class HomeTopTabItemView extends ConstraintLayout {

    private final TextView titleView;
    private final View indicatorView;

    public HomeTopTabItemView(Context context) {
        this(context, null);
    }

    public HomeTopTabItemView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HomeTopTabItemView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.tab_item_home2_0_text, this, true);
        titleView = findViewById(R.id.tv_top_view_tab_item);
        indicatorView = findViewById(R.id.line_home_plus_tab);
        applyAttributes(context, attrs, defStyleAttr);
    }

    private void applyAttributes(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        TypedArray attributes = context.obtainStyledAttributes(
                attrs,
                R.styleable.HomeTopTabItemView,
                defStyleAttr,
                0
        );
        try {
            CharSequence text = attributes.getText(R.styleable.HomeTopTabItemView_tabText);
            if (text != null) {
                setText(text);
            }
            if (attributes.hasValue(R.styleable.HomeTopTabItemView_tabTextColor)) {
                setTextColor(attributes.getColor(
                        R.styleable.HomeTopTabItemView_tabTextColor,
                        titleView.getCurrentTextColor()
                ));
            }
            boolean selected = attributes.getBoolean(R.styleable.HomeTopTabItemView_tabSelected, false);
            boolean indicatorVisible = attributes.getBoolean(
                    R.styleable.HomeTopTabItemView_tabIndicatorVisible,
                    selected
            );
            setSelected(selected);
            setIndicatorVisible(indicatorVisible);
        } finally {
            attributes.recycle();
        }
    }

    public void setText(CharSequence text) {
        titleView.setText(text);
    }

    public CharSequence getText() {
        return titleView.getText();
    }

    public void setTextColor(int color) {
        titleView.setTextColor(color);
    }

    public void setIndicatorVisible(boolean visible) {
        indicatorView.setVisibility(visible ? VISIBLE : INVISIBLE);
    }

    public boolean isIndicatorVisible() {
        return indicatorView.getVisibility() == VISIBLE;
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        titleView.setSelected(selected);
        if(selected){
            titleView.getPaint().setFakeBoldText(true);
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.x40));
        }else{
            titleView.getPaint().setFakeBoldText(false);
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.x34));
            titleView.setTextColor(Color.parseColor("#333333"));
        }
        indicatorView.setSelected(selected);
        setIndicatorVisible(selected);
    }
}
