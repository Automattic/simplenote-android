package com.automattic.simplenote.analytics;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.preference.PreferenceManager;

import com.automattic.android.tracks.TracksClient;

import org.json.JSONObject;

import java.util.Map;
import java.util.UUID;

public class AnalyticsTrackerNosara implements AnalyticsTracker.Tracker {

    private static final String TRACKS_ANON_ID = "nosara_tracks_anon_id";
    private static final String EVENTS_PREFIX = "spandroid_";

    private String mUserName = null;
    private String mAnonID = null; // do not access this variable directly. Use methods.

    private TracksClient mNosaraClient;
    private Context mContext;

    public AnalyticsTrackerNosara(Context context) {
        if (null == context) {
            mNosaraClient = null;
            return;
        }

        mContext = context;
        mNosaraClient = TracksClient.getClient(context);
    }

    private void clearAnonID() {
        mAnonID = null;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        if (preferences.contains(TRACKS_ANON_ID)) {
            final SharedPreferences.Editor editor = preferences.edit();
            editor.remove(TRACKS_ANON_ID);
            editor.apply();
        }
    }

    private String getAnonID() {
        if (mAnonID == null) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            mAnonID = preferences.getString(TRACKS_ANON_ID, null);
        }
        return mAnonID;
    }

    private String generateNewAnonID() {
        String uuid = UUID.randomUUID().toString();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putString(TRACKS_ANON_ID, uuid);
        editor.apply();

        mAnonID = uuid;
        return uuid;
    }

    @Override
    public void track(AnalyticsTracker.Stat stat, String category, String label) {
        track(stat, category, label, null);
    }

    @Override
    public void track(AnalyticsTracker.Stat stat, String category, String label, Map<String, ?> properties) {
        if (mNosaraClient == null) {
            return;
        }

        String eventName = stat.name().toLowerCase();

        final String user;
        final TracksClient.NosaraUserType userType;
        if (mUserName != null) {
            user = mUserName;
            userType = TracksClient.NosaraUserType.SIMPLENOTE;
        } else {
            // This is just a security checks since the anonID is already available here.
            // refresh metadata is called on login/logout/startup and it loads/generates the anonId when necessary.
            if (getAnonID() == null) {
                user = generateNewAnonID();
            } else {
                user = getAnonID();
            }
            userType = TracksClient.NosaraUserType.ANON;
        }

        if (properties != null) {
            JSONObject propertiesJson = new JSONObject(properties);
            mNosaraClient.track(EVENTS_PREFIX + eventName, propertiesJson, user, userType);
        } else {
            mNosaraClient.track(EVENTS_PREFIX + eventName, user, userType);
        }
    }

    @Override
    public void refreshMetadata(String username) {
        if (mNosaraClient == null) {
            return;
        }

        if (!TextUtils.isEmpty(username)) {
            mUserName = username;
            if (getAnonID() != null) {
                mNosaraClient.trackAliasUser(mUserName, getAnonID(), TracksClient.NosaraUserType.SIMPLENOTE);
                clearAnonID();
            }
        } else {
            mUserName = null;
            if (getAnonID() == null) {
                generateNewAnonID();
            }
        }
    }

    @Override
    public void flush() {
        if (mNosaraClient == null) {
            return;
        }

        mNosaraClient.flush();
    }
}