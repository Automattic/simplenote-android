package com.automattic.simplenote;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.automattic.simplenote.utils.AuthUtils;

import net.openid.appauth.RedirectUriReceiverActivity;

import java.util.Locale;

public class DeepLinkActivity extends AppCompatActivity {
    private static final String AUTHENTICATION_SCHEME = "auth";
    private static final String LOGIN_SCHEME = "login";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Uri uri = getIntent().getData();
        if (uri.getHost().equals(AUTHENTICATION_SCHEME)) {
            Intent intent = new Intent(this, RedirectUriReceiverActivity.class);
            intent.setData(uri);
            startActivity(intent);
        } else if (uri.getHost().equals(LOGIN_SCHEME)) {
            Intent intent = new Intent(this, NotesActivity.class);
            Simplenote app = (Simplenote) getApplication();
            String email = AuthUtils.extractEmailFromMagicLink(uri);
            if (app.isLoggedIn() &&
                !email.toLowerCase(Locale.US).equals(app.getUserEmail().toLowerCase(Locale.US))) {
                intent.putExtra(NotesActivity.KEY_ALREADY_LOGGED_IN, true);
            } else {
                AuthUtils.magicLinkLogin((Simplenote) getApplication(), uri);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        finish();
    }
}
