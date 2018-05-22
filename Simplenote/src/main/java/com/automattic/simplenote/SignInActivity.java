package com.automattic.simplenote;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.automattic.simplenote.utils.PrefUtils;
import com.automattic.simplenote.utils.StrUtils;
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

import static com.automattic.simplenote.utils.PrefUtils.PREFS_PRIVATE_NAME;
import static com.simperium.android.AsyncAuthClient.USER_ACCESS_TOKEN_PREFERENCE;
import static com.simperium.android.AsyncAuthClient.USER_EMAIL_PREFERENCE;

public class SignInActivity extends LoginActivity {

    private static int OAUTH_ACTIVITY_CODE = 1001;
    private static String STATE_KEY_AUTH = "authState";

    static String WPCOM_OAUTH_URL = "https://public-api.wordpress.com/oauth2/";

    // TODO: Update to production Url once deployed at app.simplenote.com
    static String WPCOM_OAUTH_REDIRECT_URL = "https://wpcom-connect-dot-simple-note-hrd.appspot.com/wpcc";

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

        layout.addView(signInFooter);
    }

    private View.OnClickListener mWPSignInButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            AuthorizationServiceConfiguration serviceConfig = new AuthorizationServiceConfiguration(
                    Uri.parse(WPCOM_OAUTH_URL + "authorize"),
                    Uri.parse(WPCOM_OAUTH_URL + "token"));

            Uri redirectUri = Uri.parse(WPCOM_OAUTH_REDIRECT_URL);
            AuthorizationRequest.Builder authRequestBuilder =
                    new AuthorizationRequest.Builder(
                            serviceConfig,
                            BuildConfig.WPCOM_CLIENT_ID,
                            ResponseTypeValues.CODE,
                            redirectUri);

            // Set a unique state value
            mAuthState = "app-" + UUID.randomUUID();
            authRequestBuilder.setState(mAuthState);

            AuthorizationRequest request = authRequestBuilder.build();
            AuthorizationService authService = new AuthorizationService(SignInActivity.this);
            Intent authIntent = authService.getAuthorizationRequestIntent(request);
            startActivityForResult(authIntent, OAUTH_ACTIVITY_CODE);
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
        if (requestCode == OAUTH_ACTIVITY_CODE) {
            AuthorizationResponse authResponse = AuthorizationResponse.fromIntent(data);
            AuthorizationException authException = AuthorizationException.fromIntent(data);
            if (authResponse != null) {
                String userEmail = authResponse.additionalParameters.get("user");
                String spToken = authResponse.additionalParameters.get("token");
                String wpToken = authResponse.additionalParameters.get("wp_token");

                // Sanity checks
                if (userEmail == null || spToken == null ||
                        !StrUtils.isSameStr(authResponse.state, mAuthState)) {
                    showErrorDialog(getString(R.string.wpcom_sign_in_error_generic));
                    return;
                }

                // Manually authorize the user with Simperium
                Simplenote app = (Simplenote)getApplication();
                User user = app.getSimperium().getUser();
                user.setAccessToken(spToken);
                user.setEmail(userEmail);
                user.setStatus(User.Status.AUTHORIZED);

                // Store the user data in Simperium shared preferences
                SharedPreferences.Editor editor = AndroidClient.sharedPreferences(this).edit();
                editor.putString(USER_ACCESS_TOKEN_PREFERENCE, user.getAccessToken());
                editor.putString(USER_EMAIL_PREFERENCE, user.getEmail());
                editor.apply();

                if (wpToken != null) {
                    SharedPreferences preferences = getSharedPreferences(
                            PREFS_PRIVATE_NAME,
                            Context.MODE_PRIVATE
                    );
                    SharedPreferences.Editor spEditor = preferences.edit();
                    spEditor.putString(PrefUtils.PREF_WP_TOKEN, wpToken);
                    spEditor.apply();
                }

                finish();
            } else if (authException != null) {
                Uri dataUri = data.getData();
                if (dataUri != null) {
                    if (StrUtils.isSameStr(dataUri.getQueryParameter("code"), "1")) {
                        showErrorDialog(getString(R.string.wpcom_sign_in_error_unverified));
                    } else {
                        showErrorDialog(getString(R.string.wpcom_sign_in_error_generic));
                    }
                }
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
    }
}
