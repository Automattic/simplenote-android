package com.automattic.simplenote;

import android.app.Application;

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.NoteCountIndexer;
import com.automattic.simplenote.models.NoteTagger;
import com.automattic.simplenote.models.Tag;
import com.simperium.android.Simperium;
import com.simperium.android.Bucket;
import com.simperium.client.BucketNameInvalid;

import org.wordpress.passcodelock.AppLockManager;

public class Simplenote extends Application {
	
	// log tag
	public static final String TAG = "Simplenote";
	
	// intent IDs
	public static final int INTENT_PREFERENCES  = 1;
	public static final int INTENT_EDIT_NOTE	= 2;

    private static final String AUTH_PROVIDER = "simplenote.com";

    public static final String CUSTOM_FONT_PATH = "fonts/SourceSansPro-Regular.ttf";

    public static final String DELETED_NOTE_ID = "deletedNoteId";

	private Simperium mSimperium;
	private Bucket<Note> mNotesBucket;
	private Bucket<Tag> mTagsBucket;
		
	public void onCreate(){
		super.onCreate();

        AppLockManager.getInstance().enableDefaultAppLockIfAvailable(this);

        mSimperium = Simperium.initializeClient(
            this,
            BuildConfig.SIMPERIUM_APP_ID,
            BuildConfig.SIMPERIUM_APP_KEY
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
	
	public Bucket<Tag> getTagsBucket(){
		return mTagsBucket;
	}
	
}
