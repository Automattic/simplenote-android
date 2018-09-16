package com.automattic.simplenote;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.wearable.activity.ConfirmationActivity;
import android.text.TextUtils;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.List;

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
                new SendNoteTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, voiceNote);
            } else {
                showConfirmationActivityAndFinish(false);
            }
        } else {
            showConfirmationActivityAndFinish(false);
        }
    }


    private class SendNoteTask extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... voiceNotes) {
            if (voiceNotes.length == 0) {
                return false;
            }

            String voiceNote = voiceNotes[0];
            Task<List<Node>> rawNodes =
                    Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();

            boolean isSuccess = false;

            // A Node represents a connected device.
            // Should be one device in most cases but we'll loop anyways.
            for (Node node : rawNodes.getResult()) {
                Task<Integer> result = Wearable.getMessageClient(getApplicationContext()).sendMessage(
                        node.getId(), "new-note", voiceNote.getBytes());

                isSuccess = result.isSuccessful();
            }

            return isSuccess;
        }

        @Override
        protected void onPostExecute(Boolean isSuccess) {
            showConfirmationActivityAndFinish(isSuccess);
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
