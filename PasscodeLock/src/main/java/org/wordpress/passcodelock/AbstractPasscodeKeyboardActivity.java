package org.wordpress.passcodelock;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.hardware.fingerprint.FingerprintManagerCompat;
import androidx.core.os.CancellationSignal;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public abstract class AbstractPasscodeKeyboardActivity extends Activity {
    public static final String KEY_MESSAGE = "message";

    protected EditText mPinCodeField;
    protected InputFilter[] filters = null;
    protected TextView topMessage = null;

	@SuppressLint("RestrictedApi")
    protected FingerprintManagerCompat mFingerprintManager;
    protected CancellationSignal mCancel;

    /**
     * Edge-to-edge is enforced from Android 16 (targetSdk 36) with no opt-out. These are fullscreen
     * passcode screens without a toolbar, so the system bar insets are added on top of the layout's
     * own padding to keep the prompt and the keypad clear of the status and navigation bars. The
     * root keeps drawing its background behind the bars.
     */
    private void setUpEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        View root = findViewById(R.id.passcode_root);

        // The system bars draw over the lock screen background, so pick the icon colour that stays
        // legible against it rather than assuming a dark background.
        boolean isLightBackground = ColorUtils.calculateLuminance(
            ContextCompat.getColor(this, R.color.passcodelock_background)) > 0.5;
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
            getWindow(),
            getWindow().getDecorView()
        );
        controller.setAppearanceLightStatusBars(isLightBackground);
        controller.setAppearanceLightNavigationBars(isLightBackground);

        final int left = root.getPaddingLeft();
        final int top = root.getPaddingTop();
        final int right = root.getPaddingRight();
        final int bottom = root.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            // Rotation is allowed on large screens, where a side cutout can land beside the keypad.
            Insets bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom);
            return windowInsets;
        });
    }

	@SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!getResources().getBoolean(R.bool.allow_rotation)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        setContentView(R.layout.app_passcode_keyboard);

        setUpEdgeToEdge();

        topMessage = (TextView) findViewById(R.id.passcodelock_prompt);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String message = extras.getString(KEY_MESSAGE);
            if (message != null) {
                topMessage.setText(message);
            }
        }

        filters = new InputFilter[2];
        filters[0]= new InputFilter.LengthFilter(1);
        filters[1] = onlyNumber;

        mPinCodeField = (EditText)findViewById(R.id.pin_field);

        //setup the keyboard
        findViewById(R.id.button0).setOnClickListener(defaultButtonListener);
        findViewById(R.id.button1).setOnClickListener(defaultButtonListener);
        findViewById(R.id.button2).setOnClickListener(defaultButtonListener);
        findViewById(R.id.button3).setOnClickListener(defaultButtonListener);
        findViewById(R.id.button4).setOnClickListener(defaultButtonListener);
        findViewById(R.id.button5).setOnClickListener(defaultButtonListener);
        findViewById(R.id.button6).setOnClickListener(defaultButtonListener);
        findViewById(R.id.button7).setOnClickListener(defaultButtonListener);
        findViewById(R.id.button8).setOnClickListener(defaultButtonListener);
        findViewById(R.id.button9).setOnClickListener(defaultButtonListener);
        findViewById(R.id.button_erase).setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        if (arg0.isHapticFeedbackEnabled()) {
                            arg0.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                        }

                        String curText = mPinCodeField.getText().toString();

                        if (curText.length() > 0) {
                            mPinCodeField.setText(curText.substring(0, curText.length() - 1));
                            mPinCodeField.setSelection(mPinCodeField.length());
                        }
                    }
                });

        mFingerprintManager = FingerprintManagerCompat.from(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mCancel != null) {
            mCancel.cancel();
        }
    }

    protected AbstractAppLock getAppLock() {
        return AppLockManager.getInstance().getAppLock();
    }

    private OnClickListener defaultButtonListener = new OnClickListener() {
        @Override
        public void onClick(View arg0) {
            int currentValue = -1;
            int id = arg0.getId();
			if (id == R.id.button0) {
				currentValue = 0;
			} else if (id == R.id.button1) {
				currentValue = 1;
			} else if (id == R.id.button2) {
				currentValue = 2;
			} else if (id == R.id.button3) {
				currentValue = 3;
			} else if (id == R.id.button4) {
				currentValue = 4;
			} else if (id == R.id.button5) {
				currentValue = 5;
			} else if (id == R.id.button6) {
				currentValue = 6;
			} else if (id == R.id.button7) {
				currentValue = 7;
			} else if (id == R.id.button8) {
				currentValue = 8;
			} else if (id == R.id.button9) {
				currentValue = 9;
			}

            if (arg0.isHapticFeedbackEnabled()) {
                arg0.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            }

            // Add value and move focus.
            mPinCodeField.append(String.valueOf(currentValue));
            mPinCodeField.setSelection(mPinCodeField.length());

            if (mPinCodeField.length() >= 4) {
                onPinLockInserted();
            }
        }
    };

    protected void authenticationSucceeded() {
        setResult(RESULT_OK);
        finish();
    }

    protected void authenticationFailed() {
        Thread shake = new Thread() {
            public void run() {
                Animation shake = AnimationUtils.loadAnimation(AbstractPasscodeKeyboardActivity.this, R.anim.shake);
                findViewById(R.id.AppUnlockLinearLayout1).startAnimation(shake);
                showPasswordError();
                mPinCodeField.setText("");
            }
        };
        runOnUiThread(shake);
    }

    protected void showPasswordError(){
        Toast.makeText(AbstractPasscodeKeyboardActivity.this, R.string.passcode_wrong_passcode, Toast.LENGTH_SHORT).show();
    }

    protected abstract void onPinLockInserted();
	@SuppressLint("RestrictedApi")
    protected abstract FingerprintManagerCompat.AuthenticationCallback getFingerprintCallback();

    private InputFilter onlyNumber = new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            if (source.length() > 1) {
                return "";
            }

            if (source.length() == 0) {
                return null;
            }

            try {
                int number = Integer.parseInt(source.toString());
                if (number >= 0 && number <= 9) {
                    return String.valueOf(number);
                }

                return "";
            } catch (NumberFormatException e) {
                return "";
            }
        }
    };
}
