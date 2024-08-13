package org.wordpress.passcodelock;

import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Base64;

public class DefaultAppLock extends AbstractAppLock {
    public static boolean isSupportedApi() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    }

    private static final String UNLOCK_CLASS_NAME = PasscodeUnlockActivity.class.getName();
    private static final String OLD_PASSWORD_SALT = "sadasauidhsuyeuihdahdiauhs";
    private static final String OLD_APP_LOCK_PASSWORD_PREF_KEY = "wp_app_lock_password_key";

    private Application mCurrentApp;
    private SharedPreferences mSharedPreferences;
    private Date mLostFocusDate;

    public DefaultAppLock(Application app) {
        super();
        mCurrentApp = app;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mCurrentApp);
    }

    /** {@link PasscodeUnlockActivity} is always exempt. */
    @Override
    public boolean isExemptActivity(String activityName) {
        return UNLOCK_CLASS_NAME.equals(activityName) || super.isExemptActivity(activityName);
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (!isExemptActivity(activity.getClass().getName())) mLostFocusDate = new Date();
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (!isExemptActivity(activity.getClass().getName()) && shouldShowUnlockScreen()) {
            Intent i = new Intent(activity.getApplicationContext(), PasscodeUnlockActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.getApplication().startActivity(i);
        }
    }

    @Override public void onActivityCreated(Activity arg0, Bundle arg1) {}
    @Override public void onActivityDestroyed(Activity arg0) {}
    @Override public void onActivitySaveInstanceState(Activity arg0, Bundle arg1) {}
    @Override public void onActivityStarted(Activity arg0) {}
    @Override public void onActivityStopped(Activity arg0) {}

    public void enable() {
        if (!isPasswordLocked()) return;
        if (isSupportedApi()) {
            mCurrentApp.unregisterActivityLifecycleCallbacks(this);
            mCurrentApp.registerActivityLifecycleCallbacks(this);
        }
    }

    public void disable() {
        if (isSupportedApi()) {
            mCurrentApp.unregisterActivityLifecycleCallbacks(this);
        }
    }

    public boolean isPasswordLocked() {
        return mSharedPreferences.contains(BuildConfig.PASSWORD_PREFERENCE_KEY) ||
               mSharedPreferences.contains(OLD_APP_LOCK_PASSWORD_PREF_KEY);
    }

    public boolean setPassword(String password) {
        removePasswordFromPreferences();
        if (TextUtils.isEmpty(password)) {
            disable();
        } else {
            savePasswordToPreferences(password.hashCode());
            enable();
        }
        return true;
    }

    @Override
    public boolean isFingerprintEnabled() {
        return mSharedPreferences.getBoolean(BuildConfig.FINGERPRINT_ENABLED_KEY, true);
    }

    @Override
    public boolean enableFingerprint() {
        mSharedPreferences.edit().putBoolean(BuildConfig.FINGERPRINT_ENABLED_KEY, true).apply();
        return true;
    }

    @Override
    public boolean disableFingerprint() {
        mSharedPreferences.edit().putBoolean(BuildConfig.FINGERPRINT_ENABLED_KEY, false).apply();
        return true;
    }

    public void forcePasswordLock() {
        mLostFocusDate = null;
    }

    public boolean verifyPassword(String password) {
        if (TextUtils.isEmpty(password)) return false;

        // successful fingerprint scan bypasses PIN security
        if (isFingerprintPassword(password)) {
            mLostFocusDate = new Date();
            return true;
        }

    	String storedPassword = "";
        String securePassword = null;
        int updatedHash = -1;

    	if (mSharedPreferences.contains(OLD_APP_LOCK_PASSWORD_PREF_KEY)) {
            // backwards compatibility
    		storedPassword = getStoredLegacyPassword(OLD_APP_LOCK_PASSWORD_PREF_KEY);
    		securePassword = legacyPasswordHash(password);
    	} else if (mSharedPreferences.contains(BuildConfig.PASSWORD_PREFERENCE_KEY)) {
            if (shouldUpdatePassword()) {
                storedPassword = getStoredLegacyPassword(BuildConfig.PASSWORD_PREFERENCE_KEY);
                storedPassword = decryptPassword(storedPassword);
                storedPassword = stripSalt(storedPassword);
                securePassword = password;
                updatedHash = password.hashCode();
            } else {
                int storedHash = getStoredPassword();
                storedPassword = String.valueOf(storedHash);
                securePassword = String.valueOf(password.hashCode());
            }
    	}

        if (!storedPassword.equalsIgnoreCase(securePassword)) return false;

        // password security updated, replace stored password with integer hash value
        if (updatedHash != -1) {
            removePasswordFromPreferences();
            savePasswordToPreferences(updatedHash);
        }
        mLostFocusDate = new Date();
        return true;
    }

    private String stripSalt(String saltedPassword) {
        if (TextUtils.isEmpty(saltedPassword) || saltedPassword.length() < 4) return "";
        int middle = saltedPassword.length() / 2;
        return saltedPassword.substring(middle - 2, middle + 2);
    }

    /** Show the unlock screen if there is a saved password and the timeout period has elapsed. */
    private boolean shouldShowUnlockScreen() {
        if(!isPasswordLocked()) return false;
        if(mLostFocusDate == null) return true;

        int currentTimeOut = getTimeout();
        setOneTimeTimeout(DEFAULT_TIMEOUT_S);

        if (timeSinceLocked() < currentTimeOut) return false;
        mLostFocusDate = null;
        return true;
    }

    private int getStoredPassword() {
        return mSharedPreferences.getInt(BuildConfig.PASSWORD_PREFERENCE_KEY, -1);
    }

    private void savePasswordToPreferences(int password) {
        mSharedPreferences.edit().putInt(BuildConfig.PASSWORD_PREFERENCE_KEY, password).apply();
    }

    private void removePasswordFromPreferences() {
        mSharedPreferences.edit()
                .remove(OLD_APP_LOCK_PASSWORD_PREF_KEY)
                .remove(BuildConfig.PASSWORD_PREFERENCE_KEY)
                .apply();
    }

    private int timeSinceLocked() {
        return Math.abs((int) ((new Date().getTime() - mLostFocusDate.getTime()) / 1000));
    }

    //
    // Legacy methods for backwards compatibility of passwords stored using deprecated security
    //

    /** Update to hash-based security if password was stored using encryption-based security. */
    private boolean shouldUpdatePassword() {
        Object storedValue = mSharedPreferences.getAll().get(BuildConfig.PASSWORD_PREFERENCE_KEY);
        return storedValue != null && storedValue instanceof String;
    }

    private String getStoredLegacyPassword(String key) {
        return mSharedPreferences.getString(key, "");
    }

    private String legacyPasswordHash(String rawPassword) {
        return StringUtils.getMd5Hash(OLD_PASSWORD_SALT + rawPassword + OLD_PASSWORD_SALT);
    }

    private String decryptPassword(String encryptedPwd) {
        try {
            DESKeySpec keySpec = new DESKeySpec(BuildConfig.PASSWORD_ENC_SECRET.getBytes("UTF-8"));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey key = keyFactory.generateSecret(keySpec);

            byte[] encryptedWithoutB64 = Base64.decode(encryptedPwd, Base64.DEFAULT);
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] plainTextPwdBytes = cipher.doFinal(encryptedWithoutB64);
            return new String(plainTextPwdBytes);
        } catch (Exception e) {
        }
        return encryptedPwd;
    }
}
