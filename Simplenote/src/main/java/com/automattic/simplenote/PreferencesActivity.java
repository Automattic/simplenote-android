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

import com.google.analytics.tracking.android.EasyTracker;
import com.simperium.Simperium;
import com.simperium.android.LoginActivity;
import com.simperium.client.User;

import org.wordpress.passcodelock.AppLockManager;

public class PreferencesActivity extends PreferenceActivity implements User.StatusChangeListener, Simperium.OnUserCreatedListener {

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        final ListPreference sortPreference = (ListPreference) findPreference("pref_key_sort_order");
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
