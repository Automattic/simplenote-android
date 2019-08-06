package com.automattic.simplenote;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.wear.activity.ConfirmationActivity;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class CreateNoteActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Disable transition animations for this activity
        overridePendingTransition(0, 0);

        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            showConfirmationActivityAndFinish(false);
            return;
        }

        if (extras.containsKey(android.content.Intent.EXTRA_TEXT)) {
            String voiceNote = extras.getString(android.content.Intent.EXTRA_TEXT);
            if (!TextUtils.isEmpty(voiceNote)) {
                new SendNoteTask(this)
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, voiceNote);
            } else {
                showConfirmationActivityAndFinish(false);
            }
        } else {
            showConfirmationActivityAndFinish(false);
        }
    }


    private static class SendNoteTask extends AsyncTask<String, Void, Boolean> {
        WeakReference<CreateNoteActivity> weakActivity;

        SendNoteTask(CreateNoteActivity activity) {
            weakActivity = new WeakReference<>(activity);
        }

        @Override
        protected Boolean doInBackground(String... voiceNotes) {
            Activity activity = weakActivity.get();
            if (voiceNotes.length == 0 || activity == null) {
                return false;
            }

            boolean isSuccess = false;

            String voiceNote = voiceNotes[0];
            Task<List<Node>> rawNodes =
                    Wearable.getNodeClient(activity.getApplicationContext()).getConnectedNodes();

            try {
                List<Node> nodes = Tasks.await(rawNodes);

                // A Node represents a connected device.
                // Should be one device in most cases but we'll loop anyways.
                for (Node node : nodes) {
                    try {
                        Task<Integer> sendMessage = Wearable.getMessageClient(
                                activity.getApplicationContext()).sendMessage(
                                node.getId(), "new-note", voiceNote.getBytes());
                        Tasks.await(sendMessage);

                        isSuccess = sendMessage.isSuccessful();
                    } catch (ExecutionException | InterruptedException e) {
                        Log.e("Create Note", "Failed to send new-note messages: " + e.getMessage(), e);
                    }

                }
            } catch (ExecutionException | InterruptedException e) {
                Log.e("Create Note", "Failed to get connected Nodes: " + e.getMessage(), e);
            }

            return isSuccess;
        }

        @Override
        protected void onPostExecute(Boolean isSuccess) {
            CreateNoteActivity activity = weakActivity.get();
            if (activity != null) {
                activity.showConfirmationActivityAndFinish(isSuccess);
            }
        }
    }

    // Launch a ConfirmationActivity, which shows a confirmation animation with message
    private void showConfirmationActivityAndFinish(Boolean isSuccess) {
        String message = isSuccess ? getString(R.string.noted) : getString(R.string.error);
        int animationType = isSuccess ? ConfirmationActivity.SUCCESS_ANIMATION : ConfirmationActivity.FAILURE_ANIMATION;

        Intent confirmationIntent = new Intent(this, ConfirmationActivity.class);
        confirmationIntent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, animationType);
        confirmationIntent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, message);

        startActivity(confirmationIntent);
        overridePendingTransition(0, 0);
        finish();
    }
}
