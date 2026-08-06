package com.example.myservicecenter.customView;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import com.example.myservicecenter.R;

/**
 * A TextView which changes its visual weight by adjusting alpha or adding a text stroke.
 *
 * <p>weight = 1 draws normally; 0 &lt; weight &lt; 1 lowers opacity; weight &gt; 1
 * simulates bold text with a fill-and-stroke paint.</p>
 */
public class CustomBoldTextView extends AppCompatTextView {

    private float fontWeight = 1.0f;

    public CustomBoldTextView(Context context) {
        this(context, null);
    }

    public CustomBoldTextView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomBoldTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(@Nullable AttributeSet attrs) {
        float weight = 1.0f;
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(
                    attrs, R.styleable.CustomBoldTextView);
            weight = a.getFloat(0, 1.0f);
            a.recycle();
        }
        setFontWeight(weight);
    }

    /**
     * Sets the display weight. Values below zero are treated as zero.
     */
    public void setFontWeight(float weight) {
        fontWeight = Math.max(weight, 0.0f);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (fontWeight <= 0.0f) {
            return;
        }
        if (fontWeight == 1.0f) {
            super.onDraw(canvas);
            return;
        }

        TextPaint paint = getPaint();
        Paint.Style originalStyle = paint.getStyle();
        float originalStrokeWidth = paint.getStrokeWidth();
        int originalColor = paint.getColor();

        if (fontWeight < 1.0f) {
            paint.setAlpha(Math.max(0, Math.min((int) (255 * fontWeight), 255)));
        } else {
            float strokeWidth = Math.min(
                    paint.getTextSize() * (fontWeight - 1.0f) / 15.0f,
                    paint.getTextSize() / 4.0f);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            paint.setStrokeWidth(strokeWidth);
        }

        super.onDraw(canvas);

        paint.setStyle(originalStyle);
        paint.setStrokeWidth(originalStrokeWidth);
        paint.setColor(originalColor);
    }
}
