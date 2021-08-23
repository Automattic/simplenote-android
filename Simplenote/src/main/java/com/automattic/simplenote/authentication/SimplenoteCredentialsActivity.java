package com.automattic.simplenote.authentication;

import static com.simperium.android.AuthenticationActivity.EXTRA_IS_LOGIN;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.automattic.simplenote.BuildConfig;
import com.automattic.simplenote.R;
import com.automattic.simplenote.utils.AccountNetworkUtils;
import com.automattic.simplenote.utils.AccountVerificationEmailHandler;
import com.automattic.simplenote.utils.AppLog;
import com.google.android.material.textfield.TextInputLayout;
import com.simperium.android.CredentialsActivity;
import com.simperium.client.AuthException;
import com.simperium.client.AuthProvider;
import com.simperium.client.AuthResponseListener;
import com.simperium.client.User;
import com.simperium.util.Logger;

public class SimplenoteCredentialsActivity extends CredentialsActivity {
    AuthResponseListener mAuthListener = new AuthResponseListener() {
        @Override
        public void onFailure(final User user, final AuthException error) {
            runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            switch (error.failureType) {
                                case EXISTING_ACCOUNT:
                                    showDialogErrorExistingAccount();
                                    break;
                                case COMPROMISED_PASSWORD:
                                    showCompromisedPasswordDialog();
                                    break;
                                case UNVERIFIED_ACCOUNT:
                                    showUnverifiedAccountDialog();
                                    break;
                                case INVALID_ACCOUNT:
                                default:
                                    showDialogError(getString(
                                            mIsLogin ?
                                                    com.simperium.R.string.simperium_dialog_message_login :
                                                    com.simperium.R.string.simperium_dialog_message_signup
                                    ));
                            }

                            Logger.log(error.getMessage(), error);
                        }
                    }
            );
        }

        @Override
        public void onSuccess(final User user, final String userId, final String token, final AuthProvider provider) {
            handleResponseSuccess(user, userId, token, provider);
        }
    };

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

    @Override
    protected AuthResponseListener getAuthListener() {
        return mAuthListener;
    }

    void showUnverifiedAccountDialog() {
        hideDialogProgress();

        final Context context = new ContextThemeWrapper(SimplenoteCredentialsActivity.this, getTheme());
        new AlertDialog.Builder(context)
                .setTitle(com.simperium.R.string.simperium_account_verification)
                .setMessage(com.simperium.R.string.simperium_account_verification_message)
                .setNegativeButton(com.simperium.R.string.simperium_okay, null)
                .setPositiveButton("Resend Verification Email",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                sendVerificationEmail();
                            }
                        }
                )
                .show();
    }

    private void sendVerificationEmail() {
        AccountNetworkUtils.makeSendVerificationEmailRequest(getEmail(), new AccountVerificationEmailHandler() {
            @Override
            public void onSuccess(@NonNull String url) {
                AppLog.add(AppLog.Type.AUTH, "Email sent (200 - " + url + ")");
            }

            @Override
            public void onFailure(@NonNull Exception e, @NonNull String url) {
                AppLog.add(AppLog.Type.AUTH, "Verification email error (" + e.getMessage() + " - " + url + ")");
            }
        });
    }
}
