package com.automattic.simplenote.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.style.ImageSpan;

import androidx.annotation.NonNull;

import com.automattic.simplenote.utils.DisplayUtils;

// From https://stackoverflow.com/a/38788432/309558

public class CenteredImageSpan extends ImageSpan {
    // Ensures icon is centered properly
    private int mIconOversizeAdjustment;

    public CenteredImageSpan(Context context, @NonNull Drawable d) {
        super(d);

        mIconOversizeAdjustment = DisplayUtils.dpToPx(context, 1);
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, int start, int end,
                       Paint.FontMetricsInt fontMetricsInt) {
        Drawable drawable = getDrawable();
        Rect rect = drawable.getBounds();
        if (fontMetricsInt != null) {
            Paint.FontMetricsInt fmPaint = paint.getFontMetricsInt();
            int fontHeight = fmPaint.descent - fmPaint.ascent;
            int drHeight = (rect.bottom - rect.top) + mIconOversizeAdjustment;
            int centerY = fmPaint.ascent + fontHeight / 2;

            fontMetricsInt.ascent = centerY - drHeight / 2;
            fontMetricsInt.top = fontMetricsInt.ascent;
            fontMetricsInt.bottom = centerY + drHeight / 2;
            fontMetricsInt.descent = fontMetricsInt.bottom;
        }
        return rect.right;
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end,
                     float x, int top, int y, int bottom, @NonNull Paint paint) {

        Drawable drawable = getDrawable();
        Rect rect = drawable.getBounds();
        canvas.save();
        Paint.FontMetricsInt fmPaint = paint.getFontMetricsInt();
        int fontHeight = fmPaint.descent - fmPaint.ascent;
        int centerY = y + fmPaint.descent - fontHeight / 2;
        int drHeight = (rect.bottom - rect.top) + mIconOversizeAdjustment;
        int transY = centerY - drHeight / 2;
        canvas.translate(x, transY);
        drawable.draw(canvas);
        canvas.restore();
    }
}
