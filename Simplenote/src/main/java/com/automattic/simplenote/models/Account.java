package com.automattic.simplenote.models;

import com.simperium.client.BucketObject;
import com.simperium.client.BucketSchema;

import org.json.JSONObject;

public class Account extends BucketObject {
    public static final String KEY_EMAIL_VERIFICATION = "email-verification";

    private static final String BUCKET_NAME = "account";
    private static final String FIELD_EMAIL_VERIFICATION_STATUS = "status";
    private static final String FIELD_EMAIL_VERIFICATION_TOKEN = "token";

    public enum Status {
        SENT,
        VERIFIED
    }

    private Account(String key, JSONObject properties) {
        super(key, properties);
    }

    public boolean hasConfirmedAccount(String email) {
        Object token = getProperty(FIELD_EMAIL_VERIFICATION_TOKEN);

        if (token == null) {
            return false;
        } else if (((String) token).split(":", 2).length > 0) {
            String emailFromToken = ((String) token).split(":", 2)[0];
            return emailFromToken.equals(email);
        } else {
            return false;
        }
    }

    public boolean hasSentEmail() {
        Object status = getProperty(FIELD_EMAIL_VERIFICATION_STATUS);

        if (status == null) {
            return false;
        } else {
            return Status.SENT.toString().equalsIgnoreCase((String) status);
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
