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
        Object isEnabled = getProperty(ANALYTICS_ENABLED_KEY);
        if (isEnabled == null) {
            return true;
        }

        if (isEnabled instanceof Boolean) {
            return (Boolean) isEnabled;
        } else {
            // Simperium-iOS sets booleans as integer values (0 or 1)
            return isEnabled instanceof Integer && ((Integer) isEnabled) > 0;
        }
    }

    public void setAnalyticsEnabled(boolean enabled) {
        try {
            getProperties().put(ANALYTICS_ENABLED_KEY, enabled);
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

        public void update(Preferences prefs, JSONObject properties) {
            prefs.setProperties(properties);
        }
    }
}
