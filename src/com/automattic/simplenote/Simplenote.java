package com.automattic.simplenote;

import com.automattic.simplenote.models.*;

import java.io.InputStream;
import java.util.Properties;

import com.simperium.client.*;
import com.simperium.client.storage.MemoryStore;

import android.util.Log;
import android.app.Application;

public class Simplenote extends Application implements User.AuthenticationListener {
	
	public static final String TAG="SimpleNote";
	
	private static final String SIMPERIUM_APP_CONFIG_KEY="simperium.appid";
	private static final String SIMPERIUM_KEY_CONFIG_KEY="simperium.key";
	
	private Properties config;
	private Simperium simperium;
	private Bucket<Note> notesBucket;
	private Bucket<Tag> tagsBucket;
	
	private StorageProvider mStorageProvider = new MemoryStore();
		
	public void onCreate(){
		super.onCreate();
		Properties config = getConfigProperties();
		simperium = new Simperium(
			config.getProperty(SIMPERIUM_APP_CONFIG_KEY),
			config.getProperty(SIMPERIUM_KEY_CONFIG_KEY),
			getApplicationContext(),
			mStorageProvider,
			this
		);
		
		notesBucket = simperium.bucket(Note.BUCKET_NAME, new Note.Schema());
		tagsBucket = simperium.bucket(Tag.BUCKET_NAME, new Tag.Schema());

		notesBucket.start();
		tagsBucket.start();
	}
	
	public StorageProvider getStorageProvider(){
		return mStorageProvider;
	}
	
	public Simperium getSimperium(){
		return simperium;
	}
	
	public Bucket<Note> getNotesBucket(){
		return notesBucket;
	}
	
	public Bucket<Tag> getTagsBucket(){
		return tagsBucket;
	}
	
	protected Properties getConfigProperties(){
		if (config == null) {
			config = new Properties();
			InputStream stream = getResources().openRawResource(R.raw.config);
			try {
				config.load(stream);				
			} catch(java.io.IOException e){
				config = null;
				Log.e(TAG, "Could not load config", e);
			}
		}
		return config;
	}
	
}
