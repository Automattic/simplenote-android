package com.automattic.simplenote.widgets.bottomsheet;

import android.view.View;

/**
 * A very simple view transformer which only handles applying a correct dim.
 */
public abstract class BaseViewTransformer implements ViewTransformer {

    public static final float MAX_DIM_ALPHA = 0.16f; // Customized for Simplenote

    @Override
    public float getDimAlpha(float translation, float maxTranslation, float peekedTranslation, BottomSheetLayout parent, View view) {
        float progress = translation / maxTranslation;
        return progress * MAX_DIM_ALPHA;
    }

}
