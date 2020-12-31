package com.automattic.simplenote;

import android.util.Log;

import com.automattic.simplenote.models.Note;
import com.simperium.client.Bucket;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class LastSyncTimeCache {
    private HashMap<String, Calendar> mSyncTimes = new HashMap<>();
    private HashSet<String> mUnsyncedKeys = new HashSet<>();
    private Set<SyncTimeListener> mListeners = new HashSet<>();
    private static final String TAG = LastSyncTimeCache.class.getSimpleName();

    public Calendar getLastSyncTime(String key) {
        return mSyncTimes.get(key);
    }

    public boolean isSynced(String key) {
        return !mUnsyncedKeys.contains(key);
    }

    public void addListener(SyncTimeListener listener) {
        mListeners.add(listener);
    }

    public void removeListener(SyncTimeListener listener) {
        mListeners.remove(listener);
    }

    private void notifyListeners(String entityId) {
        for (SyncTimeListener listener : mListeners) {
            listener.onUpdate(entityId, getLastSyncTime(entityId), isSynced(entityId));
        }
    }

    private void updateSyncTime(String entityId) {
        Calendar now = Calendar.getInstance();
        mSyncTimes.put(entityId, now);

        Log.d(TAG, "updateSyncTime: " + entityId + " (" + now.getTime() + ")");
    }

    public Bucket.Listener<Note> listener = new Bucket.Listener<Note>() {
        @Override
        public void onSaveObject(Bucket<Note> bucket, Note object) {}

        @Override
        public void onNetworkChange(Bucket<Note> bucket, Bucket.ChangeType type, String entityId) {
            updateSyncTime(entityId);
            notifyListeners(entityId);
        }

        @Override
        public void onDeleteObject(Bucket<Note> bucket, Note object) {}

        @Override
        public void onBeforeUpdateObject(Bucket<Note> bucket, Note object) {}

        @Override
        public void onSyncObject(Bucket<Note> bucket, String noteId) {
            updateSyncTime(noteId);
            notifyListeners(noteId);
        }

        @Override
        public void onLocalQueueChange(Bucket<Note> bucket, Set<String> noteIds) {
            Set<String> changed = new HashSet<>();

            for (String noteId : mUnsyncedKeys) {
                if (!noteIds.contains(noteId)) {
                    changed.add(noteId);
                }
            }

            for (String noteId : noteIds) {
                if (!mUnsyncedKeys.contains(noteId)) {
                    changed.add(noteId);
                }
            }

            mUnsyncedKeys.clear();
            mUnsyncedKeys.addAll(noteIds);

            for (String noteId : changed) {
                Log.d(TAG, "updateIsSynced: " + noteId + " (" + isSynced(noteId) + ")");
                notifyListeners(noteId);
            }
        }
    };

    interface SyncTimeListener {
        void onUpdate(String entityId, Calendar lastSyncTime, boolean isSynced);
    }
}