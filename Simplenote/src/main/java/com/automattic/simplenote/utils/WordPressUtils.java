// Helper for working with the WordPress.com OAuth and REST API
package com.automattic.simplenote.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;

import androidx.preference.PreferenceManager;

import com.automattic.simplenote.BuildConfig;
import com.automattic.simplenote.Simplenote;
import com.simperium.android.AndroidClient;
import com.simperium.client.User;

import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ResponseTypeValues;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import static com.simperium.android.AsyncAuthClient.USER_ACCESS_TOKEN_PREFERENCE;
import static com.simperium.android.AsyncAuthClient.USER_EMAIL_PREFERENCE;

public class WordPressUtils {
    public static int OAUTH_ACTIVITY_CODE = 1001;
    private static String WP_API_URL = "https://public-api.wordpress.com/rest/v1.1/";
    private static String WPCOM_OAUTH_URL = "https://public-api.wordpress.com/oauth2/";
    private static String WPCOM_OAUTH_REDIRECT_URL = "https://app.simplenote.com/wpcc";


    // Returns true if a WordPress.com token exists in preferences
    public static boolean hasWPToken(Context context) {
        if (context == null) {
            return false;
        }


        return !TextUtils.isEmpty(PrefUtils.getStringPref(context, PrefUtils.PREF_WP_TOKEN));
    }

    // Publish a post to a WordPress site via the WordPress.com REST API
    public static void publishPost(Context context, String url, String title, String content, String status, Callback callback) {
        if (!hasWPToken(context)) {
            return;
        }

        String wpToken = PrefUtils.getStringPref(context, PrefUtils.PREF_WP_TOKEN);

        OkHttpClient client = new OkHttpClient().newBuilder().readTimeout(30, TimeUnit.SECONDS).build();
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("title", title)
                .addFormDataPart("content", content)
                .addFormDataPart("status", status)
                .build();
        Request request = new Request.Builder()
                .url(WP_API_URL + String.format(Locale.ENGLISH, "sites/%s/posts/new", url))
                .header("Authorization", String.format(Locale.ENGLISH, "BEARER %s", wpToken))
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(callback);
    }

    // Returns a list of sites that a user has on WordPress.com
    public static void getSites(Context context, Callback callback) {
        if (!hasWPToken(context)) {
            return;
        }

        String wpToken = PrefUtils.getStringPref(context, PrefUtils.PREF_WP_TOKEN);

        OkHttpClient client = new OkHttpClient().newBuilder().readTimeout(30, TimeUnit.SECONDS).build();
        Request request = new Request.Builder()
                    .url(WP_API_URL + "me/sites")
                    .header("Authorization", String.format(Locale.ENGLISH, "BEARER %s", wpToken))
                    .get()
                    .build();

        client.newCall(request).enqueue(callback);
    }

    // Builds a new WordPress.com authorization request
    public static AuthorizationRequest.Builder getWordPressAuthorizationRequestBuilder() {
        AuthorizationServiceConfiguration serviceConfig = new AuthorizationServiceConfiguration(
                Uri.parse(WPCOM_OAUTH_URL + "authorize?scope=global"),
                Uri.parse(WPCOM_OAUTH_URL + "token"));

        Uri redirectUri = Uri.parse(WPCOM_OAUTH_REDIRECT_URL);

        return new AuthorizationRequest.Builder(
                serviceConfig,
                BuildConfig.WPCOM_CLIENT_ID,
                ResponseTypeValues.CODE,
                redirectUri);
    }

    // Process an authorization response from WordPress.com OAuth.
    // Saves tokens and authorizes the local Simperium user if requested
    public static boolean processAuthResponse(Simplenote app, AuthorizationResponse authResponse, String authState, boolean shouldAuthSimperiumUser) {
        String userEmail = authResponse.additionalParameters.get("user");
        String spToken = authResponse.additionalParameters.get("token");
        String wpToken = authResponse.additionalParameters.get("wp_token");

        // Sanity checks
        if (userEmail == null || spToken == null ||
                !StrUtils.isSameStr(authResponse.state, authState)) {
            return false;
        }

        if (wpToken != null) {
            SharedPreferences.Editor appEditor = PreferenceManager.getDefaultSharedPreferences(app.getApplicationContext()).edit();
            appEditor.putString(PrefUtils.PREF_WP_TOKEN, wpToken);
            appEditor.apply();
        }

        if (!shouldAuthSimperiumUser) {
            return true;
        }

        // Manually authorize the user with Simperium
        User user = app.getSimperium().getUser();
        user.setAccessToken(spToken);
        user.setEmail(userEmail);
        user.setStatus(User.Status.AUTHORIZED);

        // Store the user data in Simperium shared preferences
        SharedPreferences.Editor editor = AndroidClient.sharedPreferences(app.getApplicationContext()).edit();
        editor.putString(USER_ACCESS_TOKEN_PREFERENCE, user.getAccessToken());
        editor.putString(USER_EMAIL_PREFERENCE, user.getEmail());
        editor.apply();

        return true;
    }
}
