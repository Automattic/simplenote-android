package com.automattic.simplenote.models;

import com.simperium.client.BucketObject;
import com.simperium.client.BucketSchema;

import org.json.JSONException;
import org.json.JSONObject;

public class Account extends BucketObject {
    public static final String KEY_EMAIL_VERIFICATION = "email-verification";

    private static final String BUCKET_NAME = "account";
    private static final String FIELD_EMAIL_VERIFICATION_SENT_TO = "sent_to";
    private static final String FIELD_EMAIL_VERIFICATION_TOKEN = "token";
    private static final String FIELD_EMAIL_VERIFICATION_USERNAME = "username";

    private Account(String key, JSONObject properties) {
        super(key, properties);
    }

    public boolean hasSentEmail(String email) {
        return email.equalsIgnoreCase((String) getProperty(FIELD_EMAIL_VERIFICATION_SENT_TO));
    }

    public boolean hasVerifiedEmail(String email) {
        Object token = getProperty(FIELD_EMAIL_VERIFICATION_TOKEN);

        if (token == null) {
            return false;
        }

        try {
            JSONObject json = new JSONObject((String) token);
            Object username = json.opt(FIELD_EMAIL_VERIFICATION_USERNAME);
            return email.equalsIgnoreCase((String) username);
        } catch (JSONException exception) {
            return false;
        }
    }

    public static class Schema extends BucketSchema<Account> {
        public Schema() {
            autoIndex();
        }

        public Account build(String key, JSONObject properties) {
            return new Account(key, properties);
        }

        public String getRemoteName() {
            return BUCKET_NAME;
        }

        public void update(Account account, JSONObject properties) {
            account.setProperties(properties);
        }
    }
}
