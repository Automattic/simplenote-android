package com.automattic.simplenote;

import java.util.Properties;

import android.app.Application;
import android.util.Log;

import com.automattic.simplenote.R;
import com.automattic.simplenote.Config;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Tag;
import com.automattic.simplenote.models.NoteTagger;
import com.automattic.simplenote.utils.NoteCountIndexer;
import com.simperium.client.Bucket;
import com.simperium.Simperium;

public class Simplenote extends Application {
	
	// log tag
	public static final String TAG = "Simplenote";
	
	// intent IDs
	public static final int INTENT_PREFERENCES  = 1;
	public static final int INTENT_BILLING		= 2;
	
	private Properties mConfig;
	private Simperium mSimperium;
	private Bucket<Note> mNotesBucket;
	private Bucket<Tag> mTagsBucket;
		
	public void onCreate(){
		super.onCreate();
		
        mSimperium = new Simperium(
            Config.simperium_app_id,
            Config.simperium_app_key,
            this
        );

		mNotesBucket = mSimperium.bucket(new Note.Schema());
        Tag.Schema tagSchema = new Tag.Schema();
        tagSchema.addIndex(new NoteCountIndexer(mNotesBucket));
		mTagsBucket = mSimperium.bucket(tagSchema);

        // Every time a note is saved the NoteTagger makes sure there's a tag in the tags bucket and creates on if necessary
        mNotesBucket.addOnSaveObjectListener(new NoteTagger(mTagsBucket));

		// Start the bucket sockets
		mNotesBucket.start();
		mTagsBucket.start();
		Log.d(Simplenote.TAG, "Simplenote launched");
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
