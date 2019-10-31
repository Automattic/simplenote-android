package com.automattic.simplenote.models;

import com.automattic.simplenote.utils.DateTimeUtils;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObject;
import com.simperium.client.BucketSchema;
import com.simperium.client.Query;

import org.json.JSONObject;

import java.util.Calendar;

public class Search extends BucketObject {
    private static final String BUCKET_NAME = "search";
    private static final String PROPERTY_DATE = "date";
    private static final String PROPERTY_INDEX = "index";
    private static final String PROPERTY_NAME = "name";

    private Search(String key, JSONObject properties) {
        super(key, properties);
    }

    public static Query<Search> all(Bucket<Search> bucket) {
        return bucket.query().order(PROPERTY_INDEX).orderByKey();
    }

    public String getDate() {
        String date = (String) getProperty(PROPERTY_DATE);

        if (date == null) {
            date = DateTimeUtils.getISO8601FromDate(Calendar.getInstance().getTime());
        }

        return date;
    }

    public String getName() {
        String name = (String) getProperty(PROPERTY_NAME);

        if (name == null) {
            name = getSimperiumKey();
        }

        return name;
    }

    public Integer getIndex() {
        return (Integer) getProperty(PROPERTY_INDEX);
    }

    public void setDate(String date) {
        if (date == null) {
            date = DateTimeUtils.getISO8601FromDate(Calendar.getInstance().getTime());
        }

        setProperty(PROPERTY_DATE, date);
    }

    public void setIndex(Integer tagIndex) {
        if (tagIndex == null) {
            getProperties().remove(PROPERTY_INDEX);
        } else {
            setProperty(PROPERTY_INDEX, tagIndex);
        }
    }

    public void setName(String name) {
        if (name == null) {
            name = "";
        }

        setProperty(PROPERTY_NAME, name);
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

        public void update(Search tag, JSONObject properties) {
            tag.setProperties(properties);
        }
    }
}
