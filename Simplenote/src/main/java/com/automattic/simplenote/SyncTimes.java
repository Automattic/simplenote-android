package com.automattic.simplenote;

import android.util.Log;

import com.simperium.client.Bucket;
import com.simperium.client.Syncable;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SyncTimes<T extends Syncable> {
    private static final String TAG = SyncTimes.class.getSimpleName();

    private final HashMap<String, Calendar> mSyncTimes = new HashMap<>();
    private final HashSet<String> mUnsyncedKeys = new HashSet<>();
    private final Set<SyncTimeListener> mListeners = new HashSet<>();

    public SyncTimes(Map<String, Calendar> syncTimes) {
        mSyncTimes.putAll(syncTimes);
    }

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

    private void notifyUpdate(String entityId) {
        for (SyncTimeListener listener : mListeners) {
            listener.onUpdate(entityId, getLastSyncTime(entityId), isSynced(entityId));
        }
    }

    private void notifyRemove(String entityId) {
        for (SyncTimeListener listener : mListeners) {
            listener.onRemove(entityId);
        }
    }

    private void updateSyncTime(String entityId) {
        Calendar now = Calendar.getInstance();
        mSyncTimes.put(entityId, now);

        Log.d(TAG, "updateSyncTime: " + entityId + " (" + now.getTime() + ")");
    }

    public Bucket.Listener<T> bucketListener = new Bucket.Listener<T>() {
        @Override
        public void onBeforeUpdateObject(Bucket<T> bucket, T object) {
        }

        @Override
        public void onDeleteObject(Bucket<T> bucket, T object) {
            mSyncTimes.remove(object.getSimperiumKey());
            notifyRemove(object.getSimperiumKey());
        }

        @Override
        public void onLocalQueueChange(Bucket<T> bucket, Set<String> entityIds) {
            Set<String> changed = new HashSet<>();

            for (String entityId : mUnsyncedKeys) {
                if (!entityIds.contains(entityId)) {
                    changed.add(entityId);
                }
            }

            for (String entityId : entityIds) {
                if (!mUnsyncedKeys.contains(entityId)) {
                    changed.add(entityId);
                }
            }

            mUnsyncedKeys.clear();
            mUnsyncedKeys.addAll(entityIds);

            for (String entityId : changed) {
                Log.d(TAG, "updateIsSynced: " + entityId + " (" + isSynced(entityId) + ")");
                notifyUpdate(entityId);
            }
        }

        @Override
        public void onNetworkChange(Bucket<T> bucket, Bucket.ChangeType type, String entityId) {
            if (entityId == null) {
                return;
            }

            if (type == Bucket.ChangeType.REMOVE) {
                mSyncTimes.remove(entityId);
                notifyRemove(entityId);
            } else {
                updateSyncTime(entityId);
                notifyUpdate(entityId);
            }
        }

        @Override
        public void onSaveObject(Bucket<T> bucket, T object) {
        }

        @Override
        public void onSyncObject(Bucket<T> bucket, String noteId) {
            updateSyncTime(noteId);
            notifyUpdate(noteId);
        }
    };

    interface SyncTimeListener {
        void onRemove(String entityId);
        void onUpdate(String entityId, Calendar lastSyncTime, boolean isSynced);
    }
}
