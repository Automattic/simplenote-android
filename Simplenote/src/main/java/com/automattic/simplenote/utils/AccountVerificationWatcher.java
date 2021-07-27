package com.automattic.simplenote.utils;

import com.automattic.simplenote.Simplenote;
import com.automattic.simplenote.models.Account;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;

import java.util.ArrayList;
import java.util.List;

import static com.automattic.simplenote.models.Account.KEY_EMAIL_VERIFICATION;

/**
 * Monitors account verification status by watching the `account` bucket and notifies of status and changes.
 *
 * When connecting this monitor will wait until we have received positive confirmation from the
 * server that we're sync'd before reporting the verification status because the account might
 * have (and probably was) updated from another device.
 */
public class AccountVerificationWatcher implements Bucket.OnNetworkChangeListener<Account> {
    public enum Status {
        SENT_EMAIL,
        UNVERIFIED,
        VERIFIED,
    }

    public interface VerificationStateListener {
        void onUpdate(Status status);
    }

    private final Simplenote simplenote;
    private final List<VerificationStateListener> listeners = new ArrayList<>();

    public AccountVerificationWatcher(Simplenote simplenote) {
        this.simplenote = simplenote;
    }

    public void addVerificationStateListener(VerificationStateListener listener) {
        listeners.add(listener);
    }

    private void notifyListeners(Status status) {
        for (VerificationStateListener listener : listeners) {
            listener.onUpdate(status);
        }
    }

    @Override
    public void onNetworkChange(final Bucket<Account> bucket, Bucket.ChangeType type, String key) {
        // If the account is removed, the status is changed to UNVERIFIED immediately since the account will not be
        // available
       if (type == Bucket.ChangeType.REMOVE) {
            notifyListeners(Status.UNVERIFIED);
           return;
       }

        String email = simplenote.getUserEmail();
        if ((type != Bucket.ChangeType.INDEX && type != Bucket.ChangeType.MODIFY) || email == null) {
            return;
        }

        Account account;
        try {
            // When a network change of type INDEX or MODIFY is received, it means that the account bucket finished
            // indexing or there were some changes in the account. In both cases, we need to check for the account status
            account = bucket.get(KEY_EMAIL_VERIFICATION);
        } catch (BucketObjectMissingException e) {
            AppLog.add(AppLog.Type.SYNC, "Account for email verification is missing");
            return;
        }

        boolean hasVerifiedEmail = account.hasVerifiedEmail(email);
        if (hasVerifiedEmail) {
            notifyListeners(Status.VERIFIED);
        } else {
            Status statusUpdate = account.hasSentEmail(email) ? Status.SENT_EMAIL : Status.UNVERIFIED;
            notifyListeners(statusUpdate);
        }
    }
}
