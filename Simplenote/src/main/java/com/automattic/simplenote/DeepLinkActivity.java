package com.automattic.simplenote;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import net.openid.appauth.RedirectUriReceiverActivity;

public class DeepLinkActivity extends AppCompatActivity {
    private static final String AUTHENTICATION_SCHEME = "auth";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Uri uri = getIntent().getData();
        if (uri.getHost().equals(AUTHENTICATION_SCHEME)) {
            Intent intent = new Intent(this, RedirectUriReceiverActivity.class);
            intent.setData(uri);
            startActivity(intent);
        }
        finish();
    }
}
