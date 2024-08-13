package org.wordpress.passcodelock;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;

public class PasscodePreferenceFragment extends PreferenceFragment
        implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {
    public static final String KEY_SHOULD_INFLATE = "should-inflate";
    public static final int ENABLE_PASSLOCK  = 0;
    public static final int DISABLE_PASSLOCK = 1;
    public static final int CHANGE_PASSWORD  = 2;

    private Preference mTogglePasscodePreference;
    private Preference mChangePasscodePreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();

        if (args != null && args.getBoolean(KEY_SHOULD_INFLATE, true)) {
            addPreferencesFromResource(R.xml.passcode_preferences);
            mTogglePasscodePreference = findPreference(getString(R.string.pref_key_passcode_toggle));
            mChangePasscodePreference = findPreference(getString(R.string.pref_key_change_passcode));
        }

        refreshPreferenceState();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String preferenceKey = preference.getKey() != null ? preference.getKey() : "";

       if (preferenceKey.equals(getString(R.string.pref_key_change_passcode))) {
            return handleChangePasscodeClick();
        }

        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (newValue == null) return false;
        String preferenceKey = preference.getKey() != null ? preference.getKey() : "";
        if (!preferenceKey.equals(getString(R.string.pref_key_passcode_toggle))) {
            // Make sure we're updating the correct preference item.
            // Actually this check is not even required, since we've one item only that has set the
            // OnPreferenceChangeListener.
            return false;
        }

        Boolean newValueBool = (Boolean) newValue;
        boolean oldValue = ((SwitchPreference)mTogglePasscodePreference).isChecked();
        if (newValueBool == oldValue) {
            // Already updated. Do not call the setup activity.
            // This method get called twice if the click is on the row (not on the toggle visual item)
            // on devices Pre-Lollip.
            return true;
        }

        handlePasscodeToggleClick();

        return true;
    }

    /**
     * When the preferences are nested in a parent apps xml layout the inflated preferences will
     * need to be set.
     */
    public void setPreferences(Preference togglePreference, Preference changePreference) {
        mTogglePasscodePreference = togglePreference;
        mChangePasscodePreference = changePreference;

        refreshPreferenceState();
    }

    /**
     * Called when user requests to turn the passlock on or off.
     *
     * @return
     *  always true to indicate that the request was handled
     */
    private boolean handlePasscodeToggleClick() {
        int type = AppLockManager.getInstance().getAppLock().isPasswordLocked()
                ? DISABLE_PASSLOCK : ENABLE_PASSLOCK;
        Intent i = new Intent(getActivity(), PasscodeManagePasswordActivity.class);
        i.putExtra(PasscodeManagePasswordActivity.KEY_TYPE, type);
        startActivityForResult(i, type);

        return true;
    }

    /**
     * Called when user requests to change passcode.
     *
     * @return
     *  always true to indicate that the request was handled
     */
    private boolean handleChangePasscodeClick() {
        Intent i = new Intent(getActivity(), PasscodeManagePasswordActivity.class);
        i.putExtra(PasscodeManagePasswordActivity.KEY_TYPE, CHANGE_PASSWORD);
        i.putExtra(AbstractPasscodeKeyboardActivity.KEY_MESSAGE,
                getString(R.string.passcode_enter_old_passcode));
        startActivityForResult(i, CHANGE_PASSWORD);

        return true;
    }

    /**
     * Helper method to setup preference properties
     */
    private void refreshPreferenceState() {
        if (mTogglePasscodePreference != null && mChangePasscodePreference != null) {
            mChangePasscodePreference.setOnPreferenceClickListener(this);
            mTogglePasscodePreference.setOnPreferenceChangeListener(this);

            if (AppLockManager.getInstance().getAppLock().isPasswordLocked()) {
                mTogglePasscodePreference.setTitle(R.string.passcode_turn_off);
                mChangePasscodePreference.setEnabled(true);
            } else {
                mTogglePasscodePreference.setTitle(R.string.passcode_turn_on);
                mChangePasscodePreference.setEnabled(false);
            }
        }
    }
}
