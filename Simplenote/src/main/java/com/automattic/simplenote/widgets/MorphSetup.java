package com.automattic.simplenote.widgets;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Helper class for setting up circle to rectangle shared element transitions.
 */
public class MorphSetup {
    public static final String EXTRA_SHARED_ELEMENT_COLOR_END = "EXTRA_SHARED_ELEMENT_COLOR_END";
    public static final String EXTRA_SHARED_ELEMENT_COLOR_START = "EXTRA_SHARED_ELEMENT_COLOR_START";
    public static final String EXTRA_SHARED_ELEMENT_RADIUS = "EXTRA_SHARED_ELEMENT_RADIUS";

    private MorphSetup() {
    }

    /**
     * Configure the shared element transitions for morphing from a circle to rectangle.  This needs
     * to be in code as we need to supply the color to transition from/to and corner radius which is
     * dynamically supplied depending upon where this screen is launched from.
     */
    public static void setSharedElementTransitions(@NonNull Activity activity, @Nullable View target, int radius) {
        if (!activity.getIntent().hasExtra(EXTRA_SHARED_ELEMENT_COLOR_END) || !activity.getIntent().hasExtra(EXTRA_SHARED_ELEMENT_COLOR_START)) {
            return;
        }

        int radiusStart = activity.getIntent().getIntExtra (EXTRA_SHARED_ELEMENT_RADIUS, -1);
        int colorEnd = activity.getIntent().getIntExtra(EXTRA_SHARED_ELEMENT_COLOR_END, Color.TRANSPARENT);
        int colorStart = activity.getIntent().getIntExtra(EXTRA_SHARED_ELEMENT_COLOR_START, Color.TRANSPARENT);
        Interpolator interpolator = AnimationUtils.loadInterpolator(activity, android.R.interpolator.fast_out_slow_in);
        MorphCircleToRectangle morphCircleToRectangle = new MorphCircleToRectangle(colorEnd, colorStart, radius, radiusStart);
        morphCircleToRectangle.setInterpolator(interpolator);
        MorphRectangleToCircle morphRectangleToCircle = new MorphRectangleToCircle(colorStart, colorEnd, radiusStart);
        morphRectangleToCircle.setInterpolator(interpolator);

        if (target != null) {
            morphCircleToRectangle.addTarget(target);
        }

        activity.getWindow().setSharedElementEnterTransition(morphCircleToRectangle);
        activity.getWindow().setSharedElementReturnTransition(morphRectangleToCircle);
    }
}
