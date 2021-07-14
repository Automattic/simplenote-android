package com.automattic.simplenote.utils;

import android.os.Build;
import android.os.LocaleList;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class AccountNetworkUtils {
    private static final int TIMEOUT_SECS = 30;
    private static final String HTTP_SCHEME = "https";
    private static final String HTTP_HOST = "app.simplenote.com";
    private static final String ACCEPT_LANGUAGE = "Accept-Language";

    // URL endpoints
    private static final String SIMPLENOTE_DELETE_ACCOUNT = "account/request-signup";

    private static final MediaType JSON_MEDIA_TYPE =
            MediaType.parse("application/json; charset=utf-8");

    public static void makeDeleteAccountRequest(String email, String token, Callback callback) {
        OkHttpClient client = createClient();
        client.newCall(buildDeleteAccountRequest(email, token)).enqueue(callback);
    }

    private static Request buildDeleteAccountRequest(String email, String token) {
        JSONObject json = new JSONObject();
        try {
            json.put("username", email);
            json.put("token", token);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Cannot construct json with supplied email and " +
                    "token: " + email + ", " + token);
        }
        RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, json.toString());

        return new Request.Builder()
                .url(buildUrl(SIMPLENOTE_DELETE_ACCOUNT))
                .post(body)
                .header(ACCEPT_LANGUAGE, getLanguage())
                .build();
    }

    private static HttpUrl buildUrl(String endpoint) {
        return new HttpUrl.Builder()
                .scheme(HTTP_SCHEME)
                .host(HTTP_HOST)
                .addPathSegments(endpoint)
                .build();
    }

    private static OkHttpClient createClient() {
        return new OkHttpClient()
                .newBuilder()
                .readTimeout(TIMEOUT_SECS, TimeUnit.SECONDS)
                .build();
    }

    private static String getLanguage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return LocaleList.getDefault().toLanguageTags();
        } else {
            return Locale.getDefault().getLanguage();
        }
    }
}
