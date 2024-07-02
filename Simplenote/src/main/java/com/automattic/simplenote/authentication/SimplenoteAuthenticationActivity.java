package com.automattic.simplenote.authentication;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextThemeWrapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.ViewModelProvider;

import com.automattic.simplenote.R;
import com.automattic.simplenote.Simplenote;
import com.automattic.simplenote.analytics.AnalyticsTracker;
import com.automattic.simplenote.utils.IntentUtils;
import com.automattic.simplenote.utils.StrUtils;
import com.automattic.simplenote.utils.WordPressUtils;
import com.automattic.simplenote.viewmodels.MagicLinkUiState;
import com.automattic.simplenote.viewmodels.CompleteMagicLinkViewModel;
import com.simperium.android.AuthenticationActivity;

import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;

import java.util.UUID;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SimplenoteAuthenticationActivity extends AuthenticationActivity {
    private static String STATE_AUTH_STATE = "STATE_AUTH_STATE";

    public static final String KEY_IS_MAGIC_LINK = "KEY_IS_MAGIC_LINK";
    public static final String KEY_MAGIC_LINK_AUTH_KEY = "KEY_MAGIC_LINK_AUTH_KEY";
    public static final String KEY_MAGIC_LINK_AUTH_CODE = "KEY_MAGIC_LINK_AUTH_CODE";

    private String mAuthState;

    @Nullable
    private CompleteMagicLinkViewModel completeMagicLinkViewModel = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        final boolean isMagicLink = intent.getBooleanExtra(KEY_IS_MAGIC_LINK, false);
        if (isMagicLink) {
            completeMagicLinkViewModel = new ViewModelProvider(this).get(CompleteMagicLinkViewModel.class);
            completeMagicLinkViewModel.getMagicLinkUiState().observe(this, state -> {
                if (MagicLinkUiState.Success.INSTANCE.equals(state)) {
                    final Intent notesIntent = IntentUtils.maybeAliasedIntent(this.getApplicationContext());
                    notesIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION & (Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
                    startActivity(notesIntent);
                    finish();
                } else if (state instanceof MagicLinkUiState.Error) {
                    showDialogError(((MagicLinkUiState.Error) state).getMessageRes());
                }
            });
            final String authKey = intent.getStringExtra(KEY_MAGIC_LINK_AUTH_KEY);
            final String authCode = intent.getStringExtra(KEY_MAGIC_LINK_AUTH_CODE);
            completeMagicLinkViewModel.completeLogin(authKey, authCode);
        }
    }

    @Override
    protected void buttonSignupClicked() {
        Intent intent = new Intent(SimplenoteAuthenticationActivity.this, SimplenoteSignupActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != WordPressUtils.OAUTH_ACTIVITY_CODE || data == null) {
            return;
        }

        AuthorizationResponse authorizationResponse = AuthorizationResponse.fromIntent(data);
        AuthorizationException authorizationException = AuthorizationException.fromIntent(data);

        if (authorizationException != null) {
            Uri dataUri = data.getData();

            if (dataUri == null) {
                return;
            }

            if (StrUtils.isSameStr(dataUri.getQueryParameter("code"), "1")) {
                showDialogError(R.string.wpcom_log_in_error_unverified);
            } else {
                showDialogError(R.string.wpcom_log_in_error_generic);
            }
        } else if (authorizationResponse != null) {
            // Save token and finish activity.
            boolean authSuccess = WordPressUtils.processAuthResponse((Simplenote) getApplication(), authorizationResponse, mAuthState, true);

            if (!authSuccess) {
                showDialogError(R.string.wpcom_log_in_error_generic);
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

    @Override
    public void onLoginSheetCanceled() {
        super.onLoginSheetCanceled();
    }

    @Override
    public void onLoginSheetEmailClicked() {
        Intent intent = new Intent(SimplenoteAuthenticationActivity.this, SimplenoteSignupActivity.class);
        intent.putExtra(SimplenoteSignupActivity.KEY_IS_LOGIN, true);
        startActivity(intent);
    }

    @Override
    public void onLoginSheetOtherClicked() {
        AuthorizationRequest.Builder authRequestBuilder = WordPressUtils.getWordPressAuthorizationRequestBuilder();

        // Set unique state value.
        mAuthState = "app-" + UUID.randomUUID();
        authRequestBuilder.setState(mAuthState);

        AuthorizationRequest request = authRequestBuilder.build();
        AuthorizationService authService = new AuthorizationService(SimplenoteAuthenticationActivity.this);
        Intent authIntent = authService.getAuthorizationRequestIntent(request);
        startActivityForResult(authIntent, WordPressUtils.OAUTH_ACTIVITY_CODE);

        AnalyticsTracker.track(
            AnalyticsTracker.Stat.WPCC_BUTTON_PRESSED,
            AnalyticsTracker.CATEGORY_USER,
            "wpcc_button_press_signin_activity"
        );
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(STATE_AUTH_STATE, mAuthState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState.containsKey(STATE_AUTH_STATE)) {
            mAuthState = savedInstanceState.getString(STATE_AUTH_STATE);
        }
    }

    private void showDialogError(@StringRes int message) {
        if (isFinishing() || message == 0) {
            return;
        }

        Context context = new ContextThemeWrapper(SimplenoteAuthenticationActivity.this, getTheme());
        new AlertDialog.Builder(context)
            .setTitle(R.string.simperium_dialog_title_error)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show();

        AnalyticsTracker.track(
            AnalyticsTracker.Stat.WPCC_LOGIN_FAILED,
            AnalyticsTracker.CATEGORY_USER,
            "wpcc_login_failed_signin_activity"
        );
    }
}
