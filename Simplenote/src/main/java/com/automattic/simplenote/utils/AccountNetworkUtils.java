package com.automattic.simplenote.utils;

import android.os.Build;
import android.os.LocaleList;
import android.util.Base64;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AccountNetworkUtils {
    private static final int TIMEOUT_SECS = 30;
    private static final String HTTP_SCHEME = "https";
    private static final String HTTP_HOST = "app.simplenote.com";
    private static final String ACCEPT_LANGUAGE = "Accept-Language";

    // URL endpoints
    private static final String SIMPLENOTE_DELETE_ACCOUNT = "account/request-delete";
    private static final String SIMPLENOTE_SEND_VERIFICATION_EMAIL = "verify-email/";

    private static final MediaType JSON_MEDIA_TYPE =
            MediaType.parse("application/json; charset=utf-8");

    public static void makeDeleteAccountRequest(String email, String token, final DeleteAccountRequestHandler handler) {
        OkHttpClient client = createClient();
        client.newCall(buildDeleteAccountRequest(email, token)).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                handler.onFailure();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // The delete account requests return 200 when the request was processed
                // successfully. These requests send an email to the user with instructions
                // to delete the account. This email is valid for 24h. If the user sends
                // another request for deletion and the previous request is still valid,
                // the server sends a response with code 202. We take both 200 and 202 as
                // successful responses. Both codes are handled by isSuccessful()
                if (response.isSuccessful()) {
                    handler.onSuccess();
                } else {
                    handler.onFailure();
                }
            }
        });
    }

    public static void makeSendVerificationEmailRequest(String email, final AccountVerificationEmailHandler handler) {
        byte[] data = email.getBytes(StandardCharsets.UTF_8);
        String encodedEmail = Base64.encodeToString(data, Base64.NO_WRAP);
        new OkHttpClient()
                .newBuilder()
                .readTimeout(3000, TimeUnit.SECONDS)
                .build()
                .newCall(new Request.Builder().url(buildUrl(SIMPLENOTE_SEND_VERIFICATION_EMAIL  + encodedEmail)).build())
                .enqueue(
                        new Callback() {
                            @Override
                            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                String url = call.request().url().toString();
                                handler.onFailure(e, url);
                            }

                            @Override
                            public void onResponse(@NonNull Call call, @NonNull Response response) {
                                String url = call.request().url().toString();
                                if (response.code() == 200) {
                                    handler.onSuccess(url);
                                } else {

                                    handler.onFailure(new Exception("Error code: " + response.code()), url);
                                }

                            }
                        }
                );
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
