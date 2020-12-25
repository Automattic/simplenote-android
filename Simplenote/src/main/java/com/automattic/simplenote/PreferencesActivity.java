package com.automattic.simplenote;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;

import com.automattic.simplenote.utils.BrowserUtils;
import com.automattic.simplenote.utils.ThemeUtils;

import org.wordpress.passcodelock.PasscodePreferenceFragment;
import org.wordpress.passcodelock.PasscodePreferenceFragmentCompat;

import static com.automattic.simplenote.PreferencesFragment.WEB_APP_URL;
import static com.automattic.simplenote.utils.DisplayUtils.disableScreenshotsIfLocked;

public class PreferencesActivity extends ThemedAppCompatActivity {
    private PasscodePreferenceFragmentCompat mPasscodePreferenceFragment;
    private PreferencesFragment mPreferencesFragment;

    @Override
    public void onBackPressed() {
        NavUtils.navigateUpFromSameTask(PreferencesActivity.this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_preferences);

        Toolbar toolbar = findViewById(R.id.toolbar);
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
            mPasscodePreferenceFragment = new PasscodePreferenceFragmentCompat();
            mPasscodePreferenceFragment.setArguments(passcodeArgs);

            mPreferencesFragment = new PreferencesFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.preferences_container, mPreferencesFragment, preferencesTag)
                    .add(R.id.preferences_container, mPasscodePreferenceFragment, passcodeTag)
                    .commit();
        } else {
            FragmentManager fragmentManager = getSupportFragmentManager();
            mPreferencesFragment = (PreferencesFragment) fragmentManager.findFragmentByTag(preferencesTag);
            mPasscodePreferenceFragment = (PasscodePreferenceFragmentCompat) fragmentManager.findFragmentByTag(passcodeTag);
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
            NavUtils.navigateUpFromSameTask(PreferencesActivity.this);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        disableScreenshotsIfLocked(this);
    }

    @Override
    public void recreate() {
        Intent intent = new Intent(PreferencesActivity.this, PreferencesActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    public void openBrowserForMembership(View view) {
        try {
            BrowserUtils.launchBrowserOrShowError(PreferencesActivity.this, WEB_APP_URL);
        } catch (Exception e) {
            Toast.makeText(PreferencesActivity.this, R.string.no_browser_available, Toast.LENGTH_LONG).show();
        }
    }
}
