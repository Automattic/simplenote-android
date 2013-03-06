package com.automattic.simplenote;

import com.automattic.simplenote.models.*;

import java.io.InputStream;
import java.util.Properties;

import com.simperium.client.*;
import com.simperium.client.storage.MemoryStore;

import android.util.Log;
import android.app.Application;

public class Simplenote extends Application implements User.AuthenticationListener {
	
	public static final String TAG="Simplenote";
	
	private static final String SIMPERIUM_APP_CONFIG_KEY="simperium.appid";
	private static final String SIMPERIUM_KEY_CONFIG_KEY="simperium.key";
	
	private Properties mConfig;
	private Simperium mSimperium;
	private Bucket<Note> mNotesBucket;
	private Bucket<Tag> mTagsBucket;
	private NoteDB mNoteDB;
	
	private StorageProvider mStorageProvider = new MemoryStore();
		
	public void onCreate(){
		super.onCreate();
		Properties mConfig = getConfigProperties();
		mNoteDB = new NoteDB(getApplicationContext());
		
		mSimperium = new Simperium(
			mConfig.getProperty(SIMPERIUM_APP_CONFIG_KEY),
			mConfig.getProperty(SIMPERIUM_KEY_CONFIG_KEY),
			getApplicationContext(),
			mNoteDB.getSimperiumStore(),
			this
		);

		mNotesBucket = mSimperium.bucket(Note.BUCKET_NAME, new Note.Schema());
		mTagsBucket = mSimperium.bucket(Tag.BUCKET_NAME, new Tag.Schema());

		mNotesBucket.start();
		mTagsBucket.start();
	}
	
	public NoteDB getNoteDB(){
		return mNoteDB;
	}
	
	public StorageProvider getStorageProvider(){
		return mStorageProvider;
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
