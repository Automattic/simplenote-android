package com.automattic.simplenote.models;

import com.simperium.client.BucketObject;
import com.simperium.client.BucketSchema;

import org.json.JSONException;
import org.json.JSONObject;

public class Preferences extends BucketObject {
    public static String PREFERENCES_OBJECT_KEY = "preferences-key";

    private static final String BUCKET_NAME = "preferences";
    private static final String ANALYTICS_ENABLED_KEY = "analytics_enabled";

    private Preferences(String key, JSONObject properties) {
        super(key, properties);
    }

    public boolean getAnalyticsEnabled() {
        try {
            return getProperties().getInt(ANALYTICS_ENABLED_KEY) > 0;
        } catch (JSONException e) {
            e.printStackTrace();
            return true;
        }
    }

    public void setAnalyticsEnabled(boolean enabled) {
        try {
            getProperties().put(ANALYTICS_ENABLED_KEY, enabled ? 1 : 0);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static class Schema extends BucketSchema<Preferences> {

        public Schema() {
            autoIndex();
        }

        public String getRemoteName() {
            return BUCKET_NAME;
        }

        public Preferences build(String key, JSONObject properties) {
            return new Preferences(key, properties);
        }

        public void update(Preferences Preferences, JSONObject properties) {
            Preferences.setProperties(properties);
        }
    }
}
