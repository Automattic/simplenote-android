package com.automattic.simplenote.models;

import com.simperium.client.BucketObject;
import com.simperium.client.BucketSchema;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Search extends BucketObject {
    public static final int MAX_RECENT_SEARCHES = 5;

    private static final String BUCKET_NAME = "search";
    private static final String PROPERTY_RECENT = "recent";

    private Search(String key, JSONObject properties) {
        super(key, properties);
    }

    public ArrayList<String> getRecent() {
        JSONArray recents = (JSONArray) getProperty(PROPERTY_RECENT);

        if (recents == null) {
            recents = new JSONArray();
        }

        ArrayList<String> recentsList = new ArrayList<>(recents.length());

        for (int i = 0; i < recents.length(); i++) {
            String recent = recents.optString(i);

            if (!recent.isEmpty()) {
                recentsList.add(recent);
            }
        }

        return recentsList;
    }

    public void setRecent(List<String> recents) {
        if (recents == null) {
            recents = new ArrayList<>();
        }

        setProperty(PROPERTY_RECENT, new JSONArray(recents));
    }

    public static class Schema extends BucketSchema<Search> {
        public Schema() {
            autoIndex();
        }

        public Search build(String key, JSONObject properties) {
            return new Search(key, properties);
        }

        public String getRemoteName() {
            return Search.BUCKET_NAME;
        }

        public void update(Search search, JSONObject properties) {
            search.setProperties(properties);
        }
    }
}
