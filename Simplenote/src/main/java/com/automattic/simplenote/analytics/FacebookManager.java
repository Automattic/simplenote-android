package com.automattic.simplenote.analytics;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.automattic.simplenote.BuildConfig;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.IOException;

import cz.msebera.android.httpclient.Header;

/**
 * Pings Facebook endpoint for tracking installs from ads
 * https://developers.facebook.com/docs/marketing-api/mobile-conversions-endpoint/v2.5
 */
public class FacebookManager {
    private static final Uri ATTRIBUTION_ID_CONTENT_URI = Uri.parse("content://com.facebook.katana.provider.AttributionIdProvider");
    private static final String ATTRIBUTION_ID_COLUMN_NAME = "aid";

    private static String getAttributionId(ContentResolver contentResolver) {
        String[] projection = {ATTRIBUTION_ID_COLUMN_NAME};
        Cursor c = contentResolver.query(ATTRIBUTION_ID_CONTENT_URI, projection, null, null, null);
        if (c == null || !c.moveToFirst()) {
            return null;
        }
        String attributionId = c.getString(c.getColumnIndex(ATTRIBUTION_ID_COLUMN_NAME));
        c.close();

        return attributionId;
    }

    public static void reportInstallIfNecessary(Context context) {
        if (context == null) return;

        new GetAdvertiserIdTask().execute(context);
    }

    private static class GetAdvertiserIdTask extends AsyncTask<Context, Void, String> {
        private Context mContext;

        @Override
        protected String doInBackground(Context... contexts) {
            mContext = contexts[0];

            AdvertisingIdClient.Info idInfo;
            try {
                idInfo = AdvertisingIdClient.getAdvertisingIdInfo(mContext);
            } catch (GooglePlayServicesNotAvailableException | GooglePlayServicesRepairableException | IOException e) {
                return null;
            }

            String advertiserId;
            try {
                advertiserId = idInfo.getId();
            } catch (NullPointerException e) {
                return null;
            }

            return advertiserId;
        }

        @Override
        protected void onPostExecute(String advertiserId) {
            // If the device has an ad id, let's attempt to send to Facebook
            if (!TextUtils.isEmpty(advertiserId)) {
                sendReportForAdvertiserId(advertiserId, mContext);
            }
        }
    }

    private static void sendReportForAdvertiserId(String advertiserId, Context context) {
        if (TextUtils.isEmpty(advertiserId) || context == null) return;

        String attributionId = getAttributionId(context.getContentResolver());
        if (!TextUtils.isEmpty(attributionId)) {
            RequestParams requestParams = new RequestParams();
            requestParams.put("event", "MOBILE_APP_INSTALL");
            requestParams.put("advertiser_id", advertiserId);
            requestParams.put("attribution", attributionId);
            requestParams.put("advertiser_tracking_enabled", "1");
            requestParams.put("application_tracking_enabled", "0");

            new AsyncHttpClient().post(getFacebookEndpoint(), requestParams, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    // woot
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    // oh well
                }
            });
        }
    }

    private static String getFacebookEndpoint() {
        return String.format("https://graph.facebook.com/%s/activities", BuildConfig.FACEBOOK_APP_ID);
    }
}
