package com.automattic.simplenote.widgets;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.automattic.simplenote.PreferencesActivity;
import com.automattic.simplenote.R;
import com.automattic.simplenote.Simplenote;
import com.automattic.simplenote.utils.PrefUtils;
import com.automattic.simplenote.utils.ThemeUtils;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.simperium.Simperium;
import com.simperium.android.LoginActivity;
import com.simperium.client.User;

import org.wordpress.passcodelock.AppLockManager;

/**
 * A simple {@link Fragment} subclass.
 */
public class PreferencesFragment extends PreferenceFragment implements User.StatusChangeListener, Simperium.OnUserCreatedListener {

    private Tracker mTracker;

    public PreferencesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        Preference authenticatePreference = findPreference("pref_key_authenticate");
        Simplenote currentApp = (Simplenote) getActivity().getApplication();
        currentApp.getSimperium().setUserStatusChangeListener(this);
        currentApp.getSimperium().setOnUserCreatedListener(this);
        mTracker = currentApp.getTracker();
        authenticatePreference.setSummary(currentApp.getSimperium().getUser().getEmail());
        if (currentApp.getSimperium().needsAuthorization()) {
            authenticatePreference.setTitle(R.string.sign_in);
        } else {
            authenticatePreference.setTitle(R.string.sign_out);
        }

        authenticatePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (!isAdded()) return false;

                Simplenote currentApp = (Simplenote) getActivity().getApplication();
                if (currentApp.getSimperium().needsAuthorization()) {
                    Intent loginIntent = new Intent(getActivity(), LoginActivity.class);
                    loginIntent.putExtra(LoginActivity.EXTRA_SIGN_IN_FIRST, true);
                    startActivityForResult(loginIntent, Simperium.SIGNUP_SIGNIN_REQUEST);
                } else {
                    Simplenote application = (Simplenote) getActivity().getApplication();
                    application.getSimperium().deauthorizeUser();
                    application.getNotesBucket().reset();
                    application.getTagsBucket().reset();
                    application.getNotesBucket().stop();
                    application.getTagsBucket().stop();
                    mTracker.send(
                            new HitBuilders.EventBuilder()
                                    .setCategory("user")
                                    .setAction("signed_out")
                                    .setLabel("preferences_sign_out_button")
                                    .build()
                    );

                    getActivity().finish();
                }
                return true;
            }
        });

        findPreference("pref_key_website").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
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

        if (!PrefUtils.getBoolPref(getActivity(), PrefUtils.PREF_THEME_MODIFIED, false))
            notesPreferenceGroup.removePreference(themePreference);

        themePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                int index = Integer.parseInt(newValue.toString());
                CharSequence[] entries = themePreference.getEntries();
                themePreference.setSummary(entries[index]);

                // update intent to indicate the theme setting was changed
                getActivity().setIntent(ThemeUtils.makeThemeChangeIntent());

                // recreate the activity so new theme is applied
                getActivity().recreate();

                return true;
            }
        });

        final ListPreference sortPreference = (ListPreference) findPreference(PrefUtils.PREF_SORT_ORDER);
        sortPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int index = Integer.parseInt(newValue.toString());
                CharSequence[] entries = sortPreference.getEntries();
                sortPreference.setSummary(entries[index]);
                return true;
            }
        });

        Preference passcodePref = findPreference("pref_key_passcode");
        if (!AppLockManager.getInstance().isAppLockFeatureEnabled()) {
            //Passcode Lock not supported
            PreferenceGroup rootGroup = (PreferenceGroup) findPreference("pref_key_note_preference");
            rootGroup.removePreference(passcodePref);
        }

        Preference versionPref = findPreference("pref_key_build");
        versionPref.setSummary(PrefUtils.versionInfo());

    }

    @Override
    public void onUserStatusChange(User.Status status) {
        if (isAdded() && status == User.Status.AUTHORIZED) {
            // User signed in
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    Preference authenticatePreference = findPreference("pref_key_authenticate");
                    authenticatePreference.setTitle(R.string.sign_out);
                }
            });
            mTracker.send(
                    new HitBuilders.EventBuilder()
                            .setCategory("user")
                            .setAction("signed_in")
                            .setLabel("signed_in_from_preferences_activity")
                            .build()
            );
        }
    }

    @Override
    public void onUserCreated(User user) {
        mTracker.send(
                new HitBuilders.EventBuilder()
                        .setCategory("user")
                        .setAction("new_account_created")
                        .setLabel("account_created_from_preferences_activity")
                        .build()
        );
    }
}
