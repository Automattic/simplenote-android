package com.automattic.simplenote.widgets;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.graphics.Color;
import android.transition.ChangeBounds;
import android.transition.TransitionValues;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import androidx.annotation.ColorInt;

/**
 * A transition that morphs a rectangle into a circle and cross-fades the background color.
 */
public class MorphRectangleToCircle extends ChangeBounds {
    private static final String PROPERTY_COLOR = "color";
    private static final String PROPERTY_RADIUS = "radius";
    private static final String[] TRANSITION_PROPERTIES = {PROPERTY_COLOR, PROPERTY_RADIUS};
    private static final int DURATION = 300;

    private @ColorInt int mColorEnd = Color.TRANSPARENT;
    private @ColorInt int mColorStart = Color.TRANSPARENT;
    private int mRadiusEnd = -1;

    public MorphRectangleToCircle(@ColorInt int colorEnd, @ColorInt int colorStart, int radiusEnd) {
        super();
        setColorEnd(colorEnd);
        setColorStart(colorStart);
        setRadiusEnd(radiusEnd);
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues) {
        super.captureEndValues(transitionValues);
        final View view = transitionValues.view;

        if (view.getWidth() <= 0 || view.getHeight() <= 0) {
            return;
        }

        transitionValues.values.put(PROPERTY_COLOR, mColorEnd);
        transitionValues.values.put(PROPERTY_RADIUS, mRadiusEnd >= 0 ? mRadiusEnd : view.getHeight() / 2);
    }

    @Override
    public void captureStartValues(TransitionValues transitionValues) {
        super.captureStartValues(transitionValues);
        final View view = transitionValues.view;

        if (view.getWidth() <= 0 || view.getHeight() <= 0) {
            return;
        }

        transitionValues.values.put(PROPERTY_COLOR, mColorStart);
        transitionValues.values.put(PROPERTY_RADIUS, mRadiusEnd);
    }

    @Override
    public Animator createAnimator(final ViewGroup root, TransitionValues valuesStart, TransitionValues valuesEnd) {
        Animator bounds = super.createAnimator(root, valuesStart, valuesEnd);

        if (valuesStart == null || valuesEnd == null || bounds == null) {
            return null;
        }

        Integer colorStart = (Integer) valuesStart.values.get(PROPERTY_COLOR);
        Integer radiusStart = (Integer) valuesStart.values.get(PROPERTY_RADIUS);
        Integer colorEnd = (Integer) valuesEnd.values.get(PROPERTY_COLOR);
        Integer radiusEnd = (Integer) valuesEnd.values.get(PROPERTY_RADIUS);

        if (colorStart == null || radiusStart == null || colorEnd == null || radiusEnd == null) {
            return null;
        }

        MorphDrawable background = new MorphDrawable(colorStart, radiusStart);
        valuesEnd.view.setBackground(background);

        Animator color = ObjectAnimator.ofArgb(background, MorphDrawable.COLOR, colorEnd);
        Animator corners = ObjectAnimator.ofFloat(background, MorphDrawable.RADIUS, radiusEnd);
        TimeInterpolator interpolator = AnimationUtils.loadInterpolator(root.getContext(), android.R.interpolator.fast_out_slow_in);

        if (valuesEnd.view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) valuesEnd.view;

            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                child.animate()
                    .alpha(0f)
                    .translationY(child.getHeight() / 3f)
                    .setStartDelay(0L)
                    .setDuration(50L)
                    .setInterpolator(interpolator)
                    .start();
            }
        }

        AnimatorSet transition = new AnimatorSet();
        transition.playTogether(bounds, corners, color);
        transition.setDuration(DURATION);
        transition.setInterpolator(interpolator);
        return transition;
    }

    @Override
    public String[] getTransitionProperties() {
        return TRANSITION_PROPERTIES;
    }

    public void setColorEnd(@ColorInt int colorEnd) {
        mColorEnd = colorEnd;
    }

    public void setColorStart(@ColorInt int colorStart) {
        mColorStart = colorStart;
    }

    public void setRadiusEnd(int radiusEnd) {
        mRadiusEnd = radiusEnd;
    }
}
