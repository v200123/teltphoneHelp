package com.example.myservicecenter.customView;

import android.content.Context;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatTextView;

/* JADX INFO: loaded from: D:\AndroidProject\chinamobile\app\classes26.dex */
public class CustomFontTextView extends AppCompatTextView {
    public CustomFontTextView(Context context) {
        super(context);
        applyCustomFont(context);
    }

    public CustomFontTextView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        applyCustomFont(context);
    }

    public CustomFontTextView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        applyCustomFont(context);
    }

    private void applyCustomFont(Context context) {
        setTypeface(BaseModuleFontCache.getTypeface("base_module_font.otf", context));
    }
}
