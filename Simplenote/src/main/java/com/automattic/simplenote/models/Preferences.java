package com.automattic.simplenote.models;

import com.simperium.client.BucketObject;
import com.simperium.client.BucketSchema;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Preferences extends BucketObject {
    public static final String BUCKET_NAME = "preferences";
    public static final String PREFERENCES_OBJECT_KEY = "preferences-key";
    public static final int MAX_RECENT_SEARCHES = 5;

    private static final String ANALYTICS_ENABLED_KEY = "analytics_enabled";
    private static final String RECENT_SEARCHES_KEY = "recent_searches";

    private static final String SUBSCRIPTION_LEVEL_KEY = "subscription_level";
    private static final String SUBSCRIPTION_PLATFORM_KEY = "subscription_platform";
    private static final String SUBSCRIPTION_DATE_KEY = "subscription_date";

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

    public ArrayList<String> getRecentSearches() {
        Object object = getProperty(RECENT_SEARCHES_KEY);

        if (object instanceof JSONArray) {
            JSONArray recents = (JSONArray) object;
            ArrayList<String> recentsList = new ArrayList<>(recents.length());

            for (int i = 0; i < recents.length(); i++) {
                String recent = recents.optString(i);

                if (!recent.isEmpty()) {
                    recentsList.add(recent);
                }
            }

            return recentsList;
        } else {
            return new ArrayList<>();
        }
    }

    public void setAnalyticsEnabled(boolean enabled) {
        try {
            getProperties().put(ANALYTICS_ENABLED_KEY, enabled);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setRecentSearches(List<String> recents) {
        if (recents == null) {
            recents = new ArrayList<>();
        }

        setProperty(RECENT_SEARCHES_KEY, new JSONArray(recents));
    }

    public void setActiveSubscription(long purchaseTime){
        setSubscriptionPlatform(Preferences.SubscriptionPlatform.ANDROID);
        setSubscriptionLevel(Preferences.SubscriptionLevel.SUSTAINER);
        setSubscriptionDate(purchaseTime);
        save();
    }

    public void removeActiveSubscription(){
        setSubscriptionPlatform(SubscriptionPlatform.NONE);
        setSubscriptionLevel(SubscriptionLevel.NONE);
        setSubscriptionDate(null);
        save();
    }

    public SubscriptionPlatform getCurrentSubscriptionPlatform() {
        Object subscriptionPlatform = getProperty(SUBSCRIPTION_PLATFORM_KEY);
        if (subscriptionPlatform == null) {
            return null;
        }

        if (subscriptionPlatform instanceof String) {
            return SubscriptionPlatform.fromString((String) subscriptionPlatform);
        } else {
            return null;
        }
    }

    public void setSubscriptionDate(Long subscriptionDate) {
        setProperty(SUBSCRIPTION_DATE_KEY, subscriptionDate);
    }

    public void setSubscriptionPlatform(SubscriptionPlatform subscriptionPlatform) {
        setProperty(SUBSCRIPTION_PLATFORM_KEY, subscriptionPlatform.platformName);
    }

    public void setSubscriptionLevel(SubscriptionLevel subscriptionLevel) {
        setProperty(SUBSCRIPTION_LEVEL_KEY, subscriptionLevel.getName());
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

    public enum SubscriptionPlatform {
        ANDROID("android"),
        IOS("iOS"),
        WEB("WEB"),
        NONE(null);

        private final String platformName;

        SubscriptionPlatform(final String platform) {
            this.platformName = platform;
        }

        public String getName() {
            return platformName;
        }

        public static SubscriptionPlatform fromString(String platformName) {
            if (platformName != null) {
                for (SubscriptionPlatform platform : SubscriptionPlatform.values()) {
                    if (platformName.equalsIgnoreCase(platform.getName())) {
                        return platform;
                    }
                }
            }
            return null;
        }

    }

    public enum SubscriptionLevel {
        SUSTAINER("sustainer"),
        NONE(null);

        private final String subscriptionLevel;

        SubscriptionLevel(final String level) {
            this.subscriptionLevel = level;
        }

        public String getName() {
            return subscriptionLevel;
        }

        public static SubscriptionLevel fromString(String level) {
            if (level != null) {
                for (SubscriptionLevel subscriptionLevel : SubscriptionLevel.values()) {
                    if (level.equalsIgnoreCase(subscriptionLevel.getName())) {
                        return subscriptionLevel;
                    }
                }
            }
            return null;
        }
    }
}
