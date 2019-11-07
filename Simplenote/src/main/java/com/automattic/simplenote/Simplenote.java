package com.automattic.simplenote;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatDelegate;

import com.automattic.simplenote.analytics.AnalyticsTracker;
import com.automattic.simplenote.analytics.AnalyticsTrackerNosara;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.NoteCountIndexer;
import com.automattic.simplenote.models.NoteTagger;
import com.automattic.simplenote.models.Preferences;
import com.automattic.simplenote.models.Tag;
import com.automattic.simplenote.utils.CrashUtils;
import com.automattic.simplenote.utils.PrefUtils;
import com.simperium.Simperium;
import com.simperium.client.Bucket;
import com.simperium.client.BucketNameInvalid;
import com.simperium.client.BucketObjectMissingException;

import org.wordpress.passcodelock.AppLockManager;

import static com.automattic.simplenote.models.Preferences.PREFERENCES_OBJECT_KEY;

public class Simplenote extends Application {

    private static final int TEN_SECONDS_MILLIS = 10000;

    // log tag
    public static final String TAG = "Simplenote";

    // intent IDs
    public static final int INTENT_PREFERENCES = 1;
    public static final int INTENT_EDIT_NOTE = 2;
    public static final String DELETED_NOTE_ID = "deletedNoteId";
    public static final String SELECTED_NOTE_ID = "selectedNoteId";
    private static final String AUTH_PROVIDER = "simplenote.com";
    private Simperium mSimperium;
    private Bucket<Note> mNotesBucket;
    private Bucket<Tag> mTagsBucket;
    private static Bucket<Preferences> mPreferencesBucket;

    public void onCreate() {
        super.onCreate();

        CrashUtils.initWithContext(this);

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
            mPreferencesBucket = mSimperium.bucket(new Preferences.Schema());
            mPreferencesBucket.start();

            // Every time a note changes or is deleted we need to reindex the tag counts
            mNotesBucket.addListener(new NoteTagger(mTagsBucket));
        } catch (BucketNameInvalid e) {
            throw new RuntimeException("Could not create bucket", e);
        }

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        ApplicationLifecycleMonitor applicationLifecycleMonitor = new ApplicationLifecycleMonitor();
        registerComponentCallbacks(applicationLifecycleMonitor);
        registerActivityLifecycleCallbacks(applicationLifecycleMonitor);

        AnalyticsTracker.registerTracker(new AnalyticsTrackerNosara(this));
        AnalyticsTracker.refreshMetadata(mSimperium.getUser().getEmail());
        CrashUtils.setCurrentUser(mSimperium.getUser());
    }

    @SuppressWarnings("unused")
    private boolean isFirstLaunch() {
        // NotesActivity sets this pref to false after first launch
        return PrefUtils.getBoolPref(this, PrefUtils.PREF_FIRST_LAUNCH, true);
    }

    public static boolean analyticsIsEnabled() {
        if (mPreferencesBucket == null) {
            return true;
        }

        try {
            Preferences prefs = mPreferencesBucket.get(PREFERENCES_OBJECT_KEY);
            return prefs.getAnalyticsEnabled();
        } catch (BucketObjectMissingException e) {
            return true;
        }
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

    public Bucket<Preferences> getPreferencesBucket() {
        return mPreferencesBucket;
    }

    private class ApplicationLifecycleMonitor implements Application.ActivityLifecycleCallbacks,
            ComponentCallbacks2 {
        private boolean mIsInBackground = true;

        // ComponentCallbacks
        @Override
        public void onTrimMemory(int level) {
            if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
                mIsInBackground = true;

                // Give the buckets some time to finish sync, then stop them
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!mIsInBackground) {
                            return;
                        }

                        if (mNotesBucket != null) {
                            mNotesBucket.stop();
                        }

                        if (mTagsBucket != null) {
                            mTagsBucket.stop();
                        }

                        if (mPreferencesBucket != null) {
                            mPreferencesBucket.stop();
                        }
                    }
                }, TEN_SECONDS_MILLIS);

                // Send analytics if app is in the background
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
        public void onConfigurationChanged(Configuration newConfig) {
        }

        @Override
        public void onLowMemory() {
        }

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
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }

        @Override
        public void onActivityPaused(Activity activity) {
        }

        @Override
        public void onActivityStopped(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
        }
    }
}
