package com.automattic.simplenote;


import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.widget.Toast;

import com.automattic.simplenote.analytics.AnalyticsTracker;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.utils.PrefUtils;
import com.automattic.simplenote.utils.ThemeUtils;
import com.simperium.Simperium;
import com.simperium.android.LoginActivity;
import com.simperium.client.Bucket;
import com.simperium.client.User;
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat;
import com.takisoft.fix.support.v7.preference.SwitchPreferenceCompat;

import java.lang.ref.WeakReference;

/**
 * A simple {@link Fragment} subclass.
 */
public class PreferencesFragment extends PreferenceFragmentCompat implements User.StatusChangeListener, Simperium.OnUserCreatedListener {

    private static final String WEB_APP_URL = "https://app.simplenote.com";

    public PreferencesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreatePreferencesFix(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Preference authenticatePreference = findPreference("pref_key_authenticate");
        Simplenote currentApp = (Simplenote) getActivity().getApplication();
        currentApp.getSimperium().setUserStatusChangeListener(this);
        currentApp.getSimperium().setOnUserCreatedListener(this);
        authenticatePreference.setSummary(currentApp.getSimperium().getUser().getEmail());
        if (currentApp.getSimperium().needsAuthorization()) {
            authenticatePreference.setTitle(R.string.sign_in);
        } else {
            authenticatePreference.setTitle(R.string.sign_out);
        }

        authenticatePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (!isAdded()) {
                    return false;
                }

                Simplenote currentApp = (Simplenote) getActivity().getApplication();
                if (currentApp.getSimperium().needsAuthorization()) {
                    Intent loginIntent = new Intent(getActivity(), LoginActivity.class);
                    loginIntent.putExtra(LoginActivity.EXTRA_SIGN_IN_FIRST, true);
                    startActivityForResult(loginIntent, Simperium.SIGNUP_SIGNIN_REQUEST);
                } else {
                    new SignOutAsyncTask(PreferencesFragment.this).execute();
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
                    Toast.makeText(getActivity(), R.string.no_browser_available, Toast.LENGTH_LONG).show();
                }
                return true;
            }
        });

        findPreference("pref_key_about").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(getActivity(), AboutActivity.class));
                return true;
            }
        });

        final ListPreference themePreference = (ListPreference) findPreference(PrefUtils.PREF_THEME);
        themePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                int index = Integer.parseInt(newValue.toString());
                CharSequence[] entries = themePreference.getEntries();
                themePreference.setSummary(entries[index]);

                AnalyticsTracker.track(
                        AnalyticsTracker.Stat.SETTINGS_THEME_UPDATED,
                        AnalyticsTracker.CATEGORY_USER,
                        "theme_preference"
                );

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

        Preference versionPref = findPreference("pref_key_build");
        versionPref.setSummary(PrefUtils.versionInfo());

        SwitchPreferenceCompat switchPreference = (SwitchPreferenceCompat) findPreference(PrefUtils.PREF_CONDENSED_LIST);
        switchPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if (((SwitchPreferenceCompat) preference).isChecked()) {
                    AnalyticsTracker.track(
                            AnalyticsTracker.Stat.SETTINGS_LIST_CONDENSED_ENABLED,
                            AnalyticsTracker.CATEGORY_USER,
                            "condensed_list_preference"
                    );
                }

                return true;
            }
        });

        SwitchPreferenceCompat keepScreenOnPreference = (SwitchPreferenceCompat) findPreference(PrefUtils.PREF_EDIT_NOTE_KEEP_SCREEN_ON);
        keepScreenOnPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if (((SwitchPreferenceCompat) preference).isChecked()) {
                    AnalyticsTracker.track(
                            AnalyticsTracker.Stat.SETTINGS_EDIT_NOTE_KEEP_SCREEN_ON_ENABLED,
                            AnalyticsTracker.CATEGORY_USER,
                            "keep_screen_on"
                    );
                }

                return true;
            }
        });
    }

    private DialogInterface.OnClickListener signOutClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            signOut();
        }
    };

    private DialogInterface.OnClickListener loadWebAppClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(WEB_APP_URL)));
        }
    };

    private boolean hasUnsyncedNotes() {
        Simplenote application = (Simplenote) getActivity().getApplication();
        Bucket<Note> notesBucket = application.getNotesBucket();
        Bucket.ObjectCursor<Note> notesCursor = notesBucket.allObjects();
        while (notesCursor.moveToNext()) {
            Note note = notesCursor.getObject();
            if (note.isNew() || note.isModified()) {
                return true;
            }
        }

        return false;
    }

    private void signOut() {
        Simplenote application = (Simplenote) getActivity().getApplication();
        application.getSimperium().deauthorizeUser();
        application.getNotesBucket().reset();
        application.getTagsBucket().reset();
        application.getNotesBucket().stop();
        application.getTagsBucket().stop();
        AnalyticsTracker.track(
                AnalyticsTracker.Stat.USER_SIGNED_OUT,
                AnalyticsTracker.CATEGORY_USER,
                "preferences_sign_out_button"
        );

        // Resets analytics user back to 'anon' type
        AnalyticsTracker.refreshMetadata(null);

        getActivity().finish();
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

            Simplenote app = (Simplenote) getActivity().getApplication();
            AnalyticsTracker.refreshMetadata(app.getSimperium().getUser().getEmail());

            AnalyticsTracker.track(
                    AnalyticsTracker.Stat.USER_SIGNED_IN,
                    AnalyticsTracker.CATEGORY_USER,
                    "signed_in_from_preferences_activity"
            );
        }
    }

    @Override
    public void onUserCreated(User user) {
        AnalyticsTracker.track(
                AnalyticsTracker.Stat.USER_ACCOUNT_CREATED,
                AnalyticsTracker.CATEGORY_USER,
                "account_created_from_preferences_activity"
        );
    }

    private static class SignOutAsyncTask extends AsyncTask<Void, Void, Boolean> {
        private WeakReference<PreferencesFragment> fragmentWeakReference;

        SignOutAsyncTask(PreferencesFragment fragment) {
            fragmentWeakReference = new WeakReference<>(fragment);
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            PreferencesFragment fragment = fragmentWeakReference.get();
            return fragment == null || fragment.hasUnsyncedNotes();
        }

        @Override
        protected void onPostExecute(Boolean hasUnsyncedNotes) {
            PreferencesFragment fragment = fragmentWeakReference.get();
            if (fragment == null) {
                return;
            }

            // Safety first! Check if any notes are unsynced and warn the user if so.
            if (hasUnsyncedNotes) {
                new AlertDialog.Builder(fragment.getContext())
                        .setTitle(R.string.unsynced_notes)
                        .setMessage(R.string.unsynced_notes_message)
                        .setPositiveButton(R.string.delete_notes, fragment.signOutClickListener)
                        .setNeutralButton(R.string.visit_web_app, fragment.loadWebAppClickListener)
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            } else {
                fragment.signOut();
            }
        }
    }
}
