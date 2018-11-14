package com.automattic.simplenote;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v7.preference.PreferenceManager;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.automattic.simplenote.analytics.AnalyticsTracker;
import com.automattic.simplenote.utils.PrefUtils;
import com.automattic.simplenote.utils.StrUtils;
import com.automattic.simplenote.utils.WordPressUtils;
import com.simperium.android.AndroidClient;
import com.simperium.android.LoginActivity;
import com.simperium.client.User;

import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ResponseTypeValues;

import java.util.UUID;

import static com.simperium.android.AsyncAuthClient.USER_ACCESS_TOKEN_PREFERENCE;
import static com.simperium.android.AsyncAuthClient.USER_EMAIL_PREFERENCE;

public class SignInActivity extends LoginActivity {
    private static String STATE_KEY_AUTH = "authState";

    private String mAuthState;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Ensure we are working with the expected layout from Simperium's `LoginActivity`.
        View mainView = findViewById(R.id.main);
        if (!(mainView instanceof ScrollView)) {
            return;
        }

        ScrollView scrollView = (ScrollView)mainView;
        if (scrollView.getChildAt(0) == null || !(scrollView.getChildAt(0) instanceof LinearLayout)) {
            return;
        }

        // Create and add a new view that contains the wp.com sign in button
        LinearLayout layout = (LinearLayout)scrollView.getChildAt(0);
        View signInFooter = getLayoutInflater().inflate(R.layout.sign_in_footer, layout, false);

        View footerButton = signInFooter.findViewById(R.id.wpcom_signin_button);
        footerButton.setOnClickListener(mWPSignInButtonClickListener);

        // Manually create vector drawable to support older API levels
        Drawable leftDrawable = VectorDrawableCompat.create(
                getResources(),
                R.drawable.ic_wordpress_24dp,
                null
        );
        TextView buttonTextView = signInFooter.findViewById(R.id.wpcom_signin_text);
        buttonTextView.setCompoundDrawablesWithIntrinsicBounds(leftDrawable, null, null, null);

        layout.addView(signInFooter);
    }

    private View.OnClickListener mWPSignInButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            AuthorizationRequest.Builder authRequestBuilder = WordPressUtils.getWordPressAuthorizationRequestBuilder();

            // Set a unique state value
            mAuthState = "app-" + UUID.randomUUID();
            authRequestBuilder.setState(mAuthState);

            AuthorizationRequest request = authRequestBuilder.build();
            AuthorizationService authService = new AuthorizationService(SignInActivity.this);
            Intent authIntent = authService.getAuthorizationRequestIntent(request);
            startActivityForResult(authIntent, WordPressUtils.OAUTH_ACTIVITY_CODE);

            AnalyticsTracker.track(
                    AnalyticsTracker.Stat.WPCC_BUTTON_PRESSED,
                    AnalyticsTracker.CATEGORY_USER,
                    "wpcc_button_press_signin_activity"
            );
        }
    };

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(STATE_KEY_AUTH, mAuthState);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState.containsKey(STATE_KEY_AUTH)) {
            mAuthState = savedInstanceState.getString(STATE_KEY_AUTH);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != WordPressUtils.OAUTH_ACTIVITY_CODE || data == null) {
            return;
        }

        AuthorizationResponse authResponse = AuthorizationResponse.fromIntent(data);
        AuthorizationException authException = AuthorizationException.fromIntent(data);
        if (authException != null) {
            // Error encountered
            Uri dataUri = data.getData();

            if (dataUri == null) {
                return;
            }

            if (StrUtils.isSameStr(dataUri.getQueryParameter("code"), "1")) {
                showErrorDialog(getString(R.string.wpcom_sign_in_error_unverified));
            } else {
                showErrorDialog(getString(R.string.wpcom_sign_in_error_generic));
            }
        } else if (authResponse != null) {
            // Save token and finish activity
            boolean authSuccess = WordPressUtils.processAuthResponse((Simplenote)getApplication(), authResponse, mAuthState, true);
            if (!authSuccess) {
                showErrorDialog(getString(R.string.wpcom_sign_in_error_generic));
            } else {
                AnalyticsTracker.track(
                        AnalyticsTracker.Stat.WPCC_LOGIN_SUCCEEDED,
                        AnalyticsTracker.CATEGORY_USER,
                        "wpcc_login_succeeded_signin_activity"
                );

                finish();
            }
        }
    }

    private void showErrorDialog(String message) {
        if (isFinishing() || message == null) {
            return;
        }

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle(this.getString(com.simperium.R.string.error));
        dialogBuilder.setMessage(message);
        dialogBuilder.setPositiveButton(
            this.getString(com.simperium.R.string.ok),
            new android.content.DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.dismiss();
                }
        });
        dialogBuilder.setCancelable(true);
        dialogBuilder.create().show();

        AnalyticsTracker.track(
                AnalyticsTracker.Stat.WPCC_LOGIN_FAILED,
                AnalyticsTracker.CATEGORY_USER,
                "wpcc_login_failed_signin_activity"
        );
    }
}
