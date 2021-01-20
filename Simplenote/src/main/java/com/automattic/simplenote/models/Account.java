package com.automattic.simplenote.models;

import com.simperium.client.BucketObject;
import com.simperium.client.BucketSchema;

import org.json.JSONException;
import org.json.JSONObject;

public class Account extends BucketObject {
    public static final String KEY_EMAIL_VERIFICATION = "email-verification";

    private static final String BUCKET_NAME = "account";
    private static final String FIELD_EMAIL_VERIFICATION_STATUS = "status";
    private static final String FIELD_EMAIL_VERIFICATION_TOKEN = "token";
    private static final String FIELD_EMAIL_VERIFICATION_USERNAME = "username";

    private Account(String key, JSONObject properties) {
        super(key, properties);
    }

    /**
     * Determine if @param email address was sent a verification email.
     *
     * The <code>status</code> field is formatted as STATUS:EMAIL_ADDRESS where STATUS is the status
     * of the verification email and EMAIL_ADDRESS is the address to which the email was sent.
     *
     * e.g. sent:example@simplenote.com
     *
     * @param email {@link String} email address of user to check in status field.
     *
     * @return      {@link Boolean} true if sent; false otherwise.
     */
    public boolean hasSentEmail(String email) {
        Object status = getProperty(FIELD_EMAIL_VERIFICATION_STATUS);

        if (status == null) {
            return false;
        } else if (((String) status).split(":", 2).length > 1) {
            String emailFromStatus = ((String) status).split(":", 2)[1];
            return emailFromStatus.equals(email);
        } else {
            return false;
        }
    }

    public boolean hasVerifiedEmail(String email) {
        Object token = getProperty(FIELD_EMAIL_VERIFICATION_TOKEN);

        if (token == null) {
            return false;
        }

        try {
            JSONObject json = new JSONObject((String) token);
            Object username = json.opt(FIELD_EMAIL_VERIFICATION_USERNAME);
            return email.equals(username);
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
