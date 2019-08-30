package com.automattic.simplenote.utils;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.view.View;

import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

public class SnackbarUtils {
    public static Snackbar showSnackbar(Activity activity, @StringRes int message, @ColorRes int color, int duration) {
        Snackbar snackbar = Snackbar.make(activity.findViewById(android.R.id.content), message, duration);
        snackbar.getView().setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(activity, color)));
        snackbar.show();
        return snackbar;
    }

    public static Snackbar showSnackbar(Activity activity, @StringRes int message, @ColorRes int color, int duration,
                                        @StringRes int action, View.OnClickListener onClick) {
        Snackbar snackbar = Snackbar.make(activity.findViewById(android.R.id.content), message, duration);
        snackbar.setAction(action, onClick);
        snackbar.getView().setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(activity, color)));
        snackbar.setActionTextColor(ContextCompat.getColor(activity, android.R.color.white));
        snackbar.show();
        return snackbar;
    }
}
