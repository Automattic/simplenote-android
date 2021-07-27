package com.automattic.simplenote.utils;

import android.os.Handler;
import android.os.Looper;

import com.automattic.simplenote.Simplenote;
import com.automattic.simplenote.models.Account;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;

import static com.automattic.simplenote.models.Account.KEY_EMAIL_VERIFICATION;

/**
 * Monitors account verification status by watching the `account` bucket and notifies of status and changes.
 *
 * When connecting this monitor will wait until we have received positive confirmation from the
 * server that we're sync'd before reporting the verification status because the account might
 * have (and probably was) updated from another device.
 */
public class AccountVerificationWatcher implements Bucket.OnNetworkChangeListener<Account> {
    private enum Status {
        UNKNOWN,
        UNVERIFIED,
        VERIFIED,
    }

    private final Simplenote simplenote;
    private Status currentStatus = Status.UNKNOWN;

    public AccountVerificationWatcher(Simplenote simplenote) {
        this.simplenote = simplenote;
    }

    public void clearStatus() {
        currentStatus = Status.UNKNOWN;
    }

    @Override
    public void onNetworkChange(Bucket<Account> bucket, Bucket.ChangeType type, String key) {
        // If we already know the answer, we avoid computing it again
        if (currentStatus != Status.UNKNOWN) {
            return;
        }

        String email = simplenote.getUserEmail();
        if (type != Bucket.ChangeType.INDEX || email == null) {
            return;
        }

        // When a network change of type INDEX is received, it means that the channel finished indexing the bucket, thus
        // we can check whether the account is verified or not
        try {
            Account account = bucket.get(KEY_EMAIL_VERIFICATION);
            boolean hasVerifiedEmail = account.hasVerifiedEmail(email);
            if (!hasVerifiedEmail) {
                currentStatus = Status.UNVERIFIED;

                final boolean hasSentEmail = account.hasSentEmail(email);
                Looper looper = Looper.getMainLooper();
                Handler handler = new Handler(looper);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        simplenote.showReviewVerifyAccount(hasSentEmail);
                    }
                });
            } else {
                currentStatus = Status.VERIFIED;
            }
        } catch (BucketObjectMissingException e) {
            AppLog.add(AppLog.Type.SYNC, "Account for email verification is missing");
        }
    }
}
