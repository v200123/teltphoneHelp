package com.example.myservicecenter.customView;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.myservicecenter.R;

public class BaseCodeView extends FrameLayout {
    private TextView descriptionText;
    private CustomFontTextView numberText;
    private TextView redTipView;
    private RelativeLayout tipLayout;
    private TextView tipText;
    private CustomFontTextView unitText;


    public BaseCodeView(Context context) {
        super(context);
        init();
    }

    public BaseCodeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BaseCodeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.base_code_item_layout, this);
        initView();
    }

    private void initView() {
        this.tipLayout = (RelativeLayout) findViewById(R.id.fl_card_ticket_tip_txt);
        this.tipText = (TextView) findViewById(R.id.card_ticket_tip_txt);
        this.redTipView = (TextView) findViewById(R.id.card_ticket_red_tip);
        this.numberText = (CustomFontTextView) findViewById(R.id.card_ticket_txt);
        this.unitText = (CustomFontTextView) findViewById(R.id.card_ticket_txt_right);
        this.descriptionText = (TextView) findViewById(R.id.card_ticket_name_txt);
    }

    public void setData(String number, String unit, String description) {
        setNumber(number);
        setUnit(unit);
        setDescription(description);
    }

    public void setNumber(String number) {
        String safeNumber = safeText(number);
        this.numberText.setText(safeNumber.length() == 0 ? "--" : safeNumber);
    }

    public void setUnit(String unit) {
        String safeUnit = safeText(unit);
        this.unitText.setText(safeUnit);
        this.unitText.setVisibility(safeUnit.length() == 0 ? View.GONE : View.VISIBLE);
    }

    public void setDescription(String description) {
        this.descriptionText.setText(safeText(description));
    }

    public void setRedTipVisible(boolean visible) {
        this.redTipView.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void setTipText(String text) {
        String safeTipText = safeText(text);
        this.tipText.setText(safeTipText);
        setTipVisible(safeTipText.length() != 0);
    }

    public void setTipVisible(boolean visible) {
        this.tipLayout.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private String safeText(String text) {
        return text == null ? "" : text;
    }
}
