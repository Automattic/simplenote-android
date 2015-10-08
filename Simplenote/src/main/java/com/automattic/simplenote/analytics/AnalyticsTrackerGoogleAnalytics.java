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

        String eventName;
        // convert some of the events for backwards compatibility with Google Analytics
        switch (stat) {
            case EDITOR_NOTE_CONTENT_SHARED:
                eventName = "shared_note";
                break;
            case EDITOR_TAG_ADDED:
                eventName = "added_tag";
                break;
            case EDITOR_TAG_REMOVED:
                eventName = "removed_tag";
                break;
            case EDITOR_NOTE_PINNED:
                eventName = "pinned_note";
                break;
            case EDITOR_NOTE_UNPINNED:
                eventName = "unpinned_note";
                break;
            case EDITOR_NOTE_EDITED:
                eventName = "edited_note";
                break;
            case LIST_NOTE_CREATED:
                eventName = "create_note";
                break;
            case LIST_TAG_VIEWED:
                eventName = "viewed_notes_for_tag";
                break;
            case LIST_NOTES_SEARCHED:
                eventName = "searched_notes";
                break;
            case LIST_NOTE_DELETED:
                eventName = "deleted_note";
                break;
            case EDITOR_NOTE_RESTORED:
                eventName = "restored_notes";
                break;
            case LIST_TRASH_EMPTIED:
                eventName = "trash_emptied";
                break;
            case LIST_NOTE_OPENED:
                eventName = "viewed_note";
                break;
            case USER_ACCOUNT_CREATED:
                eventName = "new_account_created";
                break;
            case USER_SIGNED_IN:
                eventName = "signed_in";
                break;
            case LIST_TRASH_VIEWED:
                eventName = "viewed_trash";
                break;
            case TAG_MENU_DELETED:
                eventName = "deleted_tag";
                break;
            default:
                eventName = stat.name().toLowerCase();
        }

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
