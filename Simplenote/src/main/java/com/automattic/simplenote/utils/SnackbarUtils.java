package com.automattic.simplenote.utils;

import android.app.Activity;
import android.support.annotation.ColorRes;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.view.View;

import com.automattic.simplenote.R;

/**
 * Created by onko on 31/03/2016.
 */
public class SnackbarUtils {

    public static Snackbar showSnackbar(Activity activity, @StringRes int message, @ColorRes int color, int duration) {
        Snackbar snackbar = Snackbar.make(activity.findViewById(android.R.id.content), message, duration);
        snackbar.getView().setBackgroundColor(ContextCompat.getColor(activity, color));
        snackbar.show();
        return snackbar;
    }

    public static Snackbar showSnackbar(Activity activity, @StringRes int message, @ColorRes int color, int duration,
                                        @StringRes int action, View.OnClickListener onClick) {
        Snackbar snackbar = Snackbar.make(activity.findViewById(android.R.id.content), message, duration);
        snackbar.setAction(action, onClick);
        snackbar.getView().setBackgroundColor(ContextCompat.getColor(activity, color));
        snackbar.setActionTextColor(ContextCompat.getColor(activity, R.color.white));
        snackbar.show();
        return snackbar;
    }
}
