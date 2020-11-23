package com.automattic.simplenote.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class ImageButtonBehavior extends CoordinatorLayout.Behavior<ImageButton> {
    public ImageButtonBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean layoutDependsOn(@NonNull CoordinatorLayout parent, @NonNull ImageButton child, @NonNull View dependency) {
        return dependency instanceof Snackbar.SnackbarLayout;
    }

    @Override
    public boolean onDependentViewChanged(@NonNull CoordinatorLayout parent, @NonNull ImageButton child, @NonNull View dependency) {
        child.setTranslationY(getTranslationForSnackbar(parent, child));
        return super.onDependentViewChanged(parent, child, dependency);
    }

    @Override
    public void onDependentViewRemoved(@NonNull CoordinatorLayout parent, @NonNull ImageButton child, @NonNull View dependency) {
        super.onDependentViewRemoved(parent, child, dependency);
        child.setTranslationY(0);
    }

    private float getTranslationForSnackbar(CoordinatorLayout parent, ImageButton button) {
        float offset = 0;
        final List<View> dependencies = parent.getDependencies(button);

        for (int i = 0, z = dependencies.size(); i < z; i++) {
            final View view = dependencies.get(i);

            if (view instanceof Snackbar.SnackbarLayout && parent.doViewsOverlap(button, view)) {
                offset = Math.min(offset, view.getTranslationY() - view.getHeight());
            }
        }

        return offset;
    }
}
