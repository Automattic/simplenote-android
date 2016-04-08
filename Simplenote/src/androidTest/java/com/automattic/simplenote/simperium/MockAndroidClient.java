package com.automattic.simplenote.simperium;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.simperium.client.AuthProvider;
import com.simperium.client.ChannelProvider;
import com.simperium.client.ClientFactory;

import com.simperium.android.GhostStore;
import com.simperium.android.PersistentStore;

/**
 * Refactoring as much of the android specific components of the client
 * and decoupling different parts of the API.
 */
public class MockAndroidClient implements ClientFactory {

    public static final String DEFAULT_DATABASE_NAME = "simperium-store";

    protected Context mContext;
    protected SQLiteDatabase mDatabase;
    public MockChannelProvider channelProvider = new MockChannelProvider();
    public MockAuthProvider authProvider = new MockAuthProvider();

    public MockAndroidClient(Context context){
        mContext = context;
        mDatabase = mContext.openOrCreateDatabase(DEFAULT_DATABASE_NAME, 0, null);
    }

    @Override
    public AuthProvider buildAuthProvider(String appId, String appSecret){
        return authProvider;
    }

    @Override
    public ChannelProvider buildChannelProvider(String appId){
        // Simperium Bucket API
        return channelProvider;
    }

    @Override
    public PersistentStore buildStorageProvider(){
        return new PersistentStore(mDatabase);
    }

    @Override
    public GhostStore buildGhostStorageProvider(){
        return new GhostStore(mDatabase);
    }

    @Override
    public MockExecutor.Immediate buildExecutor(){
        return MockExecutor.immediate();
    }

}