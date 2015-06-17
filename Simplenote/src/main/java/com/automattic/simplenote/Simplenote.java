package com.automattic.simplenote;

import android.app.Application;
import android.content.res.Configuration;
import android.util.Log;

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.NoteCountIndexer;
import com.automattic.simplenote.models.NoteTagger;
import com.automattic.simplenote.models.Tag;
import com.automattic.simplenote.utils.WidgetUtils;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.simperium.Simperium;
import com.simperium.client.Bucket;
import com.simperium.client.BucketNameInvalid;

import org.wordpress.passcodelock.AppLockManager;

public class Simplenote extends Application {
	
	// log tag
	public static final String TAG = "Simplenote";
	
	// intent IDs
	public static final int INTENT_PREFERENCES  = 1;
	public static final int INTENT_EDIT_NOTE	= 2;

    private static final String AUTH_PROVIDER = "simplenote.com";

    public static final String DELETED_NOTE_ID = "deletedNoteId";

	private Simperium mSimperium;
	private Bucket<Note> mNotesBucket;
	private Bucket<Tag> mTagsBucket;

    private Tracker mTracker;
		
	public void onCreate(){
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
	}
		
	public Simperium getSimperium(){
		return mSimperium;
	}
	
	public Bucket<Note> getNotesBucket(){
		return mNotesBucket;
	}


    public Bucket<Tag> getTagsBucket() {
        return mTagsBucket;
    }

    // Google Analytics tracker
    public synchronized Tracker getTracker() {
        if (mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            mTracker = analytics.newTracker(BuildConfig.GOOGLE_ANALYTICS_ID);
            mTracker.enableAutoActivityTracking(true);
            mTracker.enableExceptionReporting(true);
        }

        return mTracker;
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig){
        WidgetUtils.sendBroadcastAppWigetUpdate(this);
    }
}
