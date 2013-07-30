package com.automattic.simplenote;

import android.app.Application;
import android.util.Log;

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.NoteCountIndexer;
import com.automattic.simplenote.models.NoteTagger;
import com.automattic.simplenote.models.Tag;
import com.simperium.Simperium;
import com.simperium.client.Bucket;

import java.util.Properties;

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

        // Every time a note changes or is deleted we need to reindex the tag counts
        mNotesBucket.addListener(new NoteTagger(mTagsBucket));

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
