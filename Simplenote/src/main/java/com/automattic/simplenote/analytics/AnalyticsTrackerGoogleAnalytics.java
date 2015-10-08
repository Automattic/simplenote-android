package com.automattic.simplenote.analytics;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.automattic.android.tracks.TracksClient;
import com.automattic.simplenote.BuildConfig;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.json.JSONObject;

import java.util.Map;
import java.util.UUID;

public class AnalyticsTrackerGoogleAnalytics implements AnalyticsTracker.Tracker {
    private Tracker mTracker;
    private Context mContext;

    public AnalyticsTrackerGoogleAnalytics(Context context) {
        mContext = context;
    }

    // Google Analytics tracker
    private synchronized Tracker getTracker() {
        if (mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(mContext);
            mTracker = analytics.newTracker(BuildConfig.GOOGLE_ANALYTICS_ID);
            mTracker.enableAutoActivityTracking(true);
            mTracker.enableExceptionReporting(true);
        }

        return mTracker;
    }

    @Override
    public void track(AnalyticsTracker.Stat stat, String category, String label) {
        track(stat, category, label, null);
    }

    @Override
    public void track(AnalyticsTracker.Stat stat, String category, String label, Map<String, ?> properties) {
        if (getTracker() == null) {
            return;
        }

        String eventName = stat.name().toLowerCase();

        HitBuilders.EventBuilder eventBuilder = new HitBuilders.EventBuilder();

        if (!TextUtils.isEmpty(category)) {
            eventBuilder.setCategory(category);
        }

        if (!TextUtils.isEmpty(label)) {
            eventBuilder.setLabel(label);
        }

        eventBuilder.setAction(eventName);

        getTracker().send(eventBuilder.build());
    }

    @Override
    public void refreshMetadata(String username) {

    }
}
