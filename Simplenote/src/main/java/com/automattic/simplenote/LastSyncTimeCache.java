package com.automattic.simplenote;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.automattic.simplenote.models.Note;
import com.simperium.client.Bucket;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class LastSyncTimeCache {
    public static final long DEFAULT_LAST_SYNC_TIME = -1;

    private static final String TAG = LastSyncTimeCache.class.getSimpleName();

    private final HashSet<String> mUnsyncedKeys = new HashSet<>();
    private final Set<SyncTimeListener> mListeners = new HashSet<>();
    private final SharedPreferences mPreferences;

    public LastSyncTimeCache(Context context) {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public Calendar getLastSyncTime(String key) {
        long syncInMillis = mPreferences.getLong(key, DEFAULT_LAST_SYNC_TIME);

        if (syncInMillis != DEFAULT_LAST_SYNC_TIME) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(syncInMillis);
            return calendar;
        } else {
            return null;
        }
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

    public void removeSyncTime(String entityId) {
        mPreferences.edit().remove(entityId).apply();
    }

    private void updateSyncTime(String entityId) {
        Calendar calendar = Calendar.getInstance();
        long millis = calendar.getTimeInMillis();
        mPreferences.edit().putLong(entityId, millis).apply();

        Log.d(TAG, "updateSyncTime: " + entityId + " (" + calendar.getTime() + ")");
    }

    public Bucket.Listener<Note> mNoteBucketListener = new Bucket.Listener<Note>() {
        @Override
        public void onBeforeUpdateObject(Bucket<Note> bucket, Note object) {
        }

        @Override
        public void onDeleteObject(Bucket<Note> bucket, Note object) {
            if (object != null) {
                removeSyncTime(object.getSimperiumKey());
            }
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

        @Override
        public void onNetworkChange(Bucket<Note> bucket, Bucket.ChangeType type, String entityId) {
            if (entityId != null) {
                if (type == Bucket.ChangeType.REMOVE) {
                    removeSyncTime(entityId);
                } else {
                    updateSyncTime(entityId);
                }

                notifyListeners(entityId);
            }
        }

        @Override
        public void onSaveObject(Bucket<Note> bucket, Note object) {
        }

        @Override
        public void onSyncObject(Bucket<Note> bucket, String noteId) {
            if (noteId != null) {
                updateSyncTime(noteId);
                notifyListeners(noteId);
            }
        }
    };

    interface SyncTimeListener {
        void onUpdate(String entityId, Calendar lastSyncTime, boolean isSynced);
    }
}
