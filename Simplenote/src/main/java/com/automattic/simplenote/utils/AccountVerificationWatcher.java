package com.automattic.simplenote.utils;

import static com.automattic.simplenote.models.Account.KEY_EMAIL_VERIFICATION;

import androidx.annotation.NonNull;

import com.automattic.simplenote.Simplenote;
import com.automattic.simplenote.models.Account;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;

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
    private final VerificationStateListener listener;
    private Status currentState;

    public AccountVerificationWatcher(Simplenote simplenote, @NonNull VerificationStateListener listener) {
        this.simplenote = simplenote;
        this.listener = listener;
    }

    private void updateState(Status newState) {
        if (newState != currentState) {
            currentState = newState;
            listener.onUpdate(newState);
        }
    }

    private static boolean isValidChangeType(Bucket.ChangeType type, String key) {
        return type == Bucket.ChangeType.INDEX ||
                (type == Bucket.ChangeType.INSERT && KEY_EMAIL_VERIFICATION.equals(key)) ||
                (type == Bucket.ChangeType.MODIFY && KEY_EMAIL_VERIFICATION.equals(key));
    }

    @Override
    public void onNetworkChange(final Bucket<Account> bucket, Bucket.ChangeType type, String key) {
        // If the key for email verification is removed, the status is changed to UNVERIFIED immediately
        if (type == Bucket.ChangeType.REMOVE && KEY_EMAIL_VERIFICATION.equals(key)) {
            updateState(Status.UNVERIFIED);
            return;
        }

        String email = simplenote.getUserEmail();
        if (!isValidChangeType(type, key) || email == null) {
            return;
        }

        Account account;
        try {
            // When a network change of type INDEX, INDEX or MODIFY is received, it means that the account bucket finished
            // indexing or there were some changes in the account. In both cases, we need to check for the account status
            account = bucket.get(KEY_EMAIL_VERIFICATION);
        } catch (BucketObjectMissingException e) {
            AppLog.add(AppLog.Type.SYNC, "Account for email verification is missing");
            return;
        }

        boolean hasVerifiedEmail = account.hasVerifiedEmail(email);
        if (hasVerifiedEmail) {
            updateState(Status.VERIFIED);
        } else {
            Status statusUpdate = account.hasSentEmail(email) ? Status.SENT_EMAIL : Status.UNVERIFIED;
            updateState(statusUpdate);
        }
    }
}
