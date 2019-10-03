package com.automattic.simplenote;

import android.app.Activity;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.automattic.simplenote.analytics.AnalyticsTracker;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Preferences;
import com.automattic.simplenote.utils.CrashUtils;
import com.automattic.simplenote.utils.HtmlCompat;
import com.automattic.simplenote.utils.PrefUtils;
import com.automattic.simplenote.utils.WidgetUtils;
import com.simperium.Simperium;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;
import com.simperium.client.BucketObjectNameInvalid;
import com.simperium.client.User;

import java.lang.ref.WeakReference;

import static com.automattic.simplenote.models.Preferences.PREFERENCES_OBJECT_KEY;

/**
 * A simple {@link Fragment} subclass.
 */
public class PreferencesFragment extends PreferenceFragmentCompat implements User.StatusChangeListener,
        Simperium.OnUserCreatedListener {

    private static final String WEB_APP_URL = "https://app.simplenote.com";

    private Bucket<Preferences> mPreferencesBucket;
    private SwitchPreferenceCompat mAnalyticsSwitch;

    public PreferencesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Preference authenticatePreference = findPreference("pref_key_authenticate");
        Simplenote currentApp = (Simplenote) getActivity().getApplication();
        Simperium simperium = currentApp.getSimperium();
        simperium.setUserStatusChangeListener(this);
        simperium.setOnUserCreatedListener(this);
        mPreferencesBucket = currentApp.getPreferencesBucket();
        mPreferencesBucket.start();

        authenticatePreference.setSummary(currentApp.getSimperium().getUser().getEmail());
        if (simperium.needsAuthorization()) {
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
                    Intent loginIntent = new Intent(getActivity(), SimplenoteAuthenticationActivity.class);
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
                updateTheme(requireActivity(), Integer.parseInt(newValue.toString()));
                return true;
            }

            private void updateTheme(Activity activity, int index) {
                CharSequence[] entries = themePreference.getEntries();
                themePreference.setSummary(entries[index]);

                AnalyticsTracker.track(
                        AnalyticsTracker.Stat.SETTINGS_THEME_UPDATED,
                        AnalyticsTracker.CATEGORY_USER,
                        "theme_preference"
                );

                // recreate the activity so new theme is applied
                activity.recreate();
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

        SwitchPreferenceCompat switchPreference = (SwitchPreferenceCompat) findPreference("pref_key_condensed_note_list");
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

        // Add the hyperlink to the analytics summary
        Preference analyticsSummaryPreference = findPreference("pref_key_analytics_enabled_summary");
        String formattedSummary = String.format(
                getString(R.string.share_analytics_summary),
                "<a href=\"https://automattic.com/cookies\">",
                "</a>"
        );
        analyticsSummaryPreference.setSummary(HtmlCompat.fromHtml(formattedSummary));

        mAnalyticsSwitch = (SwitchPreferenceCompat)findPreference("pref_key_analytics_switch");
        mAnalyticsSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                try {
                    boolean isChecked = (boolean)newValue;
                    Preferences prefs = mPreferencesBucket.get(PREFERENCES_OBJECT_KEY);
                    prefs.setAnalyticsEnabled(isChecked);
                    prefs.save();
                } catch (BucketObjectMissingException e) {
                    e.printStackTrace();
                }

                return true;
            }
        });

        updateAnalyticsSwitchState();
    }

    @Override
    public void onPause() {
        super.onPause();
        mPreferencesBucket.stop();
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
        application.getPreferencesBucket().reset();

        application.getNotesBucket().stop();
        application.getTagsBucket().stop();
        application.getPreferencesBucket().stop();

        AnalyticsTracker.track(
                AnalyticsTracker.Stat.USER_SIGNED_OUT,
                AnalyticsTracker.CATEGORY_USER,
                "preferences_sign_out_button"
        );

        // Resets analytics user back to 'anon' type
        AnalyticsTracker.refreshMetadata(null);
        CrashUtils.clearCurrentUser();

        // Remove wp.com token
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
        editor.remove(PrefUtils.PREF_WP_TOKEN);

        // Remove WordPress sites
        editor.remove(PrefUtils.PREF_WORDPRESS_SITES);
        editor.apply();

        WidgetUtils.updateNoteWidgets(getActivity());

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
            CrashUtils.setCurrentUser(app.getSimperium().getUser());

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

    private void updateAnalyticsSwitchState() {
        try {
            Preferences prefs = mPreferencesBucket.get(PREFERENCES_OBJECT_KEY);
            mAnalyticsSwitch.setChecked(prefs.getAnalyticsEnabled());
        } catch (BucketObjectMissingException e) {
            // The preferences object doesn't exist for this user yet, create it
            try {
                Preferences prefs = mPreferencesBucket.newObject(PREFERENCES_OBJECT_KEY);
                prefs.setAnalyticsEnabled(true);
                prefs.save();
            } catch (BucketObjectNameInvalid bucketObjectNameInvalid) {
                bucketObjectNameInvalid.printStackTrace();
            }
        }
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
                new AlertDialog.Builder(new ContextThemeWrapper(fragment.requireContext(), R.style.Dialog))
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
