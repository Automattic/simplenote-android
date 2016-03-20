package com.automattic.simplenote.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;
import android.widget.TextView;

import com.automattic.simplenote.R;
import com.automattic.simplenote.utils.DrawableUtils;

/**
 * Created by Ondrej Ruttkay on 19/03/2016.
 */
public class TintedTextView extends TextView {

    @ColorInt int tint;
    @ColorInt int transparentColor;
    Drawable l, r, t, b;

    public TintedTextView(Context context) {
        super(context);
    }

    public TintedTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public TintedTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        if (attrs != null) {
            transparentColor = ContextCompat.getColor(getContext(), android.R.color.transparent);
            TypedArray attr = getContext().obtainStyledAttributes(attrs, R.styleable.TintedTextView, 0, 0);
            tint = attr.getColor(R.styleable.TintedTextView_tint, transparentColor);
            attr.recycle();
        }

        if (tint != transparentColor) {
            setCompoundDrawablesWithIntrinsicBounds(l, t, r, b, tint);
        }
    }

    @Override
    public void setCompoundDrawables(Drawable left, Drawable top, Drawable right, Drawable bottom) {
        l = left;
        r = right;
        t = top;
        b = bottom;

        if (tint == transparentColor) {
            super.setCompoundDrawables(left, top, right, bottom);
        } else {
            setCompoundDrawables(left, top, right, bottom, tint);
        }
    }

    @Override
    public void setCompoundDrawablesWithIntrinsicBounds(Drawable left, Drawable top, Drawable right, Drawable bottom) {
        l = left;
        r = right;
        t = top;
        b = bottom;

        if (tint == transparentColor) {
            super.setCompoundDrawablesWithIntrinsicBounds(left, top, right, bottom);
        } else {
            setCompoundDrawablesWithIntrinsicBounds(left, top, right, bottom, tint);
        }
    }

    public void setCompoundDrawables(Drawable left, Drawable top, Drawable right, Drawable bottom, @ColorInt int color) {
        super.setCompoundDrawables(getTintedDrawable(left, color),
                getTintedDrawable(top, color),
                getTintedDrawable(right, color),
                getTintedDrawable(bottom, color));
    }

    public void setCompoundDrawablesWithIntrinsicBounds(Drawable left, Drawable top, Drawable right, Drawable bottom, @ColorInt int color) {
        super.setCompoundDrawablesWithIntrinsicBounds(getTintedDrawable(left, color),
                getTintedDrawable(top, color),
                getTintedDrawable(right, color),
                getTintedDrawable(bottom, color));
    }

    private Drawable getTintedDrawable(Drawable drawable, @ColorInt int color) {
        if (drawable != null) {
            return DrawableUtils.tintDrawable(drawable, color);
        }
        return null;
    }
}