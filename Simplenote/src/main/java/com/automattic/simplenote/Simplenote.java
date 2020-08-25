package com.automattic.simplenote;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.automattic.simplenote.analytics.AnalyticsTracker;
import com.automattic.simplenote.analytics.AnalyticsTrackerNosara;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.NoteCountIndexer;
import com.automattic.simplenote.models.NoteTagger;
import com.automattic.simplenote.models.Preferences;
import com.automattic.simplenote.models.Tag;
import com.automattic.simplenote.utils.AppLog;
import com.automattic.simplenote.utils.AppLog.Type;
import com.automattic.simplenote.utils.CrashUtils;
import com.automattic.simplenote.utils.DisplayUtils;
import com.automattic.simplenote.utils.PrefUtils;
import com.automattic.simplenote.utils.SyncWorker;
import com.simperium.Simperium;
import com.simperium.client.Bucket;
import com.simperium.client.BucketNameInvalid;
import com.simperium.client.BucketObjectMissingException;

import org.wordpress.passcodelock.AppLockManager;

import java.util.concurrent.TimeUnit;

import static com.automattic.simplenote.models.Preferences.PREFERENCES_OBJECT_KEY;

public class Simplenote extends Application {
    public static final String DELETED_NOTE_ID = "deletedNoteId";
    public static final String SELECTED_NOTE_ID = "selectedNoteId";
    public static final String TAG = "Simplenote";
    public static final int INTENT_EDIT_NOTE = 2;
    public static final int INTENT_PREFERENCES = 1;
    public static final int ONE_MINUTE_MILLIS = 60 * 1000;  // 60 seconds
    public static final int TEN_SECONDS_MILLIS = 10 * 1000;  // 10 seconds

    private static final String AUTH_PROVIDER = "simplenote.com";
    private static final String TAG_SYNC = "sync";

    private static Bucket<Preferences> mPreferencesBucket;

    private Bucket<Note> mNotesBucket;
    private Bucket<Tag> mTagsBucket;
    private Simperium mSimperium;
    private boolean mIsInBackground = true;

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

        AppLog.add(Type.DEVICE, getDeviceInfo());
        AppLog.add(Type.ACCOUNT, getAccountInfo());
        AppLog.add(Type.LAYOUT, DisplayUtils.getDisplaySizeAndOrientation(Simplenote.this));
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

    private String getAccountInfo() {
        String email = "Email: " + (mSimperium != null && mSimperium.getUser() != null ? mSimperium.getUser().getEmail() : "?");
        String notes = "Notes: " + (mNotesBucket != null ? mNotesBucket.count() : "?");
        String tags = "Tags: " + (mTagsBucket != null ? mTagsBucket.count() : "?");
        return email + "\n" + notes + "\n" + tags + "\n\n";
    }

    private String getDeviceInfo() {
        String architecture = Build.DEVICE != null && Build.DEVICE.matches(".+_cheets|cheets_.+") ? "Chrome OS " : "Android ";
        String device = "Device: " + Build.MANUFACTURER + " " + Build.MODEL + " (" + Build.DEVICE + ")";
        String system = "System: " + architecture + Build.VERSION.RELEASE + " (" + Build.VERSION.SDK_INT + ")";
        String app = "App: Simplenote " + PrefUtils.versionInfo();
        return device + "\n" + system + "\n" + app + "\n\n";
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

    public boolean isInBackground() {
        return mIsInBackground;
    }

    private class ApplicationLifecycleMonitor implements Application.ActivityLifecycleCallbacks, ComponentCallbacks2 {
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
                            AppLog.add(Type.SYNC, "Stopped note bucket (Simplenote)");
                        }

                        if (mTagsBucket != null) {
                            mTagsBucket.stop();
                            AppLog.add(Type.SYNC, "Stopped tag bucket (Simplenote)");
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
                AppLog.add(Type.ACTION, "App closed");
            } else {
                mIsInBackground = false;
            }
        }

        @Override
        public void onConfigurationChanged(Configuration newConfig) {
            AppLog.add(Type.LAYOUT, DisplayUtils.getDisplaySizeAndOrientation(Simplenote.this));
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
                AppLog.add(Type.ACTION, "App opened");
                WorkManager.getInstance(getApplicationContext()).cancelUniqueWork(TAG_SYNC);
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
            PeriodicWorkRequest syncWorkRequest = new PeriodicWorkRequest.Builder(
                SyncWorker.class,
                PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS,
                TimeUnit.MILLISECONDS
            )
                .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.LINEAR, ONE_MINUTE_MILLIS, TimeUnit.MILLISECONDS)
                .setInitialDelay(TEN_SECONDS_MILLIS, TimeUnit.MILLISECONDS)
                .addTag(TAG_SYNC)
                .build();
            WorkManager.getInstance(getApplicationContext()).enqueueUniquePeriodicWork(
                TAG_SYNC,
                ExistingPeriodicWorkPolicy.REPLACE,
                syncWorkRequest
            );
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
