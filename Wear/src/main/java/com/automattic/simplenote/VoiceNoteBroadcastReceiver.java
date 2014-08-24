package com.automattic.simplenote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

public class VoiceNoteBroadcastReceiver extends BroadcastReceiver {
    public VoiceNoteBroadcastReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();

        if (extras == null) return;

        if (extras.containsKey(android.content.Intent.EXTRA_TEXT)) {
            String voiceNote = extras.getString(android.content.Intent.EXTRA_TEXT);
            if (!TextUtils.isEmpty(voiceNote)) {
                GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context)
                        .addApi(Wearable.API)
                        .build();

                NodeApi.GetConnectedNodesResult rawNodes =
                        Wearable.NodeApi.getConnectedNodes(googleApiClient).await();

                for (Node node : rawNodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            googleApiClient, node.getId(), "new-note", voiceNote.getBytes()).await();

                    if (!result.getStatus().isSuccess()) {
                        Log.e("Simplenote", "ERROR: failed to send Message: " + result.getStatus());
                    }
                }
            }
        }
    }
}
