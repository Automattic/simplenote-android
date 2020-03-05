package com.automattic.simplenote;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputLayout;
import com.simperium.android.CredentialsActivity;

import static com.simperium.android.AuthenticationActivity.EXTRA_IS_LOGIN;

public class SimplenoteCredentialsActivity extends CredentialsActivity {
    @Override
    public void onBackPressed() {
        startActivity(new Intent(SimplenoteCredentialsActivity.this, SimplenoteAuthenticationActivity.class));
        finish();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (BuildConfig.DEBUG && getIntent().getBooleanExtra(EXTRA_IS_LOGIN, false)) {
            EditText inputEmail = ((TextInputLayout) findViewById(R.id.input_email)).getEditText();
            EditText inputPassword = ((TextInputLayout) findViewById(R.id.input_password)).getEditText();

            if (inputEmail != null && inputPassword != null) {
                inputEmail.setText(BuildConfig.LOGIN_EMAIL);
                inputPassword.setText(BuildConfig.LOGIN_PASSWORD);
            }
        }
    }
}
