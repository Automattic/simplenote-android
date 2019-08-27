package com.automattic.simplenote;

import android.content.Intent;

import com.simperium.android.CredentialsActivity;

public class SimplenoteCredentialsActivity extends CredentialsActivity {
    @Override
    public void onBackPressed() {
        startActivity(new Intent(SimplenoteCredentialsActivity.this, SimplenoteAuthenticationActivity.class));
        finish();
    }
}
