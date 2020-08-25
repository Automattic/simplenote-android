package com.automattic.simplenote.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.automattic.simplenote.Simplenote;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Preferences;
import com.automattic.simplenote.models.Tag;
import com.automattic.simplenote.utils.AppLog.Type;
import com.simperium.client.Bucket;

import static com.automattic.simplenote.Simplenote.TEN_SECONDS_MILLIS;

public class SyncWorker extends Worker {
    private Bucket<Note> mBucketNote;
    private Bucket<Preferences> mBucketPreference;
    private Bucket<Tag> mBucketTag;

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        Simplenote application = (Simplenote) context.getApplicationContext();
        mBucketNote = application.getNotesBucket();
        mBucketTag = application.getTagsBucket();
        mBucketPreference  = application.getPreferencesBucket();
    }

    @NonNull
    @Override
    public Result doWork() {
        AppLog.add(Type.NETWORK, NetworkUtils.getNetworkInfo(getApplicationContext()));

        if (mBucketNote != null) {
            mBucketNote.start();
            AppLog.add(Type.SYNC, "Started note bucket (SyncWorker)");
        }

        if (mBucketTag != null) {
            mBucketTag.start();
            AppLog.add(Type.SYNC, "Started tag bucket (SyncWorker)");
        }

        if (mBucketPreference != null) {
            mBucketPreference.start();
            AppLog.add(Type.SYNC, "Started preference bucket (SyncWorker)");
        }

        Log.d("SyncWorker.doWork", "Started buckets");

        new Handler(Looper.getMainLooper()).postDelayed(
            new Runnable() {
                @Override
                public void run() {
                    if (((Simplenote) getApplicationContext()).isInBackground()) {
                        if (mBucketNote != null) {
                            mBucketNote.stop();
                            AppLog.add(Type.SYNC, "Stopped note bucket (SyncWorker)");
                        }

                        if (mBucketTag != null) {
                            mBucketTag.stop();
                            AppLog.add(Type.SYNC, "Stopped tag bucket (SyncWorker)");
                        }

                        if (mBucketPreference != null) {
                            mBucketPreference.stop();
                            AppLog.add(Type.SYNC, "Stopped preference bucket (SyncWorker)");
                        }

                        Log.d("SyncWorker.doWork", "Stopped buckets");
                    }
                }
            },
            TEN_SECONDS_MILLIS
        );

        return Result.retry();
    }

    @Override
    public void onStopped() {
        super.onStopped();

        if (mBucketNote != null) {
            mBucketNote.stop();
            AppLog.add(Type.SYNC, "Stopped note bucket (SyncWorker)");
        }

        if (mBucketTag != null) {
            mBucketTag.stop();
            AppLog.add(Type.SYNC, "Stopped tag bucket (SyncWorker)");
        }

        if (mBucketPreference != null) {
            mBucketPreference.stop();
            AppLog.add(Type.SYNC, "Stopped preference bucket (SyncWorker)");
        }

        Log.d("SyncWorker.onStopped", "Stopped buckets");
    }
}
