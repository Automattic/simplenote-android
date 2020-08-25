package com.automattic.simplenote.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.automattic.simplenote.Simplenote;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Preferences;
import com.automattic.simplenote.models.Tag;
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
        mBucketNote.start();
        mBucketTag.start();
        mBucketPreference.start();

        new Handler(Looper.getMainLooper()).postDelayed(
            new Runnable() {
                @Override
                public void run() {
                    if (((Simplenote) getApplicationContext()).isInBackground()) {
                        if (mBucketNote != null) {
                            mBucketNote.stop();
                        }

                        if (mBucketTag != null) {
                            mBucketTag.stop();
                        }

                        if (mBucketPreference != null) {
                            mBucketPreference.stop();
                        }
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
        mBucketNote.stop();
        mBucketTag.stop();
        mBucketPreference.stop();
    }
}
