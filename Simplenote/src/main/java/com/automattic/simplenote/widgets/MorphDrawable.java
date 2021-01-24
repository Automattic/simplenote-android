package com.automattic.simplenote.widgets;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.Property;

import androidx.annotation.ColorInt;

import com.automattic.simplenote.utils.AniUtils;

/**
 * A drawable that can morph size, shape (via it's corner radius) and color.  Specifically this is
 * useful for animating between a floating action button and another activity.
 */
public class MorphDrawable extends Drawable {
    private Paint mPaint;
    private float mRadius;

    public static final Property<MorphDrawable, Integer> COLOR = new AniUtils.IntProperty<MorphDrawable>("color") {
        @Override
        public Integer get(MorphDrawable morphDrawable) {
            return morphDrawable.getColor();
        }

        @Override
        public void setValue(MorphDrawable morphDrawable, int value) {
            morphDrawable.setColor(value);
        }
    };

    public static final Property<MorphDrawable, Float> RADIUS = new AniUtils.FloatProperty<MorphDrawable>("radius") {
        @Override
        public Float get(MorphDrawable morphDrawable) {
            return morphDrawable.getRadius();
        }

        @Override
        public void setValue(MorphDrawable morphDrawable, float value) {
            morphDrawable.setRadius(value);
        }
    };

    public MorphDrawable(@ColorInt int color, float radius) {
        mRadius = radius;
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(color);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawRoundRect(getBounds().left, getBounds().top, getBounds().right, getBounds().bottom, mRadius, mRadius, mPaint);
    }

    @Override
    public int getOpacity() {
        return mPaint.getAlpha();
    }

    @Override
    public void getOutline(Outline outline) {
        outline.setRoundRect(getBounds(), mRadius);
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mPaint.setColorFilter(cf);
        invalidateSelf();
    }

    public int getColor() {
        return mPaint.getColor();
    }

    public float getRadius() {
        return mRadius;
    }

    public void setColor(int color) {
        mPaint.setColor(color);
        invalidateSelf();
    }

    public void setRadius(float cornerRadius) {
        mRadius = cornerRadius;
        invalidateSelf();
    }
}
