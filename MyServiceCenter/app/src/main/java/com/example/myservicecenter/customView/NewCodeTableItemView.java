package com.example.myservicecenter.customView;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.myservicecenter.R;

/**
 * Reusable code-table indicator item based on {@code layout_new_code_table_item.xml}.
 */
public class NewCodeTableItemView extends ConstraintLayout {

    private final TextView tipTextView;
    private final TextView valueTextView;
    private final TextView unitTextView;
    private final TextView titleTextView;

    public NewCodeTableItemView(Context context) {
        this(context, null);
    }

    public NewCodeTableItemView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NewCodeTableItemView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.layout_new_code_table_item, this, true);
        tipTextView = findViewById(R.id.new_code_table_item_tiptext);
        valueTextView = findViewById(R.id.new_code_table_item_value);
        unitTextView = findViewById(R.id.new_code_table_item_unit);
        titleTextView = findViewById(R.id.new_code_table_item_title);
        applyAttributes(context, attrs, defStyleAttr);
    }

    private void applyAttributes(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        TypedArray attributes = context.obtainStyledAttributes(
                attrs, R.styleable.NewCodeTableItemView, defStyleAttr, 0
        );
        try {
            setTipText(attributes.getText(R.styleable.NewCodeTableItemView_codeTableTipText));
            setValueText(attributes.getText(R.styleable.NewCodeTableItemView_codeTableValueText));
            setUnitText(attributes.getText(R.styleable.NewCodeTableItemView_codeTableUnitText));
            setTitleText(attributes.getText(R.styleable.NewCodeTableItemView_codeTableTitleText));
            setTipVisible(attributes.getBoolean(
                    R.styleable.NewCodeTableItemView_codeTableTipVisible, false
            ));
        } finally {
            attributes.recycle();
        }
    }

    public void setTipText(@Nullable CharSequence text) {
        tipTextView.setText(text);
    }

    public void setValueText(@Nullable CharSequence text) {
        valueTextView.setText(text);
    }

    public void setUnitText(@Nullable CharSequence text) {
        unitTextView.setText(text);
    }

    public void setTitleText(@Nullable CharSequence text) {
        titleTextView.setText(text);
    }

    public CharSequence getTipText() {
        return tipTextView.getText();
    }

    public CharSequence getValueText() {
        return valueTextView.getText();
    }

    public CharSequence getUnitText() {
        return unitTextView.getText();
    }

    public CharSequence getTitleText() {
        return titleTextView.getText();
    }

    public void setTipVisible(boolean visible) {
        tipTextView.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public boolean isTipVisible() {
        return tipTextView.getVisibility() == View.VISIBLE;
    }

    /** Shows the tip only when a non-empty tip is supplied. */
    public void setTip(@Nullable CharSequence text) {
        setTipText(text);
        setTipVisible(!TextUtils.isEmpty(text));
    }
}
