package org.wordpress.passcodelock;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.window.OnBackInvokedDispatcher;

import androidx.core.hardware.fingerprint.FingerprintManagerCompat;
import androidx.core.os.CancellationSignal;

public class PasscodeUnlockActivity extends AbstractPasscodeKeyboardActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Where the platform routes back through OnBackInvokedDispatcher instead of
        // onBackPressed(), the callback has to be registered directly: this is a plain Activity, so
        // there is no androidx OnBackPressedDispatcher to fall back on. Without it, back would
        // dismiss the lock screen without locking.
        //
        // registerOnBackInvokedCallback exists from Android 13, but the platform only consults it
        // when the dispatcher is enabled for the app, which depends on the OS version and the
        // manifest. onBackPressed() below is therefore still the live path on every device where it
        // is not — do not remove it as redundant without checking the behaviour on those versions.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                this::lockAndGoHome
            );
        }
    }

	@SuppressLint("RestrictedApi")
    @Override
    public void onResume() {
        super.onResume();

        if (isFingerprintSupportedAndEnabled()) {
            mCancel = new CancellationSignal();
            mFingerprintManager.authenticate(null, 0, mCancel, getFingerprintCallback(), null);
            View view = findViewById(R.id.image_fingerprint);
            view.setVisibility(View.VISIBLE);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        lockAndGoHome();
    }

    private void lockAndGoHome() {
        getAppLock().forcePasswordLock();
        Intent i = new Intent();
        i.setAction(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_HOME);
        startActivity(i);
        finish();
    }

    @Override
    protected void onPinLockInserted() {
        String passLock = mPinCodeField.getText().toString();
        if (getAppLock().verifyPassword(passLock)) {
            authenticationSucceeded();
        } else {
            authenticationFailed();
        }
    }

	@SuppressLint("RestrictedApi")
    @Override
    protected FingerprintManagerCompat.AuthenticationCallback getFingerprintCallback() {
        return new FingerprintManagerCompat.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
                // without the call to verifyPassword the unlock screen will show multiple times
                getAppLock().verifyPassword(AbstractAppLock.FINGERPRINT_VERIFICATION_BYPASS);
                authenticationSucceeded();
            }

            @Override
            public void onAuthenticationFailed() {
                authenticationFailed();
            }

            @Override public void onAuthenticationError(int errMsgId, CharSequence errString) { }
            @Override public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) { }
        };
    }

	@SuppressLint("RestrictedApi")
    private boolean isFingerprintSupportedAndEnabled() {
        return mFingerprintManager.isHardwareDetected() &&
               mFingerprintManager.hasEnrolledFingerprints() &&
               getAppLock().isFingerprintEnabled();
    }
}
