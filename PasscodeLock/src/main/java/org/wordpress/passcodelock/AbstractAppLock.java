package org.wordpress.passcodelock;

import android.annotation.TargetApi;
import android.app.Application;
import android.os.Build;

/**
 * Interface for AppLock implementations.
 *
 * There are situations where the AppLock should not be required within an app. Methods for tracking
 * exempt {@link android.app.Activity}'s are provided and sub-class implementations are expected to
 * comply with requested exemptions.
 * @see #isExemptActivity(String)
 * @see #setExemptActivities(String[])
 * @see #getExemptActivities()
 *
 * Applications can request a one-time delay in locking the app. This can be useful for activities
 * that launch external applications with the expectation that the user will return to the calling
 * application shortly.
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public abstract class AbstractAppLock implements Application.ActivityLifecycleCallbacks {
    public static final String FINGERPRINT_VERIFICATION_BYPASS = "fingerprint-bypass__";
    public static final int DEFAULT_TIMEOUT_S = 2;
    public static final int EXTENDED_TIMEOUT_S = 60;

    private int mLockTimeout = DEFAULT_TIMEOUT_S;
    private String[] mExemptActivities;

    public boolean isExemptActivity(String name) {
        if (name == null) return false;
        for (String activityName : getExemptActivities()) {
            if (name.equals(activityName)) return true;
        }
        return false;
    }

    public void setExemptActivities(String[] exemptActivities) {
        mExemptActivities = exemptActivities;
    }

    public String[] getExemptActivities() {
        if (mExemptActivities == null) setExemptActivities(new String[0]);
        return mExemptActivities;
    }

    public void setOneTimeTimeout(int timeout) {
        mLockTimeout = timeout;
    }

    public int getTimeout() {
        return mLockTimeout;
    }

    protected boolean isFingerprintPassword(String password) {
        return FINGERPRINT_VERIFICATION_BYPASS.equals(password);
    }

    /**
     * Whether the fingerprint unlocking should be available as option in the unlock screen.
     * Default is true, but implementation can override this and make their choice.
     *
     * Note that this doesn't affect system setting, the device must already have fingerprint unlock
     * available and correctly working.
     *
     * @return true if fingerprint unlock should be enabled on the lock screen
     */
    public boolean isFingerprintEnabled() {
        return true;
    }

    // Stub methods to avoid sub-classes to override to many unused methods.
    public boolean enableFingerprint() {
        return true;
    }
    public boolean disableFingerprint() {
        return false;
    }

    public abstract void enable();
    public abstract void disable();
    public abstract void forcePasswordLock();
    public abstract boolean verifyPassword(String password);
    public abstract boolean isPasswordLocked();
    public abstract boolean setPassword(String password);
}
