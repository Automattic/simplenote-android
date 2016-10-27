package com.automattic.simplenote;

import android.app.FragmentManager;
import android.os.Bundle;
import android.preference.Preference;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.automattic.simplenote.utils.ThemeUtils;

import org.wordpress.passcodelock.PasscodePreferenceFragment;

public class PreferencesActivity extends AppCompatActivity {

    private PasscodePreferenceFragment mPasscodePreferenceFragment;
    private PreferencesFragment mPreferencesFragment;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.setTheme(this);
        super.onCreate(savedInstanceState);

        // if a new theme was picked, activity is recreated with theme changed intent
        // set result to notify the calling activity once this activity is complete
        if (ThemeUtils.themeWasChanged(getIntent()))
            setResult(RESULT_OK, getIntent());

        setContentView(R.layout.activity_preferences);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setTitle(R.string.settings);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        String preferencesTag = "tag_preferences";
        String passcodeTag = "tag_passcode";
        if (savedInstanceState == null) {
            Bundle passcodeArgs = new Bundle();
            passcodeArgs.putBoolean(PasscodePreferenceFragment.KEY_SHOULD_INFLATE, false);
            mPasscodePreferenceFragment = new PasscodePreferenceFragment();
            mPasscodePreferenceFragment.setArguments(passcodeArgs);

            mPreferencesFragment = new PreferencesFragment();
            getFragmentManager().beginTransaction()
                    .add(R.id.preferences_container, mPreferencesFragment, preferencesTag)
                    .add(R.id.preferences_container, mPasscodePreferenceFragment, passcodeTag)
                    .commit();
        } else {
            FragmentManager fragmentManager = getFragmentManager();
            mPreferencesFragment = (PreferencesFragment) fragmentManager.findFragmentByTag(preferencesTag);
            mPasscodePreferenceFragment = (PasscodePreferenceFragment) fragmentManager.findFragmentByTag(passcodeTag);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        Preference togglePref =
                mPreferencesFragment.findPreference(getString(R.string.pref_key_passcode_toggle));
        Preference changePref =
                mPreferencesFragment.findPreference(getString(R.string.pref_key_change_passcode));

        if (togglePref != null && changePref != null) {
            mPasscodePreferenceFragment.setPreferences(togglePref, changePref);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
