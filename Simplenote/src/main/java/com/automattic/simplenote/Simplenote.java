package com.automattic.simplenote;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.res.Configuration;
import android.os.Bundle;

import com.automattic.simplenote.analytics.AnalyticsTracker;
import com.automattic.simplenote.analytics.AnalyticsTrackerGoogleAnalytics;
import com.automattic.simplenote.analytics.AnalyticsTrackerNosara;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.NoteCountIndexer;
import com.automattic.simplenote.models.NoteTagger;
import com.automattic.simplenote.models.Tag;
import com.automattic.simplenote.utils.PrefUtils;
import com.simperium.Simperium;
import com.simperium.client.Bucket;
import com.simperium.client.BucketNameInvalid;

import org.wordpress.passcodelock.AppLockManager;

public class Simplenote extends Application {

    // log tag
    public static final String TAG = "Simplenote";

    // intent IDs
    public static final int INTENT_PREFERENCES = 1;
    public static final int INTENT_EDIT_NOTE = 2;

    private static final String AUTH_PROVIDER = "simplenote.com";

    public static final String DELETED_NOTE_ID = "deletedNoteId";

    private Simperium mSimperium;
    private Bucket<Note> mNotesBucket;
    private Bucket<Tag> mTagsBucket;

    public void onCreate() {
        super.onCreate();

        AppLockManager.getInstance().enableDefaultAppLockIfAvailable(this);

        mSimperium = Simperium.newClient(
                BuildConfig.SIMPERIUM_APP_ID,
                BuildConfig.SIMPERIUM_APP_KEY,
                this
        );

        mSimperium.setAuthProvider(AUTH_PROVIDER);

        try {
            mNotesBucket = mSimperium.bucket(new Note.Schema());
            Tag.Schema tagSchema = new Tag.Schema();
            tagSchema.addIndex(new NoteCountIndexer(mNotesBucket));
            mTagsBucket = mSimperium.bucket(tagSchema);

            // Every time a note changes or is deleted we need to reindex the tag counts
            mNotesBucket.addListener(new NoteTagger(mTagsBucket));
        } catch (BucketNameInvalid e) {
            throw new RuntimeException("Could not create bucket", e);
        }

        ApplicationLifecycleMonitor applicationLifecycleMonitor = new ApplicationLifecycleMonitor();
        registerComponentCallbacks(applicationLifecycleMonitor);
        registerActivityLifecycleCallbacks(applicationLifecycleMonitor);

        AnalyticsTracker.registerTracker(new AnalyticsTrackerGoogleAnalytics(this));
        AnalyticsTracker.registerTracker(new AnalyticsTrackerNosara(this));
        AnalyticsTracker.refreshMetadata(mSimperium.getUser().getEmail());
    }

    private boolean isFirstLaunch() {
        // NotesActivity sets this pref to false after first launch
        return PrefUtils.getBoolPref(this, PrefUtils.PREF_FIRST_LAUNCH, true);
    }

    public Simperium getSimperium() {
        return mSimperium;
    }

    public Bucket<Note> getNotesBucket() {
        return mNotesBucket;
    }

    public Bucket<Tag> getTagsBucket() {
        return mTagsBucket;
    }

    private class ApplicationLifecycleMonitor implements Application.ActivityLifecycleCallbacks,
            ComponentCallbacks2 {
        private boolean mIsInBackground = true;

        // ComponentCallbacks
        @Override
        public void onTrimMemory(int level) {
            // Send analytics if app is in the background
            if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
                mIsInBackground = true;
                AnalyticsTracker.track(
                        AnalyticsTracker.Stat.APPLICATION_CLOSED,
                        AnalyticsTracker.CATEGORY_USER,
                        "application_closed"
                );
                AnalyticsTracker.flush();
            } else {
                mIsInBackground = false;
            }
        }

        @Override
        public void onConfigurationChanged(Configuration newConfig) {}

        @Override
        public void onLowMemory() {}

        // ActivityLifeCycle callbacks
        @Override
        public void onActivityResumed(Activity activity) {
            if (mIsInBackground) {
                AnalyticsTracker.track(
                        AnalyticsTracker.Stat.APPLICATION_OPENED,
                        AnalyticsTracker.CATEGORY_USER,
                        "application_opened"
                );

                mIsInBackground = false;
            }
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

        @Override
        public void onActivityStarted(Activity activity) {}

        @Override
        public void onActivityPaused(Activity activity) {}

        @Override
        public void onActivityStopped(Activity activity) {}

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

        @Override
        public void onActivityDestroyed(Activity activity) {}
    }
}
