package com.automattic.simplenote;

import java.io.InputStream;
import java.util.Properties;

import android.app.Application;
import android.util.Log;

import com.automattic.simplenote.R;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Tag;
import com.automattic.simplenote.models.NoteTagger;
import com.simperium.client.Bucket;
import com.simperium.Simperium;

public class Simplenote extends Application {
	
	// log tag
	public static final String TAG = "Simplenote";
	
	private static final String SIMPERIUM_APP_CONFIG_KEY = "simperium.appid";
	private static final String SIMPERIUM_KEY_CONFIG_KEY = "simperium.key";
	
	// intent IDs
	public static final int INTENT_PREFERENCES  = 1;
	public static final int INTENT_BILLING		= 2;
	
	private Properties mConfig;
	private Simperium mSimperium;
	private Bucket<Note> mNotesBucket;
	private Bucket<Tag> mTagsBucket;
		
	public void onCreate(){
		super.onCreate();
		Properties mConfig = getConfigProperties();
		
		mSimperium = new Simperium(
			mConfig.getProperty(SIMPERIUM_APP_CONFIG_KEY),
			mConfig.getProperty(SIMPERIUM_KEY_CONFIG_KEY),
			getApplicationContext()
		);

		mNotesBucket = mSimperium.bucket(new Note.Schema());
		mTagsBucket = mSimperium.bucket(new Tag.Schema());
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
	
	protected Properties getConfigProperties(){
		if (mConfig == null) {
			mConfig = new Properties();
			InputStream stream = getResources().openRawResource(R.raw.config);
			try {
				mConfig.load(stream);				
			} catch(java.io.IOException e){
				mConfig = null;
				Log.e(TAG, "Could not load config", e);
			}
		}
		return mConfig;
	}
	
}
