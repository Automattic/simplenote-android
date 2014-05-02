package com.automattic.simplenote;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.view.MenuItem;

import com.automattic.simplenote.utils.PrefUtils;
import com.automattic.simplenote.utils.ThemeUtils;
import com.google.analytics.tracking.android.EasyTracker;
import com.simperium.Simperium;
import com.simperium.android.LoginActivity;
import com.simperium.client.User;

import org.wordpress.passcodelock.AppLockManager;

public class PreferencesActivity extends PreferenceActivity implements User.StatusChangeListener, Simperium.OnUserCreatedListener {

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        ThemeUtils.setTheme(this);

        super.onCreate(savedInstanceState);

        // if a new theme was picked, activity is recreated with theme changed intent
        // set result to notify the calling activity once this activity is complete
        if (ThemeUtils.themeWasChanged(getIntent()))
            setResult(RESULT_OK, getIntent());

        addPreferencesFromResource(R.xml.preferences);

        setTitle(R.string.settings);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        Preference authenticatePreference = findPreference("pref_key_authenticate");
        Simplenote currentApp = (Simplenote) getApplication();
        currentApp.getSimperium().setUserStatusChangeListener(this);
        currentApp.getSimperium().setOnUserCreatedListener(this);
        authenticatePreference.setSummary(currentApp.getSimperium().getUser().getEmail());
        if (currentApp.getSimperium().needsAuthorization()) {
            authenticatePreference.setTitle(R.string.sign_in);
        } else {
            authenticatePreference.setTitle(R.string.sign_out);
        }

        authenticatePreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                Simplenote currentApp = (Simplenote) getApplication();
                if (currentApp.getSimperium().needsAuthorization()) {
                    Intent loginIntent = new Intent(PreferencesActivity.this, LoginActivity.class);
                    loginIntent.putExtra(LoginActivity.EXTRA_SIGN_IN_FIRST, true);
                    startActivityForResult(loginIntent, Simperium.SIGNUP_SIGNIN_REQUEST);
                } else {
                    Simplenote application = (Simplenote) getApplication();
                    application.getSimperium().deauthorizeUser();
                    application.getNotesBucket().reset();
                    application.getTagsBucket().reset();
                    application.getNotesBucket().stop();
                    application.getTagsBucket().stop();
                    EasyTracker.getTracker().sendEvent("user", "signed_out", "preferences_sign_out_button", null);
                    finish();
                }
                return true;
            }
        });

        findPreference("pref_key_website").setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://simplenote.com")));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        });

        PreferenceGroup notesPreferenceGroup = (PreferenceGroup) findPreference("pref_key_note_preferences");
        final ListPreference themePreference = (ListPreference) findPreference(PrefUtils.PREF_THEME);

        if (!PrefUtils.getBoolPref(this, PrefUtils.PREF_THEME_MODIFIED, false))
            notesPreferenceGroup.removePreference(themePreference);

        themePreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                int index = Integer.parseInt(newValue.toString());
                CharSequence[] entries = themePreference.getEntries();
                themePreference.setSummary(entries[index]);

                // update intent to indicate the theme setting was changed
                setIntent(ThemeUtils.makeThemeChangeIntent());

                // recreate the activity so new theme is applied
                recreate();

                return true;
            }
        });

        final ListPreference sortPreference = (ListPreference) findPreference(PrefUtils.PREF_SORT_ORDER);
        sortPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int index = Integer.parseInt(newValue.toString());
                CharSequence[] entries = sortPreference.getEntries();
                sortPreference.setSummary(entries[index]);
                return true;
            }
        });

        Preference passcodePref = findPreference("pref_key_passcode");
        if (AppLockManager.getInstance().isAppLockFeatureEnabled() == false) {
            //Passcode Lock not supported
            PreferenceGroup rootGroup = (PreferenceGroup) findPreference("pref_key_note_preference");
            rootGroup.removePreference(passcodePref);
        }

        Preference versionPref = findPreference("pref_key_build");
        versionPref.setSummary(PrefUtils.versionInfo());

        EasyTracker.getInstance().activityStart(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EasyTracker.getInstance().activityStop(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onUserStatusChange(User.Status status) {
        if (status == User.Status.AUTHORIZED) {
            // User signed in
            runOnUiThread(new Runnable() {
                public void run() {
                    Preference authenticatePreference = findPreference("pref_key_authenticate");
                    authenticatePreference.setTitle(R.string.sign_out);
                }
            });
            EasyTracker.getTracker().sendEvent("user", "signed_in", "signed_in_from_preferences_activity", null);
        }
    }

    @Override
    public void onUserCreated(User user) {
        EasyTracker.getTracker().sendEvent("user", "new_account_created", "account_created_from_preferences_activity", null);
    }

}
