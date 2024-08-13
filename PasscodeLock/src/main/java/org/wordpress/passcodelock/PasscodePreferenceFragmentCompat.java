package org.wordpress.passcodelock;

import android.content.Intent;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class PasscodePreferenceFragmentCompat extends PreferenceFragmentCompat
        implements Preference.OnPreferenceClickListener {
    public static final String KEY_SHOULD_INFLATE = "should-inflate";
    public static final int ENABLE_PASSLOCK  = 0;
    public static final int DISABLE_PASSLOCK = 1;
    public static final int CHANGE_PASSWORD  = 2;

    private Preference mTogglePasscodePreference;
    private Preference mChangePasscodePreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
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

        if (preferenceKey.equals(getString(R.string.pref_key_passcode_toggle))) {
            return handlePasscodeToggleClick();
        } else if (preferenceKey.equals(getString(R.string.pref_key_change_passcode))) {
            return handleChangePasscodeClick();
        }

        return false;
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
            mTogglePasscodePreference.setOnPreferenceClickListener(this);
            mChangePasscodePreference.setOnPreferenceClickListener(this);

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
