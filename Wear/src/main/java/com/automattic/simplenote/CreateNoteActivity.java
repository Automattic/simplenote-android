package com.automattic.simplenote;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.wearable.activity.ConfirmationActivity;
import android.text.TextUtils;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

public class CreateNoteActivity extends Activity {

    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Disable transition animations for this activity
        overridePendingTransition(0,0);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();

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
            NodeApi.GetConnectedNodesResult rawNodes =
                    Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();

            boolean isSuccess = false;

            // A Node represents a connected device.
            // Should be one device in most cases but we'll loop anyways.
            for (Node node : rawNodes.getNodes()) {
                MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                        mGoogleApiClient, node.getId(), "new-note", voiceNote.getBytes()).await();

                isSuccess = result.getStatus().isSuccess();
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
        overridePendingTransition(0,0);
        finish();
    }
}
