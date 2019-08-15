package com.automattic.simplenote.utils;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.automattic.android.tracks.CrashLogging.CrashLogging;
import com.automattic.android.tracks.CrashLogging.CrashLoggingDataProvider;
import com.automattic.android.tracks.TracksUser;
import com.automattic.simplenote.BuildConfig;
import com.automattic.simplenote.Simplenote;
import com.simperium.client.User;

import java.util.Locale;
import java.util.Map;

public class CrashUtils implements CrashLoggingDataProvider {

    private User currentUser;
    private Locale locale;

    private static CrashUtils sharedInstance;

    public static void initWithContext(Context context) {

        if (sharedInstance != null) {
            return;
        }

        sharedInstance = new CrashUtils();
        sharedInstance.locale = localeForContext(context);

        CrashLogging.start(context, sharedInstance);
    }

    public static void setCurrentUser(User user) {
        sharedInstance.currentUser = user;
        CrashLogging.setNeedsDataRefresh();
    }

    public static void clearCurrentUser() {
        sharedInstance.currentUser = null;
        CrashLogging.setNeedsDataRefresh();
    }

    @Override
    public String sentryDSN() {
        return BuildConfig.SENTRY_DSN;
    }

    @Override
    public boolean getUserHasOptedOut() {
        return !Simplenote.analyticsIsEnabled();
    }

    @NonNull
    @Override
    public String buildType() {
        return BuildConfig.BUILD_TYPE;
    }

    @NonNull
    @Override
    public String releaseName() {
        return BuildConfig.VERSION_NAME;
    }

    @Nullable
    @Override
    public TracksUser currentUser() {

        if (sharedInstance.currentUser == null) {
            return null;
        }

        String userID = sharedInstance.currentUser.getUserId();
        String email =  sharedInstance.currentUser.getEmail();

        return new TracksUser(userID, email, "");
    }

    @NonNull
    @Override
    public Map<String, Object> applicationContext() {
        return null;
    }

    @NonNull
    @Override
    public Map<String, Object> userContext() {
        return null;
    }

    @Nullable
    @Override
    public Locale locale() {
        return sharedInstance.locale;
    }

    @SuppressWarnings( "deprecation" )
    private static Locale localeForContext(Context context) {

        Resources resources = context.getResources();

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            return resources.getConfiguration().getLocales().get(0);
        }
        else {
            return resources.getConfiguration().locale;
        }
    }
}
